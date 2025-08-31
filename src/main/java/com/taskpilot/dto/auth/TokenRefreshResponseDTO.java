package com.taskpilot.dto.auth;

public record TokenRefreshResponseDTO(String accessToken, String refreshToken) {
}
