package com.taskpilot.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;

@Validated
public record VerifyUserDTO(
        @NotBlank(message = "email is required")
        String email,
        @NotBlank(message = "verificationCode is required") @Size(min = 6, max = 6, message = "verificationCode must be 6 digits long")
        String verificationCode
) {}



