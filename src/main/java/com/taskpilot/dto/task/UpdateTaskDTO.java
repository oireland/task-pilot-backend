package com.taskpilot.dto.task;

import java.util.List;

/**
 * A Data Transfer Object for updating an existing Task.
 * Contains only the fields that a user is allowed to modify.
 */
public record UpdateTaskDTO(
        String title,
        String description,
        List<String> items
) {}