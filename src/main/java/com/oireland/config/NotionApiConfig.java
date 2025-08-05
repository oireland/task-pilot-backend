package com.oireland.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notion.api")
public record NotionApiConfig(String token, String databaseId, String version) {
}
