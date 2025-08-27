package com.taskpilot.dto.task;

import jakarta.validation.Valid;

import java.util.List;

/**
 * DTO for updating an existing task list.
 * Replaces the list's metadata and items with the provided values.
 */
public record UpdateTaskDTO(
        String title,
        String description,
        @Valid
        List<TodoDTO> items
) {}