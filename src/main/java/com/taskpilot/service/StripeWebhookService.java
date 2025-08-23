package com.taskpilot.service;

import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.taskpilot.model.Plan;
import com.taskpilot.model.User;
import com.taskpilot.repository.PlanRepository;
import com.taskpilot.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class StripeWebhookService {

    private static final Logger logger = LoggerFactory.getLogger(StripeWebhookService.class);

    private final UserRepository userRepository;
    private final PlanRepository planRepository;

    public StripeWebhookService(UserRepository userRepository, PlanRepository planRepository) {
        this.userRepository = userRepository;
        this.planRepository = planRepository;
    }

    /**
     * Main entry point for handling a verified Stripe event.
     * It delegates to specific methods based on the event type.
     *
     * @param event A verified Stripe Event object.
     */
    @Transactional
    public void handleStripeEvent(Event event) {
        String eventType = event.getType();
        StripeObject stripeObject = event.getDataObjectDeserializer().getObject().orElse(null);

        if (stripeObject == null) {
            logger.warn("Webhook event data object is null. Event ID: {}", event.getId());
            return;
        }

        logger.info("Handling Stripe event: {} ({})", eventType, event.getId());

        switch (eventType) {
            case "customer.subscription.created", "customer.subscription.updated":
                handleSubscriptionChange((Subscription) stripeObject);
                break;
            case "customer.subscription.deleted":
                handleSubscriptionCancellation((Subscription) stripeObject);
                break;
            // You can add other events like 'checkout.session.completed' for logging if needed
            default:
                logger.warn("Unhandled event type: {}", eventType);
        }
    }

    /**
     * Handles subscription creation or plan changes (upgrades/downgrades).
     *
     * @param subscription The Stripe Subscription object from the event.
     */
    private void handleSubscriptionChange(Subscription subscription) {
        // Retrieve the customer from Stripe to get their email address
        User user = findUserByStripeCustomerId(subscription.getCustomer());
        if (user == null) {
            return; // Error is logged in the helper method
        }

        // Get the price ID from the first item in the subscription
        String priceId = subscription.getItems().getData().getFirst().getPrice().getId();
        Plan newPlan = planRepository.findByStripePriceId(priceId)
                .orElse(null);

        if (newPlan == null) {
            logger.error("Received subscription update for user {} with an unknown Stripe Price ID: {}", user.getEmail(), priceId);
            return;
        }

        user.setPlan(newPlan);
        // Reset their request quota and refresh date here
         user.setRequestsInCurrentMonth(0);
         user.setPlanRefreshDate(LocalDate.now());
        userRepository.save(user);
        logger.info("Successfully updated plan for user {} to '{}'", user.getEmail(), newPlan.getName());
    }

    /**
     * Handles subscription cancellations.
     *
     * @param subscription The Stripe Subscription object from the event.
     */
    private void handleSubscriptionCancellation(Subscription subscription) {
        User user = findUserByStripeCustomerId(subscription.getCustomer());
        if (user == null) {
            return; // Error is logged in the helper method
        }

        // Find the default "Free" plan to downgrade the user
        Plan freePlan = planRepository.findByName("Free").orElse(null);
        if (freePlan == null) {
            logger.error("CRITICAL: Could not find the default 'Free' plan to downgrade user {}.", user.getEmail());
            return;
        }

        user.setPlan(freePlan);
        userRepository.save(user);
        logger.info("User {} downgraded to '{}' plan due to subscription cancellation.", user.getEmail(), freePlan.getName());
    }

    /**
     * Helper method to find a User in your database using the Stripe Customer ID.
     * It makes an API call to Stripe to get the customer's email.
     *
     * @param stripeCustomerId The customer ID from the Stripe event (e.g., "cus_...").
     * @return The User entity or null if not found.
     */
    private User findUserByStripeCustomerId(String stripeCustomerId) {
        try {
            Customer customer = Customer.retrieve(stripeCustomerId);
            String userEmail = customer.getEmail();
            return userRepository.findByEmail(userEmail)
                    .orElseGet(() -> {
                        logger.warn("Received webhook for email '{}' but no matching user was found in the database.", userEmail);
                        return null;
                    });
        } catch (StripeException e) {
            logger.error("Stripe API error: Could not retrieve customer {} to find user by email.", stripeCustomerId, e);
            return null;
        }
    }
}