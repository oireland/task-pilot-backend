package com.taskpilot.dto.user;

import jakarta.validation.constraints.NotBlank;

public record ExchangeCodeDTO(
        @NotBlank(message = "code cannot be null or empty")
        String code
) {
}
