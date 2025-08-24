package com.taskpilot.dto.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * DTO for creating a new task manually.
 *
 * @param title       The title of the task list.
 * @param description A brief description of the task list.
 * @param items       The list of individual task items.
 */
public record CreateTaskDTO(
        @NotBlank(message = "Title cannot be blank")
        @Size(max = 255, message = "Title cannot exceed 255 characters")
        String title,

        String description,

        @NotNull(message = "Items list cannot be null")
        List<String> items
) {}