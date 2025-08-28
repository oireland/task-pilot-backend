package com.taskpilot.service;

import com.stripe.model.*;
import com.taskpilot.model.Plan;
import com.taskpilot.model.User;
import com.taskpilot.repository.PlanRepository;
import com.taskpilot.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StripeWebhookServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PlanRepository planRepository;

    @InjectMocks
    private StripeWebhookService stripeWebhookService;

    private User testUser;
    private Plan freePlan;
    private Plan proPlan;

    @BeforeEach
    void setUp() {
        // Create reusable test data
        proPlan = new Plan("Pro", 100, 15, List.of("price_pro_123"));
        freePlan = new Plan("Free", 15, 3, List.of());
        testUser = new User("customer@example.com", "password");
        testUser.setPlan(freePlan); // User starts on the free plan

        // Manually inject the @Value-annotated static field
        ReflectionTestUtils.setField(stripeWebhookService, "FREE_PLAN_NAME", "Free");
    }

    /**
     * Helper method to create a mock Stripe Event with a Subscription object.
     */
    private Event createMockEvent(String eventType, Subscription subscription) {
        Event event = mock(Event.class);
        when(event.getType()).thenReturn(eventType);

        // Mock the deserializer chain to return our subscription object
        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(event.getDataObjectDeserializer()).thenReturn(deserializer);
        when(deserializer.getObject()).thenReturn(Optional.of(subscription));

        return event;
    }

    @Test
    @DisplayName("handleStripeEvent should upgrade user on 'customer.subscription.created'")
    void handleStripeEvent_shouldUpgradeUserOnSubscriptionCreated() {
        // ARRANGE
        // 1. Mock the Stripe Subscription object
        Subscription subscription = mock(Subscription.class);
        Price price = mock(Price.class);
        SubscriptionItemCollection items = mock(SubscriptionItemCollection.class);
        SubscriptionItem item = mock(SubscriptionItem.class);

        when(subscription.getCustomer()).thenReturn("cus_123");
        when(subscription.getItems()).thenReturn(items);
        when(items.getData()).thenReturn(List.of(item));
        when(item.getPrice()).thenReturn(price);
        when(price.getId()).thenReturn("price_pro_123"); // The price ID for the Pro plan

        // 2. Create the mock event
        Event event = createMockEvent("customer.subscription.created", subscription);

        // 3. Mock the static Customer.retrieve call
        try (MockedStatic<Customer> mockedCustomer = Mockito.mockStatic(Customer.class)) {
            Customer customer = mock(Customer.class);
            when(customer.getEmail()).thenReturn("customer@example.com");
            mockedCustomer.when(() -> Customer.retrieve("cus_123")).thenReturn(customer);

            // 4. Mock repository calls
            when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(testUser));
            when(planRepository.findByStripePriceId("price_pro_123")).thenReturn(Optional.of(proPlan));

            // ACT
            stripeWebhookService.handleStripeEvent(event);

            // ASSERT & VERIFY
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());

            User savedUser = userCaptor.getValue();
            assertEquals("Pro", savedUser.getPlan().getName()); // Check that the plan was upgraded
            assertEquals(0, savedUser.getRequestsInCurrentDay()); // Check that usage was reset
        }
    }

    @Test
    @DisplayName("handleStripeEvent should downgrade user on 'customer.subscription.deleted'")
    void handleStripeEvent_shouldDowngradeUserOnSubscriptionDeleted() {
        // ARRANGE
        testUser.setPlan(proPlan); // User starts on the Pro plan
        Subscription subscription = mock(Subscription.class);
        when(subscription.getCustomer()).thenReturn("cus_123");
        Event event = createMockEvent("customer.subscription.deleted", subscription);

        try (MockedStatic<Customer> mockedCustomer = Mockito.mockStatic(Customer.class)) {
            Customer customer = mock(Customer.class);
            when(customer.getEmail()).thenReturn("customer@example.com");
            mockedCustomer.when(() -> Customer.retrieve("cus_123")).thenReturn(customer);

            when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(testUser));
            when(planRepository.findByName("Free")).thenReturn(Optional.of(freePlan));

            // ACT
            stripeWebhookService.handleStripeEvent(event);

            // ASSERT & VERIFY
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());

            assertEquals("Free", userCaptor.getValue().getPlan().getName());
        }
    }

    @Test
    @DisplayName("handleStripeEvent should do nothing if 'cancel_at_period_end' is true")
    void handleStripeEvent_shouldDoNothingWhenCancelAtPeriodEndIsTrue() {
        // ARRANGE
        Subscription subscription = mock(Subscription.class);
        when(subscription.getCustomer()).thenReturn("cus_123");
        when(subscription.getCancelAtPeriodEnd()).thenReturn(true); // Key condition for this test
        Event event = createMockEvent("customer.subscription.updated", subscription);

        try (MockedStatic<Customer> mockedCustomer = Mockito.mockStatic(Customer.class)) {
            Customer customer = mock(Customer.class);
            when(customer.getEmail()).thenReturn("customer@example.com");
            mockedCustomer.when(() -> Customer.retrieve("cus_123")).thenReturn(customer);

            when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(testUser));

            // ACT
            stripeWebhookService.handleStripeEvent(event);

            // ASSERT & VERIFY
            // The most important check: ensure the user was NOT saved.
            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Test
    @DisplayName("handleStripeEvent should downgrade user if subscription is paused")
    void handleStripeEvent_shouldDowngradeWhenSubscriptionIsPaused() {
        // ARRANGE
        Subscription subscription = mock(Subscription.class);
        when(subscription.getCustomer()).thenReturn("cus_123");
        when(subscription.getStatus()).thenReturn("paused"); // Key condition
        Event event = createMockEvent("customer.subscription.updated", subscription);

        try (MockedStatic<Customer> mockedCustomer = Mockito.mockStatic(Customer.class)) {
            Customer customer = mock(Customer.class);
            when(customer.getEmail()).thenReturn("customer@example.com");
            mockedCustomer.when(() -> Customer.retrieve("cus_123")).thenReturn(customer);

            when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(testUser));
            when(planRepository.findByName("Free")).thenReturn(Optional.of(freePlan));

            // ACT
            stripeWebhookService.handleStripeEvent(event);

            // ASSERT & VERIFY
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertEquals("Free", userCaptor.getValue().getPlan().getName());
        }
    }
}