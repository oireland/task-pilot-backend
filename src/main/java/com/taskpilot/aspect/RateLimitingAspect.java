package com.taskpilot.aspect;

import com.taskpilot.model.User;
import com.taskpilot.service.RateLimitingService;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Aspect
@Component
public class RateLimitingAspect {

    private final RateLimitingService rateLimitingService;

    public RateLimitingAspect(RateLimitingService rateLimitingService) {
        this.rateLimitingService = rateLimitingService;
    }

    // This expression targets all methods in your TaskController
    @Before("execution(* com.taskpilot.controller.TaskController.*(..))")
    public void checkRateLimit() {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (!rateLimitingService.isRequestAllowed(currentUser)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "You have exceeded either your daily or monthly request limit.");
        }
    }
}