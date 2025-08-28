// services/NotionService.java

package com.taskpilot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.taskpilot.dto.notion.DatabaseInfoDTO;
import com.taskpilot.dto.notion.NotionApiV1;
import com.taskpilot.dto.notion.NotionSearchResponse;
import com.taskpilot.dto.notion.NotionTokenResponse;
import com.taskpilot.dto.task.TaskListDTO;
import com.taskpilot.dto.task.TodoDTO;
import com.taskpilot.exception.InvalidDatabaseSchemaException;
import com.taskpilot.exception.ResourceNotFoundException;
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
import reactor.core.publisher.Flux;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class NotionService {

    private final TaskService taskService;
    @Value("${notion.client.id}")
    private String notionClientId;

    @Value("${notion.client.secret}")
    private String notionClientSecret;

    @Value("${notion.redirect.uri}")
    private String notionRedirectUri;

    private final RestTemplate restTemplate;
    private final UserService userService;
    private final EncryptionService encryptionService;
    private final WebClient.Builder webClientBuilder;
    private final Logger logger = LoggerFactory.getLogger(NotionService.class);

    public NotionService(UserService userService, EncryptionService encryptionService, WebClient.Builder webClientBuilder, RestTemplate restTemplate, TaskService taskService) {
        this.userService = userService;
        this.encryptionService = encryptionService;
        this.webClientBuilder = webClientBuilder;
        this.restTemplate = restTemplate;
        this.taskService = taskService;
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

    public void createTaskListPage(Long taskListId, User user) {
        String accessToken = encryptionService.decrypt(user.getNotionAccessToken());
        String databaseId = user.getNotionTargetDatabaseId();

        if (databaseId == null) {
            throw new IllegalStateException("User has not selected a target Notion database.");
        }

        // Fetch the TaskList from the database
        TaskListDTO taskList = taskService.getTaskListByIdForUser(taskListId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Task list not found"));

        WebClient webClient = webClientBuilder
                .baseUrl("https://api.notion.com")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .defaultHeader("Notion-Version", "2022-06-28")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        // Calculate the Status from the number of checked tasks
        long todosCount = taskList.todos().stream().filter(TodoDTO::checked).count();

        String status = "In Progress";
        if (todosCount == 0) {
            status = "Not Started";
        } else if (todosCount == taskList.todos().size()) {
            status = "Done";
        }

        try {
            // --- STEP 1: Create the new, empty page in the parent database ---
            var createPageRequest = NotionApiV1.buildCreatePageRequest(
                    taskList.title(),
                    status,
                    taskList.description(),
                    databaseId
            );

            // Send the request and chain the next steps
            webClient.post()
                    .uri("/v1/pages")
                    .bodyValue(createPageRequest)
                    .retrieve()
                    .bodyToMono(JsonNode.class) // Deserialize the response to a JsonNode
                    .flatMap(pageResponse -> {
                        // Extract the new page's ID from the response
                        String newPageId = pageResponse.get("id").asText();

                        // --- STEP 2: Create the new database on the page we just created ---
                        var createDbRequest = NotionApiV1.buildCreateDatabaseRequest(newPageId);

                        return webClient.post()
                                .uri("/v1/databases")
                                .bodyValue(createDbRequest)
                                .retrieve()
                                .bodyToMono(JsonNode.class);
                    })
                    .flatMap(dbResponse -> {
                        // Extract the new database's ID from the response
                        String newDatabaseId = dbResponse.get("id").asText();

                        // --- STEP 3: Create a page for each todo item in the new database ---
                        // Use concatMap for sequential requests, to prevent Notion from flagging a conflict
                        return Flux.fromIterable(taskList.todos().reversed())
                                .concatMap(todo -> {
                                    var addTodoRequest = NotionApiV1.buildAddTodoRequest(
                                            todo.content(),
                                            todo.checked() ? "Completed" : "Not Started",
                                            todo.deadline() != null ? todo.deadline().format(DateTimeFormatter.ISO_LOCAL_DATE) : null,
                                            newDatabaseId
                                    );
                                    return webClient.post()
                                            .uri("/v1/pages")
                                            .bodyValue(addTodoRequest)
                                            .retrieve()
                                            .toBodilessEntity(); // We don't need the response body here
                                })
                                .collectList(); // Wait for all to-do creation requests to complete
                    })
                    .block(); // Block until the entire chain is complete

        } catch (WebClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                logger.error("Notion API validation error: {}", e.getResponseBodyAsString());
                throw new InvalidDatabaseSchemaException(
                        "The selected Notion database has an invalid schema or another validation error occurred."
                );
            }
            throw e;
        }
    }
}