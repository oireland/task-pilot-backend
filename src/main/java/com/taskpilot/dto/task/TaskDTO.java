package com.taskpilot.dto.task;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A Data Transfer Object representing a Task list for API responses.
 */
public record TaskDTO(
        Long id,
        String title,
        String description,
        List<String> items,
        LocalDateTime createdAt
) {}