package com.taskpilot.dto.user;

public record PlanDTO(
        String name,
        int requestsPerDay,
        int requestsPerMonth
) {}