package com.oireland.controller;

import com.oireland.exception.InvalidLLMResponseException;
import com.oireland.model.TaskListDTO;
import com.oireland.service.DocumentParsingService;
import com.oireland.service.NotionPageService;
import com.oireland.service.TaskRouterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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

    @GetMapping
    public ResponseEntity<String> getWelcomeMessage() {
        return ResponseEntity.ok("Welcome to the Task Extraction API! Use POST /api/v1/tasks/extract to upload a document.");
    }

    @PostMapping(value = "/extract", consumes = "multipart/form-data")
    public ResponseEntity<?> extractTasksFromFile(@RequestParam("file") MultipartFile file) throws IOException, InvalidLLMResponseException {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File cannot be empty."));
        }

        // Step 1: Parse the document to get plaintext
        logger.info("Step 1: Parsing document '{}' with content type '{}'.", file.getOriginalFilename(), file.getContentType());
        String documentText = parsingService.parseDocument(file);

        logger.info("Parsed document text: {}", documentText.substring(0, Math.min(documentText.length(), 100)) + "...");

//         Step 2: Extract tasks using the router
        logger.info("Step 2: Starting task extraction.");
        TaskListDTO extractedTasks = taskRouterService.processDocument(documentText);

        if (extractedTasks == null || extractedTasks.tasks().isEmpty()) {
            logger.info("Extraction complete. No tasks found to create in Notion.");
            return ResponseEntity.ok(Map.of("message", "No tasks found to create in Notion."));
        }

        logger.info("Extraction complete. Found {} tasks to create in Notion.", extractedTasks.tasks().size());

        extractedTasks.tasks().forEach(task -> logger.debug("Extracted task: '{}', Status: '{}', Description: '{}'", task.taskName(), task.status(), task.description()));


//        saveTasksToFile(extractedTasks);

        // Step 3: Create pages in Notion using the dedicated service
        logger.info("Step 3: Passing {} extracted tasks to Notion page service.", extractedTasks.tasks().size());
        notionPageService.createPagesFromTasks(extractedTasks.tasks());

        logger.info("Document processing complete.");

        // Return a 202 Accepted status to indicate the request has been accepted for processing.
        return ResponseEntity.accepted().body(Map.of("message", "File received and processing started."));
    }
}

