package com.taskpilot.service;

import com.taskpilot.model.User;
import com.taskpilot.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuotaResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private QuotaResetService quotaResetService;

    @Test
    @DisplayName("resetUserQuotas() should reset daily and applicable monthly quotas")
    void resetUserQuotas_shouldResetQuotas() {
        // ARRANGE
        // Create two users whose monthly quotas are due for a reset.
        User user1 = new User("user1@example.com", "pass");
        user1.setRequestsInCurrentMonth(50);
        user1.setPlanRefreshDate(LocalDate.now().minusDays(1)); // Due for reset

        User user2 = new User("user2@example.com", "pass");
        user2.setRequestsInCurrentMonth(80);
        user2.setPlanRefreshDate(LocalDate.now().minusMonths(1)); // Due for reset

        // Mock the repository to return these two users.
        when(userRepository.findByPlanRefreshDateBefore(any(LocalDate.class)))
                .thenReturn(List.of(user1, user2));

        // ACT: Run the quota reset job.
        quotaResetService.resetUserQuotas();

        // VERIFY
        // 1. Check that the daily count reset method was called exactly once.
        verify(userRepository, times(1)).resetAllDailyCounts();

        // 2. Capture the user objects passed to the save method.
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        // We expect save to be called twice, once for each user.
        verify(userRepository, times(2)).save(userCaptor.capture());

        // 3. Inspect the saved users to ensure their quotas were reset correctly.
        List<User> savedUsers = userCaptor.getAllValues();
        // Check user 1
        assertEquals(0, savedUsers.get(0).getRequestsInCurrentMonth());
        assertEquals(LocalDate.now().plusMonths(1), savedUsers.get(0).getPlanRefreshDate());
        // Check user 2
        assertEquals(0, savedUsers.get(1).getRequestsInCurrentMonth());
        assertEquals(LocalDate.now().plusMonths(1), savedUsers.get(1).getPlanRefreshDate());
    }
}