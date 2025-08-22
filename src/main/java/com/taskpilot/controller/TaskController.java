package com.taskpilot.controller;

import com.taskpilot.dto.task.ExtractedDocDataDTO;
import com.taskpilot.exception.InvalidLLMResponseException;
import com.taskpilot.model.Task;
import com.taskpilot.model.User;
import com.taskpilot.repository.TaskRepository;
import com.taskpilot.service.DocumentParsingService;
import com.taskpilot.service.TaskRouterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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
    private final TaskRepository taskRepository;

    public TaskController(DocumentParsingService parsingService, TaskRouterService taskRouterService, TaskRepository taskRepository) {
        this.parsingService = parsingService;
        this.taskRouterService = taskRouterService;
        this.taskRepository = taskRepository;
    }

    /**
     * A single endpoint to handle file parsing, task extraction, and saving.
     * This will count as one request against the user's quota.
     */
    @PostMapping(value = "/process", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> processDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "equations", defaultValue = "false") boolean hasEquations,
            Authentication authentication
    ) throws IOException, InvalidLLMResponseException {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File cannot be empty."));
        }

        // Get the current user from the security context
        User currentUser = (User) authentication.getPrincipal();

        // Step 1: Parse the document
        logger.info("Parsing document '{}' for user '{}'", file.getOriginalFilename(), currentUser.getEmail());
        String documentText = parsingService.parseDocument(file, hasEquations);

        if (documentText.isEmpty()) {
            logger.warn("Parsed document text is empty for user '{}'.", currentUser.getEmail());
            return ResponseEntity.badRequest().body(Map.of("error", "Parsed document text is empty."));
        }

        // Step 2: Extract tasks from the text
        logger.info("Starting task extraction from document text for user '{}'.", currentUser.getEmail());
        ExtractedDocDataDTO docData = taskRouterService.processDocument(documentText);

        if (docData == null || docData.tasks() == null || docData.tasks().isEmpty()) {
            logger.info("Extraction complete. No tasks found for user '{}'.", currentUser.getEmail());
            ExtractedDocDataDTO emptyDto = new ExtractedDocDataDTO("No Title Found", "Not Started", "No tasks were found in the document.", List.of());
            return ResponseEntity.ok(emptyDto);
        }

        // Step 3: Save the extracted tasks to the database
        logger.info("Saving {} extracted tasks for user '{}'", docData.tasks().size(), currentUser.getEmail());
        Task newTask = new Task();
        newTask.setTitle(docData.title());
        newTask.setDescription(docData.description());
        newTask.setItems(docData.tasks()); // Assumes Task model has setTaskItems(List<String>)
        newTask.setUser(currentUser);

        taskRepository.save(newTask);
        logger.info("Successfully saved new task list with id {} for user '{}'", newTask.getId(), currentUser.getEmail());

        logger.info("Extraction and saving complete. Found {} tasks.", docData.tasks().size());
        return ResponseEntity.ok(docData);
    }
}