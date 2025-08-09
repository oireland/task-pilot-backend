package com.taskpilot.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginUserDTO(
     @NotBlank(message = "email is required")
     String email,
     @NotBlank(message = "password is required")
     String password
) {}

