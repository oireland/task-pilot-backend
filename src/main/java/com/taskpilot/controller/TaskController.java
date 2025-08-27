package com.taskpilot.controller;

import com.taskpilot.aspect.CheckRateLimit;
import com.taskpilot.dto.task.CreateTaskDTO;
import com.taskpilot.dto.task.ExtractedTaskListDTO;
import com.taskpilot.dto.task.TaskDTO;
import com.taskpilot.dto.task.UpdateTaskDTO;
import com.taskpilot.exception.InvalidLLMResponseException;
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

    @GetMapping("/{taskId}")
    public ResponseEntity<TaskDTO> getTaskById(@PathVariable Long taskId, Authentication authentication) {
        User currentUser = findUserByAuthentication(authentication);
        logger.info("User '{}' attempting to retrieve task with id {}", currentUser.getEmail(), taskId);

        return taskService.getTaskByIdForUser(taskId, currentUser)
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    logger.warn("Failed to retrieve task with id {}. Task not found or user '{}' is not the owner.", taskId, currentUser.getEmail());
                    return ResponseEntity.notFound().build();
                });
    }

    @PostMapping
    public ResponseEntity<TaskDTO> createTask(
            @Valid @RequestBody CreateTaskDTO createTaskDTO,
            Authentication authentication) {
        User currentUser = findUserByAuthentication(authentication);
        logger.info("User '{}' attempting to create a new task with title '{}'", currentUser.getEmail(), createTaskDTO.title());

        TaskDTO createdTask = taskService.createTask(createTaskDTO, currentUser);

        logger.info("Successfully created new task with id {} for user '{}'", createdTask.id(), currentUser.getEmail());

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdTask.id())
                .toUri();

        return ResponseEntity.created(location).body(createdTask);
    }

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
        TaskDTO res = taskService.createTask(docData, currentUser);
        logger.info("Successfully saved new task list with id {} for user '{}'", res.id(), currentUser.getEmail());

        return ResponseEntity.ok(res);
    }

    @PutMapping("/{taskId}")
    public ResponseEntity<TaskDTO> updateTask(
            @PathVariable Long taskId,
            @RequestBody UpdateTaskDTO updateTaskDTO,
            Authentication authentication) {

        User currentUser = findUserByAuthentication(authentication);
        logger.info("User '{}' attempting to update task with id {}", currentUser.getEmail(), taskId);

        Optional<TaskDTO> updatedTaskOptional = taskService.updateTask(taskId, updateTaskDTO, currentUser);

        return updatedTaskOptional
                .map(dto -> {
                    logger.info("Successfully updated task with id {}", taskId);
                    return ResponseEntity.ok(dto);
                })
                .orElseGet(() -> {
                    logger.warn("Failed to update task with id {}. Task not found or user '{}' is not the owner.", taskId, currentUser.getEmail());
                    return ResponseEntity.notFound().build();
                });
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long taskId, Authentication authentication) {
        User currentUser = findUserByAuthentication(authentication);
        logger.info("User '{}' attempting to delete task with id {}", currentUser.getEmail(), taskId);

        boolean deleted = taskService.deleteTask(taskId, currentUser);

        if (deleted) {
            logger.info("Successfully deleted task with id {} for user '{}'", taskId, currentUser.getEmail());
            return ResponseEntity.noContent().build();
        } else {
            logger.warn("Failed to delete task with id {}. Task not found or user '{}' is not the owner.", taskId, currentUser.getEmail());
            return ResponseEntity.notFound().build();
        }
    }

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

    private User findUserByAuthentication(Authentication authentication) {
        String userEmail = authentication.getName();
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Authenticated user '" + userEmail + "' not found in the database."));
    }
}