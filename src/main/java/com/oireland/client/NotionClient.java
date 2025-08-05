package com.oireland.client;

import com.oireland.config.NotionApiConfig;
import com.oireland.dto.TaskDTO;
import com.oireland.notion.NotionApiV1;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class NotionClient {

    private final WebClient webClient;
    private final NotionApiConfig config;

    public NotionClient(WebClient.Builder webClientBuilder, NotionApiConfig config) {
        this.config = config;
        this.webClient = webClientBuilder
                .baseUrl("https://api.notion.com") // Notion API base URL
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.token())
                .defaultHeader("Notion-Version", config.version()) // Notion-specific header
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public void createTask(TaskDTO task) {
        var requestBody = getPageCreateRequest(task);

        // Make the API call
        webClient
                .post()
                .uri("/v1/pages") // The endpoint for creating pages
                .bodyValue(requestBody)
                .retrieve()
                .toBodilessEntity() // We don't need the response body, just success/fail
                .block(); // Wait for the operation to complete
    }

    private NotionApiV1.PageCreateRequest getPageCreateRequest(TaskDTO task) {
        // Build the 'properties' object using our API DTOs
        var title = new NotionApiV1.TitleProperty(task.taskName());
        var status = new NotionApiV1.StatusProperty(task.status());
        var description = new NotionApiV1.RichTextProperty(task.description());

        var properties = new NotionApiV1.Properties(
                Map.of("title", title.toRequestFormat()),
                status,
                Map.of("rich_text", description.toRequestFormat())
        );

        // Build the 'parent' object
        var parent = new NotionApiV1.Parent(config.databaseId());

        // Build the final request body
        return new NotionApiV1.PageCreateRequest(parent, properties);
    }
}
