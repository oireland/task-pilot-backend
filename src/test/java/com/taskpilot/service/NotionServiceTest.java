package com.taskpilot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskpilot.dto.notion.DatabaseInfoDTO;
import com.taskpilot.dto.notion.NotionDatabaseResult;
import com.taskpilot.dto.notion.NotionSearchResponse;
import com.taskpilot.dto.notion.NotionTokenResponse;
import com.taskpilot.dto.task.TaskListDTO;
import com.taskpilot.dto.task.TodoDTO;
import com.taskpilot.exception.InvalidDatabaseSchemaException;
import com.taskpilot.exception.ResourceNotFoundException;
import com.taskpilot.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotionServiceTest {

    // --- Mocks for all dependencies ---
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private UserService userService;
    @Mock
    private EncryptionService encryptionService;
    @Mock
    private WebClient.Builder webClientBuilder;
    @Mock
    private TaskService taskService;

    // --- Mocks for the WebClient chain ---
    @Mock
    private WebClient webClient;
    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock
    private WebClient.RequestBodySpec requestBodySpec;
    @Mock
    private WebClient.RequestHeadersSpec<?> requestHeadersSpec;
    @Mock
    private WebClient.ResponseSpec responseSpec;

    // --- The service we are testing ---
    @InjectMocks
    private NotionService notionService;

    // --- Reusable test data ---
    private User testUser;
    private TaskListDTO taskListDTO;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // This method runs before each test to set up common objects.
        testUser = new User("user@example.com", "password");
        testUser.setId(1L);

        taskListDTO = new TaskListDTO(1L, "My Tasks", "A description",
                List.of(new TodoDTO(1L, "Task 1", false, LocalDateTime.now())),
                null, null);

        objectMapper = new ObjectMapper();

        // Manually set the values for the @Value-annotated fields.
        ReflectionTestUtils.setField(notionService, "notionClientId", "test-client-id");
        ReflectionTestUtils.setField(notionService, "notionClientSecret", "test-client-secret");
        ReflectionTestUtils.setField(notionService, "notionRedirectUri", "http://localhost/redirect");
    }

    // --- Tests for exchangeCodeAndSaveToken method ---

    @Test
    @DisplayName("exchangeCodeAndSaveToken() should call Notion and save the token")
    void exchangeCodeAndSaveToken_shouldSucceed() {
        // ARRANGE
        NotionTokenResponse mockResponse = new NotionTokenResponse(
                "new-access-token", "ws-123", "My Workspace", "icon.png", "bot-456", null
        );
        ResponseEntity<NotionTokenResponse> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(NotionTokenResponse.class)))
                .thenReturn(responseEntity);

        // ACT
        notionService.exchangeCodeAndSaveToken("auth-code-123", testUser);

        // VERIFY
        verify(userService).saveNotionIntegrationDetails(
                eq(1L),
                eq("new-access-token"),
                eq("ws-123"),
                eq("My Workspace"),
                eq("icon.png"),
                eq("bot-456")
        );
    }

    // --- Tests for getAvailableDatabases method ---

    @Test
    @DisplayName("getAvailableDatabases() should decrypt token and return list of databases")
    void getAvailableDatabases_shouldReturnDatabases() {
        // ARRANGE
        when(encryptionService.decrypt("encrypted-token")).thenReturn("decrypted-token");
        NotionSearchResponse mockResponse = new NotionSearchResponse(
                "list", List.of(new NotionDatabaseResult("database", "db-1", List.of())), null, false
        );
        ResponseEntity<NotionSearchResponse> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        when(restTemplate.postForEntity(anyString(), any(), eq(NotionSearchResponse.class))).thenReturn(responseEntity);

        // ACT
        List<DatabaseInfoDTO> databases = notionService.getAvailableDatabases("encrypted-token");

        // ASSERT
        assertFalse(databases.isEmpty());
        assertEquals("db-1", databases.getFirst().id());
    }

    // --- Tests for createTaskListPage method ---

    @Test
    @DisplayName("createTaskListPage() should build and send request to Notion")
    void createTaskListPage_shouldSucceed() {
        // ARRANGE
        testUser.setNotionTargetDatabaseId("db-123");
        when(encryptionService.decrypt(any())).thenReturn("decrypted-token");
        when(taskService.getTaskListByIdForUser(eq(1L), eq(testUser))).thenReturn(Optional.of(taskListDTO));

        // Fix the TodoDTO in taskListDTO to use LocalDate instead of LocalDateTime
        taskListDTO = new TaskListDTO(1L, "My Tasks", "A description",
                List.of(new TodoDTO(1L, "Task 1", false, LocalDateTime.now())),
                null, null);

        // Create mock JsonNode responses for the reactive chain
        ObjectMapper mapper = new ObjectMapper();
        JsonNode pageResponseNode = mapper.createObjectNode().put("id", "page-123");
        JsonNode dbResponseNode = mapper.createObjectNode().put("id", "db-123");

        // 1. Set up WebClient builder
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.defaultHeader(anyString(), anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);

        // 2. Mock WebClient for page creation
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(eq("/v1/pages"))).thenReturn(requestBodySpec);
        doReturn(requestHeadersSpec).when(requestBodySpec).bodyValue(any());
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(JsonNode.class)).thenReturn(Mono.just(pageResponseNode));

        // 3. Set up mock for database creation (the flatMap call)
        WebClient.RequestBodyUriSpec requestBodyUriSpec2 = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec2 = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec<?> requestHeadersSpec2 = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec2 = mock(WebClient.ResponseSpec.class);

        // Use consecutive calls pattern instead of chaining thenReturn
        when(webClient.post())
                .thenReturn(requestBodyUriSpec)
                .thenReturn(requestBodyUriSpec2);

        when(requestBodyUriSpec2.uri(eq("/v1/databases"))).thenReturn(requestBodySpec2);
        doReturn(requestHeadersSpec2).when(requestBodySpec2).bodyValue(any());
        when(requestHeadersSpec2.retrieve()).thenReturn(responseSpec2);
        when(responseSpec2.bodyToMono(JsonNode.class)).thenReturn(Mono.just(dbResponseNode));

        // 4. Set up mock for to-do item creation (the flatMap with Flux.concatMap)
        WebClient.RequestBodyUriSpec requestBodyUriSpec3 = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec3 = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec<?> requestHeadersSpec3 = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec3 = mock(WebClient.ResponseSpec.class);

        // Use different when block for the third call
        when(webClient.post())
                .thenReturn(requestBodyUriSpec)
                .thenReturn(requestBodyUriSpec2)
                .thenReturn(requestBodyUriSpec3);

        when(requestBodyUriSpec3.uri(eq("/v1/pages"))).thenReturn(requestBodySpec3);
        doReturn(requestHeadersSpec3).when(requestBodySpec3).bodyValue(any());

        when(requestHeadersSpec3.retrieve()).thenReturn(responseSpec3);
        when(responseSpec3.toBodilessEntity()).thenReturn(Mono.empty());

        // ACT
        notionService.createTaskListPage(taskListDTO.id(), testUser);

        // VERIFY
        verify(requestBodySpec).bodyValue(any());
    }

    @Test
    @DisplayName("createTaskListPage() should throw IllegalStateException if no database is selected")
    void createTaskListPage_shouldThrowExceptionIfNoDatabaseId() {
        // ARRANGE
        testUser.setNotionTargetDatabaseId(null);
        when(encryptionService.decrypt(any())).thenReturn("decrypted-token");

        // ACT & ASSERT
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> notionService.createTaskListPage(taskListDTO.id(), testUser));

        assertEquals("User has not selected a target Notion database.", exception.getMessage());
    }

    @Test
    @DisplayName("createTaskListPage() should throw ResourceNotFoundException when task list not found")
    void createTaskListPage_shouldThrowResourceNotFoundExceptionWhenTaskListNotFound() {
        // ARRANGE
        testUser.setNotionTargetDatabaseId("db-123");
        when(encryptionService.decrypt(any())).thenReturn("decrypted-token");
        when(taskService.getTaskListByIdForUser(eq(1L), eq(testUser))).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThrows(ResourceNotFoundException.class,
                () -> notionService.createTaskListPage(taskListDTO.id(), testUser));
    }

    @Test
    @DisplayName("createTaskListPage() should throw InvalidDatabaseSchemaException on 400 Bad Request")
    void createTaskListPage_shouldThrowCustomExceptionOnBadRequest() {
        // ARRANGE
        testUser.setNotionTargetDatabaseId("db-123");
        when(encryptionService.decrypt(any())).thenReturn("decrypted-token");
        when(taskService.getTaskListByIdForUser(eq(1L), eq(testUser))).thenReturn(Optional.of(taskListDTO));

        // Mock WebClient setup
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.defaultHeader(any(), any())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/v1/pages")).thenReturn(requestBodySpec);
        doReturn(requestHeadersSpec).when(requestBodySpec).bodyValue(any());


        // Mock the retrieve() call to throw the exception
        String errorBody = "{\"message\":\"validation error\"}";
        when(requestHeadersSpec.retrieve()).thenThrow(
                new WebClientResponseException(
                        HttpStatus.BAD_REQUEST.value(),
                        "Bad Request",
                        new HttpHeaders(),
                        errorBody.getBytes(StandardCharsets.UTF_8),
                        StandardCharsets.UTF_8
                )
        );

        // ACT & ASSERT
        InvalidDatabaseSchemaException exception = assertThrows(InvalidDatabaseSchemaException.class,
                () -> notionService.createTaskListPage(taskListDTO.id(), testUser));

        assertTrue(exception.getMessage().contains("invalid schema"));
    }
}