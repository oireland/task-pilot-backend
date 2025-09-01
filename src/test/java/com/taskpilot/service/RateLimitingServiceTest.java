package com.taskpilot.service;

import com.taskpilot.model.Plan;
import com.taskpilot.model.User;
import com.taskpilot.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RateLimitingServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RateLimitingService rateLimitingService;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Create a plan with specific limits for testing.
        Plan testPlan = new Plan("Test Plan", 100, 10, 1000000, null); // 100/month, 10/day
        testUser = new User("user@example.com", "password");
        testUser.setPlan(testPlan);
    }

    @Test
    @DisplayName("isRequestAllowed() should return true and increment counts when user is under limits")
    void isRequestAllowed_shouldReturnTrue_whenUnderLimits() {
        // ARRANGE: Set user's current usage to be well below the limits.
        testUser.setRequestsInCurrentDay(5);
        testUser.setRequestsInCurrentMonth(50);

        // ACT
        boolean isAllowed = rateLimitingService.isRequestAllowed(testUser);

        // ASSERT
        assertTrue(isAllowed);

        // VERIFY: Capture the user object passed to save() and check that counts were incremented.
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals(6, savedUser.getRequestsInCurrentDay());
        assertEquals(51, savedUser.getRequestsInCurrentMonth());
    }

    @Test
    @DisplayName("isRequestAllowed() should return false when daily limit is reached")
    void isRequestAllowed_shouldReturnFalse_whenDailyLimitReached() {
        // ARRANGE: Set user's daily usage to the maximum limit.
        testUser.setRequestsInCurrentDay(10);
        testUser.setRequestsInCurrentMonth(50);

        // ACT
        boolean isAllowed = rateLimitingService.isRequestAllowed(testUser);

        // ASSERT
        assertFalse(isAllowed);
    }

    @Test
    @DisplayName("isRequestAllowed() should return false when monthly limit is reached")
    void isRequestAllowed_shouldReturnFalse_whenMonthlyLimitReached() {
        // ARRANGE: Set user's monthly usage to the maximum limit.
        testUser.setRequestsInCurrentDay(5);
        testUser.setRequestsInCurrentMonth(100);

        // ACT
        boolean isAllowed = rateLimitingService.isRequestAllowed(testUser);

        // ASSERT
        assertFalse(isAllowed);
    }
}