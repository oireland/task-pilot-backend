package com.taskpilot.dto.task;

import java.time.LocalDateTime;
import java.util.List;

public record TaskListDTO(
        Long id,
        String title,
        String description,
        List<TodoDTO> todos,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}