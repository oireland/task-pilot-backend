package com.taskpilot.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record TokenRefreshRequestDTO(
        @NotBlank
        String refreshToken
) {}
