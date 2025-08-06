package com.oireland.client;

import com.oireland.config.NotionApiConfig;
import com.oireland.model.NotionApiV1;
import com.oireland.model.TaskDTO;
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

    public void createTask(TaskDTO task) {
        logger.info("Creating task in Notion: {}", task.taskName());
        var requestBody = NotionApiV1.buildCreateTaskRequest(task, config.databaseId());
        logger.info("Request body for Notion API: {}", requestBody);

        // Make the API call
        webClient
                .post()
                .uri("/v1/pages") // The endpoint for creating pages
                .bodyValue(requestBody)
                .retrieve()
                .toBodilessEntity() // We don't need the response body, just success/fail
                .block(); // Wait for the operation to complete
    }

}
