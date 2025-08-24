package com.taskpilot.controller;

import com.taskpilot.aspect.CheckRateLimit;
import com.taskpilot.dto.task.CreateTaskDTO;
import com.taskpilot.dto.task.ExtractedTaskListDTO;
import com.taskpilot.dto.task.TaskDTO;
import com.taskpilot.dto.task.UpdateTaskDTO;
import com.taskpilot.exception.InvalidLLMResponseException;
import com.taskpilot.model.Task;
import com.taskpilot.model.User;
import com.taskpilot.repository.UserRepository;
import com.taskpilot.service.DocumentParsingService;
import com.taskpilot.service.TaskRouterService;
import com.taskpilot.service.TaskService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private static final Logger logger = LoggerFactory.getLogger(TaskController.class);
    private final DocumentParsingService parsingService;
    private final TaskRouterService taskRouterService;
    private final TaskService taskService;
    private final UserRepository userRepository;

    public TaskController(
            DocumentParsingService parsingService,
            TaskRouterService taskRouterService,
            TaskService taskService,
            UserRepository userRepository
    ) {
        this.parsingService = parsingService;
        this.taskRouterService = taskRouterService;
        this.taskService = taskService;
        this.userRepository = userRepository;
    }

    /**
     * Retrieves a paginated and searchable list of tasks for the authenticated user.
     * Defaults to sorting by the most recently updated tasks.
     */
    @GetMapping
    public ResponseEntity<Page<TaskDTO>> getUserTasks(
            Authentication authentication,
            @RequestParam(required = false) String search,
            @PageableDefault(sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        User currentUser = findUserByAuthentication(authentication);
        Page<TaskDTO> tasksDtoPage = taskService.getTasksForUser(currentUser, search, pageable);
        return ResponseEntity.ok(tasksDtoPage);
    }

    /**
     * Retrieves a single task by its ID for the authenticated user.
     *
     * @param taskId The ID of the task to retrieve.
     * @param authentication The security context.
     * @return A response entity with the task DTO or 404 if not found.
     */
    @GetMapping("/{taskId}")
    public ResponseEntity<TaskDTO> getTaskById(@PathVariable Long taskId, Authentication authentication) {
        User currentUser = findUserByAuthentication(authentication);
        logger.info("User '{}' attempting to retrieve task with id {}", currentUser.getEmail(), taskId);

        return taskService.getTaskByIdForUser(taskId, currentUser)
                .map(ResponseEntity::ok) // If present, wrap in 200 OK
                .orElseGet(() -> {
                    logger.warn("Failed to retrieve task with id {}. Task not found or user '{}' is not the owner.", taskId, currentUser.getEmail());
                    return ResponseEntity.notFound().build(); // If empty, return 404
                });
    }

    /**
     * Creates a new task from user-provided data.
     * This endpoint is NOT rate-limited.
     *
     * @param createTaskDTO The request body containing the new task data.
     * @param authentication The security context.
     * @return A response entity with the created task and a location header.
     */
    @PostMapping
    public ResponseEntity<TaskDTO> createTask(
            @Valid @RequestBody CreateTaskDTO createTaskDTO,
            Authentication authentication) {
        User currentUser = findUserByAuthentication(authentication);
        logger.info("User '{}' attempting to create a new task with title '{}'", currentUser.getEmail(), createTaskDTO.title());

        TaskDTO createdTask = taskService.createTask(createTaskDTO, currentUser);

        logger.info("Successfully created new task with id {} for user '{}'", createdTask.id(), currentUser.getEmail());

        // Build the location URI of the newly created resource
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdTask.id())
                .toUri();

        return ResponseEntity.created(location).body(createdTask);
    }

    /**
     * A single endpoint to handle file parsing, task extraction, and saving.
     * This endpoint IS rate-limited.
     */
    @PostMapping(value = "/process", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @CheckRateLimit
    public ResponseEntity<?> processDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "equations", defaultValue = "false") boolean hasEquations,
            Authentication authentication
    ) throws IOException, InvalidLLMResponseException {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File cannot be empty."));
        }

        User currentUser = findUserByAuthentication(authentication);

        logger.info("Parsing document '{}' for user '{}'", file.getOriginalFilename(), currentUser.getEmail());
        String documentText = parsingService.parseDocument(file, hasEquations);

        if (documentText.isEmpty()) {
            logger.warn("Parsed document text is empty for user '{}'.", currentUser.getEmail());
            return ResponseEntity.badRequest().body(Map.of("error", "Parsed document text is empty."));
        }

        logger.info("Starting task extraction from document text for user '{}'.", currentUser.getEmail());
        ExtractedTaskListDTO docData = taskRouterService.processDocument(documentText);

        if (docData == null || docData.tasks() == null || docData.tasks().isEmpty()) {
            logger.info("Extraction complete. No tasks found for user '{}'.", currentUser.getEmail());
            ExtractedTaskListDTO emptyDto = new ExtractedTaskListDTO("No Title Found", "No tasks were found in the document.", List.of());
            return ResponseEntity.ok(emptyDto);
        }

        logger.info("Saving {} extracted tasks for user '{}'", docData.tasks().size(), currentUser.getEmail());
        Task savedTask = taskService.createTask(docData, currentUser);
        logger.info("Successfully saved new task list with id {} for user '{}'", savedTask.getId(), currentUser.getEmail());

        logger.info("Extraction and saving complete. Found {} tasks.", docData.tasks().size());
        return ResponseEntity.ok(docData);
    }

    /**
     * Updates an existing task for the authenticated user.
     * This endpoint is NOT rate-limited.
     *
     * @param taskId The ID of the task to update.
     * @param updateTaskDTO The request body containing the new task data.
     * @param authentication The security context.
     * @return A response entity with the updated task or 404 if not found.
     */
    @PutMapping("/{taskId}")
    public ResponseEntity<TaskDTO> updateTask(
            @PathVariable Long taskId,
            @RequestBody UpdateTaskDTO updateTaskDTO,
            Authentication authentication) {

        User currentUser = findUserByAuthentication(authentication);
        logger.info("User '{}' attempting to update task with id {}", currentUser.getEmail(), taskId);

        // Call the service to update the task
        Optional<TaskDTO> updatedTaskOptional = taskService.updateTask(taskId, updateTaskDTO, currentUser);

        // Use the Optional to build the correct response
        return updatedTaskOptional
                .map(dto -> {
                    logger.info("Successfully updated task with id {}", taskId);
                    return ResponseEntity.ok(dto); // HTTP 200 OK with updated task
                })
                .orElseGet(() -> {
                    logger.warn("Failed to update task with id {}. Task not found or user '{}' is not the owner.", taskId, currentUser.getEmail());
                    return ResponseEntity.notFound().build(); // HTTP 404 Not Found
                });
    }

    /**
     * Deletes a task by its ID for the authenticated user.
     * This endpoint is NOT rate-limited.
     */
    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long taskId, Authentication authentication) {
        User currentUser = findUserByAuthentication(authentication);
        logger.info("User '{}' attempting to delete task with id {}", currentUser.getEmail(), taskId);

        boolean deleted = taskService.deleteTask(taskId, currentUser);

        if (deleted) {
            logger.info("Successfully deleted task with id {} for user '{}'", taskId, currentUser.getEmail());
            return ResponseEntity.noContent().build(); // HTTP 204 No Content
        } else {
            logger.warn("Failed to delete task with id {}. Task not found or user '{}' is not the owner.", taskId, currentUser.getEmail());
            return ResponseEntity.notFound().build(); // HTTP 404 Not Found
        }
    }

    /**
     * Deletes multiple tasks by a list of IDs for the authenticated user.
     * This endpoint is NOT rate-limited.
     *
     * @param taskIds A JSON list of task IDs to be deleted.
     * @param authentication The security context.
     * @return A response entity with the count of deleted tasks.
     */
    @DeleteMapping("/batch")
    public ResponseEntity<Map<String, Integer>> deleteTasks(@RequestBody List<Long> taskIds, Authentication authentication) {
        User currentUser = findUserByAuthentication(authentication);
        logger.info("User '{}' attempting to batch delete {} tasks.", currentUser.getEmail(), taskIds != null ? taskIds.size() : 0);

        if (taskIds == null || taskIds.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        int deletedCount = taskService.deleteTasks(taskIds, currentUser);

        logger.info("User '{}' successfully deleted {} tasks out of {} requested.", currentUser.getEmail(), deletedCount, taskIds.size());

        return ResponseEntity.ok(Map.of("deletedCount", deletedCount));
    }

    /**
     * A private helper method to safely retrieve the User entity from the security context.
     */
    private User findUserByAuthentication(Authentication authentication) {
        String userEmail = authentication.getName();
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Authenticated user '" + userEmail + "' not found in the database."));
    }
}