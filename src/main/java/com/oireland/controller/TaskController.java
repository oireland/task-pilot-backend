package com.oireland.controller;

import com.oireland.dto.TaskListDTO;
import com.oireland.exception.InvalidHuggingFaceResponseException;
import com.oireland.service.DocumentParsingService;
import com.oireland.service.NotionPageService;
import com.oireland.service.TaskRouterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {
    private static final Logger logger = LoggerFactory.getLogger(TaskController.class);
    private final DocumentParsingService parsingService;
    private final TaskRouterService taskRouterService;
    private final NotionPageService notionPageService;

    public TaskController(DocumentParsingService parsingService, TaskRouterService taskRouterService, NotionPageService notionPageService) {
        this.parsingService = parsingService;
        this.taskRouterService = taskRouterService;
        this.notionPageService = notionPageService;
    }

    @PostMapping(value = "extract", consumes = "multipart/form-data")
    public ResponseEntity<?> extractTasksFromFile(@RequestParam("file") MultipartFile file) throws IOException, InvalidHuggingFaceResponseException {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File cannot be empty."));
        }

        // Step 1: Parse the document to get plaintext
        logger.info("Step 1: Parsing document '{}' with content type '{}'.", file.getOriginalFilename(), file.getContentType());
        String documentText = parsingService.parseDocument(file);

        // Step 2: Extract tasks using the router
        logger.info("Step 2: Starting task extraction.");
        TaskListDTO extractedTasks = taskRouterService.processDocument(documentText);

        if (extractedTasks == null || extractedTasks.tasks().isEmpty()) {
            logger.info("Extraction complete. No tasks found to create in Notion.");
            return ResponseEntity.ok(Map.of("message", "No tasks found to create in Notion."));
        }

        // Step 3: Create pages in Notion using the dedicated service
        logger.info("Step 3: Passing {} extracted tasks to Notion page service.", extractedTasks.tasks().size());
        notionPageService.createPagesFromTasks(extractedTasks.tasks());

        logger.info("Document processing complete.");

        // Return a 202 Accepted status to indicate the request has been accepted for processing.
        // Any exceptions thrown by the service will now be handled by the GlobalExceptionHandler.
        return ResponseEntity.accepted().body(Map.of("message", "File received and processing started."));
    }
}
