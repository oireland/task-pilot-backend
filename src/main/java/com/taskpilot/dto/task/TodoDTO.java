package com.taskpilot.dto.task;

import java.time.LocalDateTime;

public record TodoDTO(
        Long id,
        String content,
        boolean checked,
        LocalDateTime deadline
) {}