package com.taskpilot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskpilot.dto.task.ExtractedDocDataDTO;
import com.taskpilot.model.User;
import com.taskpilot.repository.UserRepository;
import com.taskpilot.service.DocumentParsingService;
import com.taskpilot.service.TaskRouterService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DocumentParsingService parsingService;
    @MockitoBean
    private TaskRouterService taskRouterService;

    private User testUser;

    @BeforeAll
    void setupTestUser() {
        userRepository.findByEmail("test.user@example.com").ifPresent(userRepository::delete);
        testUser = new User("test.user@example.com", passwordEncoder.encode("password"));
        testUser.setEnabled(true);
        userRepository.save(testUser);
    }

    @AfterAll
    void cleanupTestUser() {
        userRepository.delete(testUser);
    }

    private Cookie getAuthCookie() throws Exception {
        String loginRequestJson = "{\"email\":\"test.user@example.com\", \"password\":\"password\"}";
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequestJson))
                .andExpect(status().isOk())
                .andReturn();

        Cookie authCookie = result.getResponse().getCookie("task_pilot_auth_token");
        if (authCookie == null) {
            throw new IllegalStateException("Authentication failed, cookie not received.");
        }
        return authCookie;
    }

    @Test
    void parseDocument_shouldSucceed_whenFileIsValid() throws Exception {
        // Arrange
        Cookie authCookie = getAuthCookie();
        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "Some file content".getBytes()
        );
        when(parsingService.parseDocument(any(MockMultipartFile.class), anyBoolean()))
                .thenReturn("Parsed document text");

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/tasks/parse")
                        .file(mockFile)
                        .cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentText").value("Parsed document text"));
    }

    @Test
    void parseDocument_shouldFail_whenFileIsEmpty() throws Exception {
        // Arrange
        Cookie authCookie = getAuthCookie();
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.txt",
                "text/plain",
                new byte[0]
        );

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/tasks/parse")
                        .file(emptyFile)
                        .cookie(authCookie))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("File cannot be empty."));
    }

    @Test
    void parseDocument_shouldFail_whenNotAuthenticated() throws Exception {
        // Arrange
        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "Some content".getBytes()
        );

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/tasks/parse")
                        .file(mockFile))
                .andExpect(status().isForbidden());
    }

    @Test
    void extractTasks_shouldSucceed_whenDocumentTextIsValid() throws Exception {
        // Arrange
        Cookie authCookie = getAuthCookie();
        String documentText = "Some document text to process.";
        ExtractedDocDataDTO mockData = new ExtractedDocDataDTO(
                "Test Title",
                "Not started",
                "Test Desc",
                List.of("Task 1")
        );
        when(taskRouterService.processDocument(anyString())).thenReturn(mockData);

        // Act & Assert
        mockMvc.perform(post("/api/v1/tasks/extract")
                        .cookie(authCookie)
                        .contentType(MediaType.TEXT_PLAIN_VALUE)
                        .content(documentText))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.title").value("Test Title"))
                .andExpect(jsonPath("$.status").value("Not started"))
                .andExpect(jsonPath("$.description").value("Test Desc"))
                .andExpect(jsonPath("$.tasks[0]").value("Task 1"));
    }

    @Test
    void extractTasks_shouldReturnMessage_whenNoTasksFound() throws Exception {
        // Arrange
        Cookie authCookie = getAuthCookie();
        String documentText = "Some document text to process.";
        when(taskRouterService.processDocument(anyString())).thenReturn(null);

        // Act & Assert
        mockMvc.perform(post("/api/v1/tasks/extract")
                        .cookie(authCookie)
                        .contentType(MediaType.TEXT_PLAIN_VALUE)
                        .content(documentText))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("No tasks found to create in Notion."));
    }

    @Test
    void extractTasks_shouldFail_whenNotAuthenticated() throws Exception {
        // Arrange
        String documentText = "Some document text to process.";

        // Act & Assert
        mockMvc.perform(post("/api/v1/tasks/extract")
                        .contentType(MediaType.TEXT_PLAIN_VALUE)
                        .content(documentText))
                .andExpect(status().isForbidden());
    }
}