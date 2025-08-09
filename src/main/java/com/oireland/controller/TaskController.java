package com.oireland.controller;

import com.oireland.dto.ExtractedDocDataDTO;
import com.oireland.exception.InvalidLLMResponseException;
import com.oireland.service.DocumentParsingService;
import com.oireland.service.NotionPageService;
import com.oireland.service.TaskRouterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
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

    @PostMapping(value = "/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> parseDocument(@RequestParam("file") MultipartFile file, @RequestParam(value = "equations", defaultValue = "false") boolean hasEquations) throws IOException, InvalidLLMResponseException {
        logger.debug("Received request to /parse endpoint with file: {}", file.getOriginalFilename());

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File cannot be empty."));
        }

        // Step 1: Parse the document to get plaintext
        logger.info("Step 1: Parsing document '{}' with content type '{}'.", file.getOriginalFilename(), file.getContentType());
        String documentText = parsingService.parseDocument(file, hasEquations);

        logger.info("Parsed document text: {}", documentText.substring(0, Math.min(documentText.length(), 100)) + "...");
        if (documentText.isEmpty()) {
            logger.warn("Parsed document text is empty.");
            return ResponseEntity.badRequest().body(Map.of("error", "Parsed document text is empty. Please check the file format and content."));
        }

        return ResponseEntity.ok(Map.of("documentText", documentText));
    }

    @PostMapping(value = "/extract", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<?> extractTasksFromFile(@RequestBody String documentText) throws InvalidLLMResponseException {

//      Step 1: Extract tasks using the TaskRouterService
        logger.info("Starting task extraction.");
        ExtractedDocDataDTO docData = taskRouterService.processDocument(documentText);

        if (docData == null || docData.tasks().isEmpty()) {
            logger.info("Extraction complete. No tasks found to create in Notion.");
            return ResponseEntity.ok(Map.of("message", "No tasks found to create in Notion."));
        }

        logger.info("Extraction complete. Found {} tasks to create in Notion.", docData.tasks().size());

        // Step 3: Create pages in Notion using the dedicated service
        logger.info("Step 3: Passing {} extracted tasks to Notion page service.", docData.tasks().size());
        notionPageService.createTasksPage(docData);

        logger.info("Document processing complete.");

        // Return a 202 Accepted status to indicate the request has been accepted for processing.
        return ResponseEntity.accepted().body(Map.of("message", "File received and processing started."));
    }
}

