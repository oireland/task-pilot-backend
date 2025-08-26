package com.taskpilot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskpilot.config.JwtAuthenticationFilter;
import com.taskpilot.config.SecurityConfiguration;
import com.taskpilot.dto.task.CreateTaskDTO;
import com.taskpilot.dto.task.ExtractedTaskListDTO;
import com.taskpilot.dto.task.TaskDTO;
import com.taskpilot.dto.task.UpdateTaskDTO;
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
        // Mock accepted JWT -> username
        when(jwtService.extractUsername(RAW_TOKEN)).thenReturn(USER_EMAIL);

        // Mock user details for the username
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(USER_EMAIL)
                .password("N/A")
                .authorities("ROLE_USER")
                .build();
        when(userDetailsService.loadUserByUsername(USER_EMAIL)).thenReturn(userDetails);

        // Token validity accepted
        when(jwtService.isTokenValid(eq(RAW_TOKEN), any(UserDetails.class))).thenReturn(true);

        // Mock current user entity resolution
        currentUser = org.mockito.Mockito.mock(User.class);
        when(currentUser.getEmail()).thenReturn(USER_EMAIL);
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(currentUser));
    }

    // GET /api/v1/tasks
    @Test
    @DisplayName("GET /api/v1/tasks returns 200 with a page of tasks")
    void getUserTasks_returnsOk_withPage() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        TaskDTO t1 = new TaskDTO(1L, "T1", "D1", List.of("i1"), now, now);
        TaskDTO t2 = new TaskDTO(2L, "T2", "D2", List.of("i2"), now, now);

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
        TaskDTO dto = new TaskDTO(10L, "Title", "Desc", List.of("a"), now, now);

        when(taskService.getTaskByIdForUser(10L, currentUser)).thenReturn(Optional.of(dto));

        mockMvc.perform(get("/api/v1/tasks/{taskId}", 10L)
                        .header(AUTH_HEADER, BEARER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.title").value("Title"));
    }

    @Test
    @DisplayName("GET /api/v1/tasks/{id} returns 404 when missing")
    void getTaskById_returnsNotFound_whenMissing() throws Exception {
        when(taskService.getTaskByIdForUser(99L, currentUser)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/tasks/{taskId}", 99L)
                        .header(AUTH_HEADER, BEARER_TOKEN))
                .andExpect(status().isNotFound());
    }

    // POST /api/v1/tasks
    @Test
    @DisplayName("POST /api/v1/tasks returns 201 with Location and body for valid payload")
    void createTask_returnsCreated_whenValid() throws Exception {
        CreateTaskDTO req = new CreateTaskDTO("New Task", "Some desc", List.of("i1", "i2"));
        LocalDateTime now = LocalDateTime.now();
        TaskDTO created = new TaskDTO(5L, "New Task", "Some desc", List.of("i1", "i2"), now, now);

        when(taskService.createTask(eq(req), eq(currentUser))).thenReturn(created);

        mockMvc.perform(post("/api/v1/tasks")
                        .header(AUTH_HEADER, BEARER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/v1/tasks/5")))
                .andExpect(jsonPath("$.id").value(5L))
                .andExpect(jsonPath("$.title").value("New Task"));
    }

    @Test
    @DisplayName("POST /api/v1/tasks returns 400 for invalid payload")
    void createTask_returnsBadRequest_whenInvalid() throws Exception {
        // Title missing or blank to trigger \@Valid failure
        String invalidJson = "{\"title\":\"\",\"description\":\"x\",\"items\":[\"i1\"]}";

        mockMvc.perform(post("/api/v1/tasks")
                        .header(AUTH_HEADER, BEARER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    // POST /api/v1/tasks/process
    @Test
    @DisplayName("POST /api/v1/tasks/process returns 400 when file is empty")
    void processDocument_returnsBadRequest_whenEmptyFile() throws Exception {
        MockMultipartFile empty = new MockMultipartFile("file", "f.txt", "text/plain", new byte[0]);

        mockMvc.perform(multipart("/api/v1/tasks/process")
                        .file(empty)
                        .param("equations", "false")
                        .header(AUTH_HEADER, BEARER_TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("File cannot be empty."));
    }

    @Test
    @DisplayName("POST /api/v1/tasks/process returns 200 with extracted empty DTO when no tasks parsed")
    void processDocument_returnsOk_withEmptyDto_whenNoTasks() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "doc.txt", "text/plain", "content".getBytes());

        when(parsingService.parseDocument(any(), eq(false))).thenReturn("parsed text");
        when(taskRouterService.processDocument("parsed text"))
                .thenReturn(new ExtractedTaskListDTO("No Title Found", "No tasks were found in the document.", List.of()));

        mockMvc.perform(multipart("/api/v1/tasks/process")
                        .file(file)
                        .param("equations", "false")
                        .header(AUTH_HEADER, BEARER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("No Title Found"))
                .andExpect(jsonPath("$.tasks").isArray())
                .andExpect(jsonPath("$.tasks.length()").value(0));
    }

    @Test
    @DisplayName("POST /api/v1/tasks/process returns 200 with saved TaskDTO when tasks parsed")
    void processDocument_returnsOk_withSavedTask() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "doc.txt", "text/plain", "content".getBytes());

        when(parsingService.parseDocument(any(), eq(true))).thenReturn("parsed text");
        when(taskRouterService.processDocument("parsed text"))
                .thenReturn(new ExtractedTaskListDTO("Doc Title", "Desc", List.of("A", "B")));

        LocalDateTime now = LocalDateTime.now();
        // Service called with ExtractedTaskListDTO + user, returns entity -> controller maps; we stub return DTO directly by matching controller behavior
        // We can't intercept the internal conversion here, so stub with any() and provide our desired DTO
        when(taskService.createTask(any(ExtractedTaskListDTO.class), eq(currentUser)))
                .thenAnswer(inv -> {
                    // Simulate entity id presence to be mapped
                    com.taskpilot.model.Task saved = new com.taskpilot.model.Task();
                    saved.setId(42L);
                    saved.setTitle("Doc Title");
                    saved.setDescription("Desc");
                    saved.setItems(List.of("A", "B"));
                    saved.setUser(currentUser);
                    return saved;
                });

        mockMvc.perform(multipart("/api/v1/tasks/process")
                        .file(file)
                        .param("equations", "true")
                        .header(AUTH_HEADER, BEARER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42L))
                .andExpect(jsonPath("$.title").value("Doc Title"))
                .andExpect(jsonPath("$.items.length()").value(2));
    }

    // PUT /api/v1/tasks/{taskId}
    @Test
    @DisplayName("PUT /api/v1/tasks/{id} returns 200 with updated DTO when service succeeds")
    void updateTask_returnsOk_whenUpdated() throws Exception {
        UpdateTaskDTO req = new UpdateTaskDTO("New Title", "New Desc", List.of("x"));
        LocalDateTime now = LocalDateTime.now();
        TaskDTO updated = new TaskDTO(7L, "New Title", "New Desc", List.of("x"), now, now);

        when(taskService.updateTask(eq(7L), eq(req), eq(currentUser))).thenReturn(Optional.of(updated));

        mockMvc.perform(put("/api/v1/tasks/{taskId}", 7L)
                        .header(AUTH_HEADER, BEARER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7L))
                .andExpect(jsonPath("$.title").value("New Title"));
    }

    @Test
    @DisplayName("PUT /api/v1/tasks/{id} returns 404 when not found")
    void updateTask_returnsNotFound_whenMissing() throws Exception {
        UpdateTaskDTO req = new UpdateTaskDTO("New Title", "New Desc", List.of("x"));
        when(taskService.updateTask(eq(55L), eq(req), eq(currentUser))).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/v1/tasks/{taskId}", 55L)
                        .header(AUTH_HEADER, BEARER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    // DELETE /api/v1/tasks/{taskId}
    @Test
    @DisplayName("DELETE /api/v1/tasks/{id} returns 204 when deleted")
    void deleteTask_returnsNoContent_whenDeleted() throws Exception {
        when(taskService.deleteTask(11L, currentUser)).thenReturn(true);

        mockMvc.perform(delete("/api/v1/tasks/{taskId}", 11L)
                        .header(AUTH_HEADER, BEARER_TOKEN))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/v1/tasks/{id} returns 404 when not found")
    void deleteTask_returnsNotFound_whenNotDeleted() throws Exception {
        when(taskService.deleteTask(12L, currentUser)).thenReturn(false);

        mockMvc.perform(delete("/api/v1/tasks/{taskId}", 12L)
                        .header(AUTH_HEADER, BEARER_TOKEN))
                .andExpect(status().isNotFound());
    }

    // DELETE /api/v1/tasks/batch
    @Test
    @DisplayName("DELETE /api/v1/tasks/batch returns 200 with deletedCount")
    void deleteTasks_returnsOk_withCount() throws Exception {
        List<Long> ids = List.of(1L, 2L, 3L);
        when(taskService.deleteTasks(ids, currentUser)).thenReturn(2);

        mockMvc.perform(delete("/api/v1/tasks/batch")
                        .header(AUTH_HEADER, BEARER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ids)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedCount").value(2));
    }

    @Test
    @DisplayName("DELETE /api/v1/tasks/batch returns 400 for empty list")
    void deleteTasks_returnsBadRequest_whenEmpty() throws Exception {
        List<Long> ids = List.of();

        mockMvc.perform(delete("/api/v1/tasks/batch")
                        .header(AUTH_HEADER, BEARER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ids)))
                .andExpect(status().isBadRequest());
    }

    @TestConfiguration
    static class ValidationConfig {
        @Bean
        LocalValidatorFactoryBean validator() {
            return new LocalValidatorFactoryBean();
        }
    }
}