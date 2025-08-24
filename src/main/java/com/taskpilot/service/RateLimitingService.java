package com.taskpilot.service;

import com.taskpilot.model.User;
import com.taskpilot.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RateLimitingService {

    private final Logger logger = LoggerFactory.getLogger(RateLimitingService.class);
    private final UserRepository userRepository;

    public RateLimitingService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public boolean isRequestAllowed(User user) {
        // Check both daily and monthly limits
        boolean underDailyLimit = user.getRequestsInCurrentDay() < user.getPlan().getRequestsPerDay();
        boolean underMonthlyLimit = user.getRequestsInCurrentMonth() < user.getPlan().getRequestsPerMonth();

        if (underDailyLimit && underMonthlyLimit) {
            // Increment both usage counts
            user.setRequestsInCurrentDay(user.getRequestsInCurrentDay() + 1);
            user.setRequestsInCurrentMonth(user.getRequestsInCurrentMonth() + 1);
            userRepository.save(user);
            return true;
        }
        logger.warn("Rate limit exceeded for user '{}'.", user.getEmail());
        return false;
    }
}