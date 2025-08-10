package com.taskpilot.service;

import com.taskpilot.model.User;
import com.taskpilot.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RateLimitingService {

    private final UserRepository userRepository;

    public RateLimitingService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public boolean isRequestAllowed(User user) {
        // Check if current usage is less than the plan's limit
        if (user.getRequestsInCurrentPeriod() < user.getPlan().getRequestsPerMonth()) {
            // Increment the usage count
            user.setRequestsInCurrentPeriod(user.getRequestsInCurrentPeriod() + 1);
            userRepository.save(user);
            return true;
        }
        // If usage has reached the limit, deny the request
        return false;
    }
}