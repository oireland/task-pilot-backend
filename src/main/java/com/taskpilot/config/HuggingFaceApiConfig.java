package com.taskpilot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "huggingface.api")
public record HuggingFaceApiConfig(String baseUrl, String token) {
}
