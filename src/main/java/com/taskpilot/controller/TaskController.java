package com.taskpilot.controller;

import com.taskpilot.dto.task.ExtractedDocDataDTO;
import com.taskpilot.exception.InvalidLLMResponseException;
import com.taskpilot.service.DocumentParsingService;
import com.taskpilot.service.TaskRouterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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


    public TaskController(DocumentParsingService parsingService, TaskRouterService taskRouterService) {
        this.parsingService = parsingService;
        this.taskRouterService = taskRouterService;
    }

    @PostMapping(value = "/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> parseDocument(@RequestParam("file") MultipartFile file, @RequestParam(value = "equations", defaultValue = "false") boolean hasEquations) throws IOException, InvalidLLMResponseException {
        logger.debug("Received request to /parse endpoint with file: {}", file.getOriginalFilename());

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File cannot be empty."));
        }

        // Step 1: Parse the document to get plaintext
        String documentText = parsingService.parseDocument(file, hasEquations);

        if (documentText.isEmpty()) {
            logger.warn("Parsed document text is empty.");
            return ResponseEntity.badRequest().body(Map.of("error", "Parsed document text is empty. Please check the file format and content."));
        }

        return ResponseEntity.ok(Map.of("documentText", documentText));
    }

    @PostMapping(value = "/extract", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<?> extractTasksFromFile(@RequestBody String documentText) throws InvalidLLMResponseException {
        logger.info("Starting task extraction.");
        ExtractedDocDataDTO docData = taskRouterService.processDocument(documentText);

        // If the LLM returns null, create a default DTO with an empty task list.
        if (docData == null) {
            logger.info("Extraction complete. No tasks found to create in Notion.");
            // Return a valid DTO with an empty list instead of a different object.
            ExtractedDocDataDTO emptyDto = new ExtractedDocDataDTO("No Title Found", "Not Started", "No tasks were found in the document.", List.of());
            return ResponseEntity.ok(emptyDto);
        }

        // Also handle the case where the DTO exists but the list is empty
        if (docData.tasks() == null || docData.tasks().isEmpty()) {
            logger.info("Extraction complete. Task list is empty.");
        } else {
            logger.info("Extraction complete. Found {} tasks.", docData.tasks().size());
        }

        // Always return the ExtractedDocDataDTO object.
        return ResponseEntity.ok(docData);
    }
}

