package com.taskpilot.client;

import com.taskpilot.config.NotionApiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class NotionClient {

    private final Logger logger = LoggerFactory.getLogger(NotionClient.class);
    private final WebClient webClient;
    private final NotionApiConfig config;

    public NotionClient(WebClient.Builder webClientBuilder, NotionApiConfig config) {
        this.config = config;
        this.webClient = webClientBuilder
                .baseUrl(config.baseUrl()) // Notion API base URL
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.token())
                .defaultHeader("Notion-Version", config.version()) // Notion-specific header
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

}
