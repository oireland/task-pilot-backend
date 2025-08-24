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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class StripeWebhookService {

    private static final Logger logger = LoggerFactory.getLogger(StripeWebhookService.class);

    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    @Value("${plan.free.name}")
    private static String FREE_PLAN_NAME;


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
        Optional<StripeObject> stripeObjectOptional = event.getDataObjectDeserializer().getObject();

        if (stripeObjectOptional.isEmpty()) {
            logger.warn("Webhook event data object is null. Event ID: {}", event.getId());
            return;
        }

        StripeObject stripeObject = stripeObjectOptional.get();
        logger.info("Handling Stripe event: {} ({})", eventType, event.getId());

        switch (eventType) {
            case "customer.subscription.created":
                handleSubscriptionCreation((Subscription) stripeObject);
                break;
            case "customer.subscription.updated":
                handleSubscriptionUpdate((Subscription) stripeObject);
                break;
            case "customer.subscription.deleted":
                handleSubscriptionCancellation((Subscription) stripeObject);
                break;
            default:
                logger.warn("Unhandled event type: {}", eventType);
        }
    }

    /**
     * Handles brand-new subscription creations.
     */
    private void handleSubscriptionCreation(Subscription subscription) {
        updateUserPlanFromSubscription(subscription, "created");
    }

    /**
     * Handles a variety of subscription updates:
     * - Plan changes (upgrade/downgrade)
     * - Pauses and resumes
     * - Pending cancellations (when a user cancels but the period hasn't ended)
     */
    private void handleSubscriptionUpdate(Subscription subscription) {
        User user = findUserByStripeCustomerId(subscription.getCustomer());
        if (user == null) return;

        // Scenario 1: The subscription is scheduled for cancellation at the end of the period.
        // The user remains on their current plan until the period ends.
        if (subscription.getCancelAtPeriodEnd()) {
            logger.info("Subscription for user {} is scheduled to cancel at period end. No immediate plan change.", user.getEmail());
            // Optional: You could add a field to your User entity to store the expiration date
            // and display a message in your UI. The actual downgrade will happen on 'customer.subscription.deleted'.
            return;
        }

        // Scenario 2: The subscription is paused.
        boolean isPaused = "paused".equals(subscription.getStatus()) || subscription.getPauseCollection() != null;
        if (isPaused) {
            logger.info("Subscription for user {} has been paused.", user.getEmail());
            downgradeToFreePlan(user, "subscription pause");
            return;
        }

        // Scenario 3: The subscription is active. This handles resumes, upgrades, or downgrades.
        if ("active".equals(subscription.getStatus())) {
            updateUserPlanFromSubscription(subscription, "updated/resumed");
        } else {
            logger.warn("Received subscription update for user {} with unhandled status: {}", user.getEmail(), subscription.getStatus());
        }
    }

    /**
     * Handles the final subscription cancellation, which occurs after the billing period ends.
     */
    private void handleSubscriptionCancellation(Subscription subscription) {
        User user = findUserByStripeCustomerId(subscription.getCustomer());
        if (user == null) return;
        downgradeToFreePlan(user, "subscription cancellation");
    }

    /**
     * Helper method to find a User in your database using the Stripe Customer ID.
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

    /**
     * Centralized logic to update a user's plan based on an active subscription.
     * Resets usage quotas.
     */
    private void updateUserPlanFromSubscription(Subscription subscription, String context) {
        User user = findUserByStripeCustomerId(subscription.getCustomer());
        if (user == null) return;

        String priceId = subscription.getItems().getData().getFirst().getPrice().getId();
        Optional<Plan> newPlanOpt = planRepository.findByStripePriceId(priceId);

        if (newPlanOpt.isEmpty()) {
            logger.error("Received subscription {} for user {} with an unknown Stripe Price ID: {}", context, user.getEmail(), priceId);
            return;
        }

        Plan newPlan = newPlanOpt.get();
        user.setPlan(newPlan);
        user.setRequestsInCurrentMonth(0);
        user.setRequestsInCurrentDay(0);
        user.setPlanRefreshDate(LocalDate.now());
        userRepository.save(user);
        logger.info("Successfully {} plan for user {} to '{}'", context, user.getEmail(), newPlan.getName());
    }

    /**
     * Centralized logic to downgrade a user to the free plan.
     */
    private void downgradeToFreePlan(User user, String reason) {
        Plan freePlan = planRepository.findByName(FREE_PLAN_NAME)
                .orElse(null);

        if (freePlan == null) {
            logger.error("CRITICAL: Could not find the default '{}' plan to downgrade user {}.", FREE_PLAN_NAME, user.getEmail());
            return;
        }

        user.setPlan(freePlan);
        // Reset quotas upon downgrade
        user.setRequestsInCurrentMonth(0);
        user.setRequestsInCurrentDay(0);
        user.setPlanRefreshDate(LocalDate.now());
        userRepository.save(user);
        logger.info("User {} downgraded to '{}' plan due to {}.", user.getEmail(), freePlan.getName(), reason);
    }
}