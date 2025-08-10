package com.taskpilot.service;

import com.taskpilot.model.User;
import com.taskpilot.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class QuotaResetService {

    private static final Logger logger = LoggerFactory.getLogger(QuotaResetService.class);
    private final UserRepository userRepository;

    public QuotaResetService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * This method runs every day at 1 AM server time.
     * It finds all users whose quota refresh date is today or in the past
     * and resets their request count.
     */
    @Scheduled(cron = "0 0 1 * * ?") // Cron for 1:00 AM daily
    @Transactional
    public void resetUserQuotas() {
        LocalDate today = LocalDate.now();
        logger.info("Running daily quota reset job for date: {}", today);

        // This requires adding a new method to your UserRepository
        List<User> usersToReset = userRepository.findByPlanRefreshDateBefore(today);

        for (User user : usersToReset) {
            user.setRequestsInCurrentPeriod(0);
            user.setPlanRefreshDate(today.plusMonths(1)); // Schedule next reset
            userRepository.save(user);
            logger.info("Reset quota for user: {}. Next reset: {}", user.getEmail(), user.getPlanRefreshDate());
        }

        logger.info("Finished daily quota reset job. Reset {} users.", usersToReset.size());
    }
}