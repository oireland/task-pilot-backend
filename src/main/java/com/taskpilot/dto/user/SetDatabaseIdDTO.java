package com.taskpilot.dto.user;

import jakarta.validation.constraints.NotBlank;

public record SetDatabaseIdDTO(
        @NotBlank(message = "databaseId cannot be null or empty")
        String databaseId,
        @NotBlank(message = "databaseName cannot be null")
        String databaseName
) {
}
