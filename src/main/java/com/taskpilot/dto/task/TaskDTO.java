package com.taskpilot.dto.task;

import java.time.LocalDateTime;
import java.util.List;

public record TaskDTO(
        Long id,
        String title,
        String description,
        List<TodoDTO> todos,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}