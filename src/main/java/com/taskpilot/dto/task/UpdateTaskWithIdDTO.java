package com.taskpilot.dto.task;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateTaskWithIdDTO(
        @NotNull Long id,
        String title,
        String description,
        List<TodoDTO> todos
) {}