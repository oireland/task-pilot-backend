package com.taskpilot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskpilot.config.JwtAuthenticationFilter;
import com.taskpilot.config.SecurityConfiguration;
import com.taskpilot.dto.user.ExchangeCodeDTO;
import com.taskpilot.exception.InvalidDatabaseSchemaException;
import com.taskpilot.exception.ResourceNotFoundException;
import com.taskpilot.model.User;
import com.taskpilot.service.JwtService;
import com.taskpilot.service.NotionService;
import com.taskpilot.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotionController.class)
@Import({SecurityConfiguration.class, JwtAuthenticationFilter.class, NotionControllerTest.ValidationConfig.class})
class NotionControllerTest {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_TOKEN = "Bearer test-token";
    private static final String RAW_TOKEN = "test-token";
    private static final String USER_EMAIL = "test@example.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Security collaborators
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UserDetailsService userDetailsService;
    @MockitoBean
    private AuthenticationProvider authenticationProvider;

    // Controller collaborators
    @MockitoBean
    private NotionService notionService;
    @MockitoBean
    private UserService userService;

    private User currentUser;

    @TestConfiguration
    static class ValidationConfig {
        @Bean
        LocalValidatorFactoryBean validator() {
            return new LocalValidatorFactoryBean();
        }
    }

    @BeforeEach
    void setupSecurity() {
        when(jwtService.extractUsername(RAW_TOKEN)).thenReturn(USER_EMAIL);

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(USER_EMAIL)
                .password("N/A")
                .authorities("ROLE_USER")
                .build();
        when(userDetailsService.loadUserByUsername(USER_EMAIL)).thenReturn(userDetails);
        when(jwtService.isTokenValid(eq(RAW_TOKEN), any(UserDetails.class))).thenReturn(true);

        currentUser = Mockito.mock(User.class);
        when(currentUser.getEmail()).thenReturn(USER_EMAIL);
        when(userService.findUserByEmail(USER_EMAIL)).thenReturn(Optional.of(currentUser));
    }

    // POST /api/v1/notion/exchange-code
    @Test
    @DisplayName("POST /api/v1/notion/exchange-code returns 200 when code is valid")
    void exchangeCode_returnsOk_whenValid() throws Exception {
        ExchangeCodeDTO req = new ExchangeCodeDTO("auth-code");

        mockMvc.perform(post("/api/v1/notion/exchange-code")
                        .header(AUTH_HEADER, BEARER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Notion account connected successfully."));

        verify(notionService).exchangeCodeAndSaveToken(eq("auth-code"), eq(currentUser));
    }

    @Test
    @DisplayName("POST /api/v1/notion/exchange-code returns 400 for invalid payload")
    void exchangeCode_returnsBadRequest_whenInvalidPayload() throws Exception {
        // Empty code to trigger @Valid failure
        String invalidJson = "{\"code\":\"\"}";

        mockMvc.perform(post("/api/v1/notion/exchange-code")
                        .header(AUTH_HEADER, BEARER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/notion/exchange-code returns 500 when service throws")
    void exchangeCode_returnsServerError_whenServiceThrows() throws Exception {
        doThrow(new RuntimeException("exchange failed"))
                .when(notionService).exchangeCodeAndSaveToken(anyString(), eq(currentUser));

        ExchangeCodeDTO req = new ExchangeCodeDTO("auth-code");

        mockMvc.perform(post("/api/v1/notion/exchange-code")
                        .header(AUTH_HEADER, BEARER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Failed to connect Notion account."));
    }

    // GET /api/v1/notion/databases
    @Test
    @DisplayName("GET /api/v1/notion/databases returns 200 with list when connected")
    void databases_returnsOk_whenConnected() throws Exception {
        when(currentUser.getNotionAccessToken()).thenReturn("notion-token");
        // Return empty list to avoid depending on DTO structure
        when(notionService.getAvailableDatabases("notion-token")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/notion/databases")
                        .header(AUTH_HEADER, BEARER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/v1/notion/databases returns 400 when not connected")
    void databases_returnsBadRequest_whenNotConnected() throws Exception {
        when(currentUser.getNotionAccessToken()).thenReturn(null);

        mockMvc.perform(get("/api/v1/notion/databases")
                        .header(AUTH_HEADER, BEARER_TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Notion account not connected."));
    }

    @Test
    @DisplayName("GET /api/v1/notion/databases returns 500 when service throws")
    void databases_returnsServerError_whenServiceThrows() throws Exception {
        when(currentUser.getNotionAccessToken()).thenReturn("tok");
        when(notionService.getAvailableDatabases("tok"))
                .thenThrow(new RuntimeException("notion down"));

        mockMvc.perform(get("/api/v1/notion/databases")
                        .header(AUTH_HEADER, BEARER_TOKEN))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Failed to retrieve databases."));
    }

    // POST /api/v1/notion/taskList/{id}
    @Test
    @DisplayName("POST /api/v1/notion/taskList/{id} returns 200 when task list is exported successfully")
    void createTaskListPage_returnsOk_whenSuccessful() throws Exception {
        when(currentUser.getNotionAccessToken()).thenReturn("notion-token");
        when(currentUser.getNotionTargetDatabaseId()).thenReturn("db-123");

        mockMvc.perform(post("/api/v1/notion/taskList/1")
                        .header(AUTH_HEADER, BEARER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Task list exported to Notion successfully."));

        verify(notionService).createTaskListPage(eq(1L), eq(currentUser));
    }

    @Test
    @DisplayName("POST /api/v1/notion/taskList/{id} returns 400 when Notion account not connected")
    void createTaskListPage_returnsBadRequest_whenNotionNotConnected() throws Exception {
        when(currentUser.getNotionAccessToken()).thenReturn(null);

        mockMvc.perform(post("/api/v1/notion/taskList/1")
                        .header(AUTH_HEADER, BEARER_TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Notion account not connected."));

        verify(notionService, never()).createTaskListPage(anyLong(), any(User.class));
    }

    @Test
    @DisplayName("POST /api/v1/notion/taskList/{id} returns 400 when no database selected")
    void createTaskListPage_returnsBadRequest_whenNoDatabaseSelected() throws Exception {
        when(currentUser.getNotionAccessToken()).thenReturn("notion-token");
        when(currentUser.getNotionTargetDatabaseId()).thenReturn(null);

        mockMvc.perform(post("/api/v1/notion/taskList/1")
                        .header(AUTH_HEADER, BEARER_TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("No Notion database selected."));

        verify(notionService, never()).createTaskListPage(anyLong(), any(User.class));
    }

    @Test
    @DisplayName("POST /api/v1/notion/taskList/{id} returns 404 when task list not found")
    void createTaskListPage_returnsNotFound_whenTaskListNotFound() throws Exception {
        when(currentUser.getNotionAccessToken()).thenReturn("notion-token");
        when(currentUser.getNotionTargetDatabaseId()).thenReturn("db-123");

        doThrow(new ResourceNotFoundException("Task list not found with id: 1"))
                .when(notionService).createTaskListPage(eq(1L), eq(currentUser));

        mockMvc.perform(post("/api/v1/notion/taskList/1")
                        .header(AUTH_HEADER, BEARER_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Task list not found with id: 1"));
    }

    @Test
    @DisplayName("POST /api/v1/notion/taskList/{id} returns 400 when database schema is invalid")
    void createTaskListPage_returnsBadRequest_whenInvalidDatabaseSchema() throws Exception {
        when(currentUser.getNotionAccessToken()).thenReturn("notion-token");
        when(currentUser.getNotionTargetDatabaseId()).thenReturn("db-123");

        doThrow(new InvalidDatabaseSchemaException("The selected Notion database has an invalid schema."))
                .when(notionService).createTaskListPage(eq(1L), eq(currentUser));

        mockMvc.perform(post("/api/v1/notion/taskList/1")
                        .header(AUTH_HEADER, BEARER_TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("The selected Notion database has an invalid schema."));
    }

    @Test
    @DisplayName("POST /api/v1/notion/taskList/{id} returns 500 when service throws unexpected error")
    void createTaskListPage_returnsServerError_whenServiceThrows() throws Exception {
        when(currentUser.getNotionAccessToken()).thenReturn("notion-token");
        when(currentUser.getNotionTargetDatabaseId()).thenReturn("db-123");

        doThrow(new RuntimeException("Unexpected error"))
                .when(notionService).createTaskListPage(eq(1L), eq(currentUser));

        mockMvc.perform(post("/api/v1/notion/taskList/1")
                        .header(AUTH_HEADER, BEARER_TOKEN))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value(startsWith("Failed to export task list to Notion:")));
    }
}