// services/NotionService.java

package com.taskpilot.service;

import com.taskpilot.dto.notion.DatabaseInfoDTO;
import com.taskpilot.dto.notion.NotionApiV1;
import com.taskpilot.dto.notion.NotionSearchResponse;
import com.taskpilot.dto.notion.NotionTokenResponse;
import com.taskpilot.dto.task.ExtractedDocDataDTO;
import com.taskpilot.exception.InvalidDatabaseSchemaException;
import com.taskpilot.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class NotionService {

    @Value("${notion.client.id}")
    private String notionClientId;

    @Value("${notion.client.secret}")
    private String notionClientSecret;

    @Value("${notion.redirect.uri}")
    private String notionRedirectUri;

    private final RestTemplate restTemplate;
    private final UserService userService;
    private final EncryptionService encryptionService;
    private final Logger logger = LoggerFactory.getLogger(NotionService.class);

    public NotionService(UserService userService, EncryptionService encryptionService) {
        this.userService = userService;
        this.encryptionService = encryptionService;
        this.restTemplate = new RestTemplate();
    }

    public void exchangeCodeAndSaveToken(String code, User user) {
        String notionTokenUrl = "https://api.notion.com/v1/oauth/token";

        // 1. Create the Authorization header
        String authHeader = Base64.getEncoder().encodeToString(
                (notionClientId + ":" + notionClientSecret).getBytes()
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic " + authHeader);
        headers.set("Notion-Version", "2022-06-28");

        // 2. Create the request body
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("grant_type", "authorization_code");
        requestBody.put("code", code);
        requestBody.put("redirect_uri", notionRedirectUri);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

        // 3. Make the POST request to Notion's token endpoint
        ResponseEntity<NotionTokenResponse> response = restTemplate.postForEntity(
                notionTokenUrl, request, NotionTokenResponse.class
        );

        NotionTokenResponse tokenResponse = response.getBody();

        if (tokenResponse != null && response.getStatusCode() == HttpStatus.OK) {
            // 4. Call the user service to encrypt and save the details
            userService.saveNotionIntegrationDetails(
                    user.getId(),
                    tokenResponse.accessToken(),
                    tokenResponse.workspaceId(),
                    tokenResponse.workspaceName(),
                    tokenResponse.workspaceIcon(),
                    tokenResponse.botId()
            );
        } else {
            throw new RuntimeException("Failed to retrieve a valid token from Notion.");
        }
    }

    public List<DatabaseInfoDTO> getAvailableDatabases(String encryptedAccessToken) {
        String accessToken = encryptionService.decrypt(encryptedAccessToken);
        String searchUrl = "https://api.notion.com/v1/search";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.set("Notion-Version", "2022-06-28");
        headers.setContentType(MediaType.APPLICATION_JSON);

        String requestBody = "{\"filter\": {\"value\": \"database\", \"property\": \"object\"}}";
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<NotionSearchResponse> response = restTemplate.postForEntity(
                    searchUrl, request, NotionSearchResponse.class
            );

            NotionSearchResponse searchResponse = response.getBody();

            if (searchResponse == null || searchResponse.results() == null) {
                return Collections.emptyList();
            }

            return searchResponse.results().stream()
                    .map(dbResult -> {
                        String id = dbResult.id();
                        String name = "Untitled Database"; // Default name

                        // Safely get the title from the list of rich text objects
                        if (dbResult.title() != null && !dbResult.title().isEmpty()) {
                            // Use the plain_text from the first title object
                            name = dbResult.title().getFirst().plainText();
                        }
                        return new DatabaseInfoDTO(id, name);
                    })
                    .collect(Collectors.toList());

        } catch (HttpClientErrorException e) {
            logger.error("Error fetching databases from Notion: {}", e.getResponseBodyAsString());
            return Collections.emptyList();
        }
    }

    public void createTasksPage(ExtractedDocDataDTO docData, User user) {
        String accessToken = encryptionService.decrypt(user.getNotionAccessToken());
        String databaseId = user.getNotionTargetDatabaseId();

        if (databaseId == null) {
            throw new IllegalStateException("User has not selected a target Notion database.");
        }

        var requestBody = NotionApiV1.buildCreateTaskRequest(docData, databaseId);
        WebClient webClient = WebClient.builder()
                .baseUrl("https://api.notion.com")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .defaultHeader("Notion-Version", "2022-06-28")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        try {
            webClient.post()
                    .uri("/v1/pages")
                    .bodyValue(requestBody)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (WebClientResponseException e) {
            String responseBody = e.getResponseBodyAsString();
            logger.info("Response body from notion is {}", responseBody);
            // Check if the error is about a missing property (like "Status" or "Description")
            if (responseBody.contains("Unsaved transactions")) {
                throw new InvalidDatabaseSchemaException(
                        "The selected Notion database is missing required properties. Please ensure it has 'Status' and 'Description' columns."
                );
            }
            // Re-throw other errors
            throw e;
        }
    }
}