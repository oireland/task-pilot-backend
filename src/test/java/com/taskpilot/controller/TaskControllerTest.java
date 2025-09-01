package com.taskpilot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskpilot.config.JwtAuthenticationFilter;
import com.taskpilot.config.SecurityConfiguration;
import com.taskpilot.dto.task.*;
import com.taskpilot.model.User;
import com.taskpilot.repository.UserRepository;
import com.taskpilot.service.DocumentParsingService;
import com.taskpilot.service.JwtService;
import com.taskpilot.service.TaskRouterService;
import com.taskpilot.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
@Import({SecurityConfiguration.class, JwtAuthenticationFilter.class, TaskControllerTest.ValidationConfig.class})
class TaskControllerTest {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_TOKEN = "Bearer test-token";
    private static final String RAW_TOKEN = "test-token";
    private static final String USER_EMAIL = "test@example.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Security collaborators for the filter chain
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UserDetailsService userDetailsService;
    @MockitoBean
    private AuthenticationProvider authenticationProvider;

    // Controller collaborators
    @MockitoBean
    private DocumentParsingService parsingService;
    @MockitoBean
    private TaskRouterService taskRouterService;
    @MockitoBean
    private TaskService taskService;
    @MockitoBean
    private UserRepository userRepository;

    private User currentUser;

    @BeforeEach
    void setupSecurityStubs() {
        when(jwtService.extractUsername(RAW_TOKEN)).thenReturn(USER_EMAIL);

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(USER_EMAIL)
                .password("N/A")
                .authorities("ROLE_USER")
                .build();
        when(userDetailsService.loadUserByUsername(USER_EMAIL)).thenReturn(userDetails);
        when(jwtService.isTokenValid(eq(RAW_TOKEN), any(UserDetails.class))).thenReturn(true);

        currentUser = org.mockito.Mockito.mock(User.class);
        when(currentUser.getEmail()).thenReturn(USER_EMAIL);
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(currentUser));
    }

    @TestConfiguration
    static class ValidationConfig {
        @Bean
        LocalValidatorFactoryBean localValidatorFactoryBean() {
            return new LocalValidatorFactoryBean();
        }
    }

    // GET /api/v1/tasks
    @Test
    @DisplayName("GET /api/v1/tasks returns 200 with a page of tasks")
    void getUserTasks_returnsOk_withPage() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        TaskListDTO t1 = new TaskListDTO(1L, "T1", "D1", List.of(new TodoDTO(11L, "i1", false, null)), now, now);
        TaskListDTO t2 = new TaskListDTO(2L, "T2", "D2", List.of(new TodoDTO(22L, "i2", false, null)), now, now);

        when(taskService.getTasksForUser(eq(currentUser), eq("foo"), any()))
                .thenReturn(new PageImpl<>(List.of(t1, t2), PageRequest.of(0, 20), 2));

        mockMvc.perform(get("/api/v1/tasks")
                        .header(AUTH_HEADER, BEARER_TOKEN)
                        .param("search", "foo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("T1"))
                .andExpect(jsonPath("$.content[1].title").value("T2"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    // GET /api/v1/tasks/{taskId}
    @Test
    @DisplayName("GET /api/v1/tasks/{id} returns 200 when found")
    void getTaskById_returnsOk_whenFound() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        TaskListDTO dto = new TaskListDTO(10L, "Title", "Desc", List.of(new TodoDTO(1L, "a", false, null)), now, now);

        when(taskService.getTaskListByIdForUser(10L, currentUser)).thenReturn(Optional.of(dto));

        mockMvc.perform(get("/api/v1/tasks/{taskId}", 10L)
                        .header(AUTH_HEADER, BEARER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.title").value("Title"));
    }

    @Test
    @DisplayName("GET /api/v1/tasks/{id} returns 404 when missing")
    void getTaskById_returnsNotFound_whenMissing() throws Exception {
        when(taskService.getTaskListByIdForUser(99L, currentUser)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/tasks/{taskId}", 99L)
                        .header(AUTH_HEADER, BEARER_TOKEN))
                .andExpect(status().isNotFound());
    }

    // POST /api/v1/tasks
    @Test
    @DisplayName("POST /api/v1/tasks returns 201 Created with Location")
    void createTask_returnsCreated() throws Exception {
        CreateTaskDTO req = new CreateTaskDTO(
                "New",
                "Desc",
                List.of(
                        new TodoDTO(null, "i1", false, null),
                        new TodoDTO(null, "i2", false, null)
                )
        );
        LocalDateTime now = LocalDateTime.now();
        TaskListDTO created = new TaskListDTO(
                123L,
                "New",
                "Desc",
                List.of(
                        new TodoDTO(1L, "i1", false, null),
                        new TodoDTO(2L, "i2", false, null)
                ),
                now,
                now
        );

        when(taskService.createTaskList(eq(req), eq(currentUser))).thenReturn(created);

        mockMvc.perform(post("/api/v1/tasks")
                        .header(AUTH_HEADER, BEARER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/tasks/123"))
                .andExpect(jsonPath("$.id").value(123L))
                .andExpect(jsonPath("$.title").value("New"));
    }

    // POST /api/v1/tasks/process
    @Test
    @DisplayName("POST /api/v1/tasks/process returns 200 with extracted task when tasks are found")
    void processDocument_returnsOk_withExtracted() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", MediaType.APPLICATION_PDF_VALUE, "pdf-bytes".getBytes());
        when(parsingService.parseDocument(any(), eq(false))).thenReturn("parsed-text");

        ExtractedTaskListDTO docData = new ExtractedTaskListDTO("Doc Title", "Doc Desc", List.of("x"));
        when(taskRouterService.processDocument("parsed-text")).thenReturn(docData);

        LocalDateTime now = LocalDateTime.now();
        TaskListDTO saved = new TaskListDTO(
                55L,
                "Doc Title",
                "Doc Desc",
                List.of(new TodoDTO(101L, "x", false, null)),
                now,
                now
        );
        when(taskService.createTaskList(eq(docData), eq(currentUser))).thenReturn(saved);

        mockMvc.perform(multipart("/api/v1/tasks/process")
                        .file(file)
                        .header(AUTH_HEADER, BEARER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(55L))
                .andExpect(jsonPath("$.title").value("Doc Title"));
    }

    // PUT /api/v1/tasks/{taskId}
    @Test
    @DisplayName("PUT /api/v1/tasks/{id} returns 200 when updated")
    void updateTask_returnsOk_whenUpdated() throws Exception {
        UpdateTaskDTO req = new UpdateTaskDTO(
                "Updated Title",
                "Updated Desc",
                List.of(new TodoDTO(null, "x", true, null))
        );
        LocalDateTime now = LocalDateTime.now();
        TaskListDTO updated = new TaskListDTO(
                10L,
                "Updated Title",
                "Updated Desc",
                List.of(new TodoDTO(1L, "x", true, null)),
                now,
                now
        );

        when(taskService.updateTask(10L, req, currentUser)).thenReturn(Optional.of(updated));

        mockMvc.perform(put("/api/v1/tasks/{taskId}", 10L)
                        .header(AUTH_HEADER, BEARER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.description").value("Updated Desc"));
    }

    @Test
    @DisplayName("PUT /api/v1/tasks/{id} returns 404 when task not found")
    void updateTask_returnsNotFound_whenMissing() throws Exception {
        UpdateTaskDTO req = new UpdateTaskDTO("Title", "Desc", List.of(new TodoDTO(null, "x", false, null)));
        when(taskService.updateTask(999L, req, currentUser)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/v1/tasks/{taskId}", 999L)
                        .header(AUTH_HEADER, BEARER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    // DELETE /api/v1/tasks/{taskId}
    @Test
    @DisplayName("DELETE /api/v1/tasks/{id} returns 204 when deleted")
    void deleteTask_returnsNoContent_whenDeleted() throws Exception {
        when(taskService.deleteTask(10L, currentUser)).thenReturn(true);

        mockMvc.perform(delete("/api/v1/tasks/{taskId}", 10L)
                        .header(AUTH_HEADER, BEARER_TOKEN))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/v1/tasks/{id} returns 404 when not found")
    void deleteTask_returnsNotFound_whenMissing() throws Exception {
        when(taskService.deleteTask(11L, currentUser)).thenReturn(false);

        mockMvc.perform(delete("/api/v1/tasks/{taskId}", 11L)
                        .header(AUTH_HEADER, BEARER_TOKEN))
                .andExpect(status().isNotFound());
    }
}