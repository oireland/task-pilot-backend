package com.oireland.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oireland.dto.ExtractedDocDataDTO;
import com.oireland.model.User;
import com.oireland.repository.UserRepository;
import com.oireland.service.DocumentParsingService;
import com.oireland.service.NotionPageService;
import com.oireland.service.TaskRouterService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // Allows non-static @BeforeAll and @AfterAll
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    // Mock the services to isolate the controller logic
    @MockitoBean
    private DocumentParsingService parsingService;
    @MockitoBean
    private TaskRouterService taskRouterService;
    @MockitoBean
    private NotionPageService notionPageService;

    private User testUser;

    /**
     * Sets up a test user in the database before any tests run.
     * This user is enabled and ready for authentication.
     */
    @BeforeAll
    void setupTestUser() {
        // Clean up any previous test user, just in case
        userRepository.findByEmail("test.user@example.com").ifPresent(userRepository::delete);

        testUser = new User("testuser", "test.user@example.com", passwordEncoder.encode("password"));
        testUser.setEnabled(true); // Must be enabled to log in
        userRepository.save(testUser);
    }

    /**
     * Cleans up the test user from the database after all tests have completed.
     */
    @AfterAll
    void cleanupTestUser() {
        userRepository.delete(testUser);
    }

    /**
     * Helper method to perform login and retrieve the authentication cookie.
     *
     * @return The HttpOnly auth_token cookie.
     */
    private Cookie getAuthCookie() throws Exception {
        String loginRequestJson = "{\"email\":\"test.user@example.com\", \"password\":\"password\"}";

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequestJson))
                .andExpect(status().isOk())
                .andReturn();

        Cookie authCookie = result.getResponse().getCookie("auth_token");
        if (authCookie == null) {
            throw new IllegalStateException("Authentication failed, cookie not received.");
        }
        return authCookie;
    }

    @Test
    void parseDocument_shouldSucceed_whenAuthenticated() throws Exception {
        // ARRANGE
        Cookie authCookie = getAuthCookie();
        var mockFile = new MockMultipartFile("file", "test.txt", "text/plain", "Some file content".getBytes());
        when(parsingService.parseDocument(any(MockMultipartFile.class))).thenReturn("Parsed document text");

        // ACT & ASSERT
        mockMvc.perform(multipart("/api/v1/tasks/parse").file(mockFile)
                        .cookie(authCookie)) // Attach the auth cookie
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentText").value("Parsed document text"));
    }

    @Test
    void parseDocument_shouldFail_whenNotAuthenticated() throws Exception {
        // ARRANGE
        var mockFile = new MockMultipartFile("file", "test.txt", "text/plain", "Some file content".getBytes());

        // ACT & ASSERT
        mockMvc.perform(multipart("/api/v1/tasks/parse").file(mockFile)) // No cookie
                .andExpect(status().isForbidden()); // Expect 403 Forbidden
    }

    @Test
    void extractTasksFromFile_shouldReturnAccepted_whenAuthenticatedAndFileIsValid() throws Exception {
        // ARRANGE
        Cookie authCookie = getAuthCookie();
        String documentText = "Some document text to process.";
        ExtractedDocDataDTO mockData = new ExtractedDocDataDTO("Test Title", "Not started", "Test Desc", List.of("Task 1"));

        when(taskRouterService.processDocument(anyString())).thenReturn(mockData);
        doNothing().when(notionPageService).createTasksPage(any(ExtractedDocDataDTO.class));

        // ACT & ASSERT
        mockMvc.perform(post("/api/v1/tasks/extract")
                        .cookie(authCookie)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(documentText))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value("File received and processing started."));
    }

    @Test
    void extractTasksFromFile_shouldFail_whenNotAuthenticated() throws Exception {
        // ARRANGE
        String documentText = "Some document text to process.";

        // ACT & ASSERT
        mockMvc.perform(post("/api/v1/tasks/extract")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(documentText))
                .andExpect(status().isForbidden());
    }
}
