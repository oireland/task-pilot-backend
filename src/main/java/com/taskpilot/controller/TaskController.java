package com.taskpilot.controller;

import com.taskpilot.aspect.CheckRateLimit;
import com.taskpilot.dto.task.ExtractedDocDataDTO;
import com.taskpilot.dto.task.TaskDTO;
import com.taskpilot.exception.InvalidLLMResponseException;
import com.taskpilot.model.Task;
import com.taskpilot.model.User;
import com.taskpilot.service.DocumentParsingService;
import com.taskpilot.service.TaskRouterService;
import com.taskpilot.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private static final Logger logger = LoggerFactory.getLogger(TaskController.class);
    private final DocumentParsingService parsingService;
    private final TaskRouterService taskRouterService;
    private final TaskService taskService;

    public TaskController(DocumentParsingService parsingService, TaskRouterService taskRouterService, TaskService taskService) {
        this.parsingService = parsingService;
        this.taskRouterService = taskRouterService;
        this.taskService = taskService;
    }

    /**
     * Retrieves a paginated and searchable list of tasks for the authenticated user.
     * This endpoint is NOT rate-limited.
     */
    @GetMapping
    public ResponseEntity<Page<TaskDTO>> getUserTasks(
            Authentication authentication,
            @RequestParam(required = false) String search,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        User currentUser = (User) authentication.getPrincipal();
        // FIX: The service now returns a Page<TaskDTO> directly, so we assign it to the correct type.
        // This removes the need for manual mapping in the controller.
        Page<TaskDTO> tasksDtoPage = taskService.getTasksForUser(currentUser, search, pageable);
        return ResponseEntity.ok(tasksDtoPage);
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

        User currentUser = (User) authentication.getPrincipal();

        logger.info("Parsing document '{}' for user '{}'", file.getOriginalFilename(), currentUser.getEmail());
        String documentText = parsingService.parseDocument(file, hasEquations);

        if (documentText.isEmpty()) {
            logger.warn("Parsed document text is empty for user '{}'.", currentUser.getEmail());
            return ResponseEntity.badRequest().body(Map.of("error", "Parsed document text is empty."));
        }

        logger.info("Starting task extraction from document text for user '{}'.", currentUser.getEmail());
        ExtractedDocDataDTO docData = taskRouterService.processDocument(documentText);

        if (docData == null || docData.tasks() == null || docData.tasks().isEmpty()) {
            logger.info("Extraction complete. No tasks found for user '{}'.", currentUser.getEmail());
            ExtractedDocDataDTO emptyDto = new ExtractedDocDataDTO("No Title Found", "Not Started", "No tasks were found in the document.", List.of());
            return ResponseEntity.ok(emptyDto);
        }

        logger.info("Saving {} extracted tasks for user '{}'", docData.tasks().size(), currentUser.getEmail());
        Task savedTask = taskService.createTask(docData, currentUser);
        logger.info("Successfully saved new task list with id {} for user '{}'", savedTask.getId(), currentUser.getEmail());

        logger.info("Extraction and saving complete. Found {} tasks.", docData.tasks().size());
        return ResponseEntity.ok(docData);
    }
}