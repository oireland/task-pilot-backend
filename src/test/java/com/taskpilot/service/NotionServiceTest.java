package com.taskpilot.service;

import com.taskpilot.dto.notion.DatabaseInfoDTO;
import com.taskpilot.dto.notion.NotionDatabaseResult;
import com.taskpilot.dto.notion.NotionSearchResponse;
import com.taskpilot.dto.notion.NotionTokenResponse;
import com.taskpilot.dto.task.TaskListDTO;
import com.taskpilot.dto.task.TodoDTO;
import com.taskpilot.exception.InvalidDatabaseSchemaException;
import com.taskpilot.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private WebClient.RequestHeadersSpec<?> requestHeadersSpec; // Correct generic type
    @Mock
    private WebClient.ResponseSpec responseSpec;


    // --- The service we are testing ---
    @InjectMocks
    private NotionService notionService;

    // --- Reusable test data ---
    private User testUser;
    private TaskListDTO taskListDTO;

    @BeforeEach
    void setUp() {
        // This method runs before each test to set up common objects.
        testUser = new User("user@example.com", "password");
        testUser.setId(1L);

        taskListDTO = new TaskListDTO(1L, "My Tasks", "A description", List.of(new TodoDTO(1L, "Task 1", false, null)), null, null);

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

    // --- Tests for createTasksPage method ---

    @Test
    @DisplayName("createTasksPage() should build and send request to Notion")
    void createTasksPage_shouldSucceed() {
        // ARRANGE
        testUser.setNotionTargetDatabaseId("db-123");
        when(encryptionService.decrypt(any())).thenReturn("decrypted-token");

        // 1. Mock the builder to return our mocked WebClient
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.defaultHeader(any(), any())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);

        // 2. Mock the call chain on the WebClient instance
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/v1/pages")).thenReturn(requestBodySpec);
        doReturn(requestHeadersSpec).when(requestBodySpec).bodyValue(any());
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(Mono.empty());

        // 3. Mock the taskService
        when(taskService.getTaskListByIdForUser(anyLong(), any(User.class))).thenReturn(java.util.Optional.of(taskListDTO));

        // ACT
        notionService.createTaskListPage(taskListDTO.id(), testUser);

        // VERIFY that the final part of the chain was called
        verify(responseSpec).toBodilessEntity();
    }


    @Test
    @DisplayName("createTasksPage() should throw IllegalStateException if no database is selected")
    void createTasksPage_shouldThrowExceptionIfNoDatabaseId() {
        // ARRANGE
        testUser.setNotionTargetDatabaseId(null);
        when(encryptionService.decrypt(any())).thenReturn("decrypted-token");

        // ACT & ASSERT
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> notionService.createTaskListPage(taskListDTO.id(), testUser));

        assertEquals("User has not selected a target Notion database.", exception.getMessage());
    }

    @Test
    @DisplayName("createTasksPage() should throw InvalidDatabaseSchemaException on 400 Bad Request")
    void createTasksPage_shouldThrowCustomExceptionOnBadRequest() {
        // ARRANGE
        testUser.setNotionTargetDatabaseId("db-123");
        when(encryptionService.decrypt(any())).thenReturn("decrypted-token");

        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.defaultHeader(any(), any())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        doReturn(requestHeadersSpec).when(requestBodySpec).bodyValue(any());

        // Mock the retrieve() call to throw the exception
        when(requestHeadersSpec.retrieve()).thenThrow(new WebClientResponseException(
                HttpStatus.BAD_REQUEST.value(), "Bad Request", null, null, null));

        // Mock taskService
        when(taskService.getTaskListByIdForUser(anyLong(), any(User.class))).thenReturn(java.util.Optional.of(taskListDTO));


        // ACT & ASSERT
        assertThrows(InvalidDatabaseSchemaException.class, () -> notionService.createTaskListPage(taskListDTO.id(), testUser));
    }
}