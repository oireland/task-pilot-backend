package com.taskpilot.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;

@Validated
public record RegisterUserDTO(
        @NotBlank(message = "email is required")
        @Email(message = "email must be a valid email address")
        String email,

        @NotBlank(message = "password is required")
        @Size(min = 8, message = "password must contain at least 8 characters")
        @Pattern(regexp = ".*\\d.*", message = "password must contain at least one digit")
        @Pattern(regexp = ".*[^A-Za-z0-9].*", message = "password must contain at least one special character")
        String password
) {
}