package com.taskpilot.controller;

import com.taskpilot.dto.task.ExtractedDocDataDTO;
import com.taskpilot.exception.InvalidLLMResponseException;
import com.taskpilot.service.DocumentParsingService;
import com.taskpilot.service.TaskRouterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    public TaskController(DocumentParsingService parsingService, TaskRouterService taskRouterService) {
        this.parsingService = parsingService;
        this.taskRouterService = taskRouterService;
    }

    /**
     * A single endpoint to handle file parsing and task extraction.
     * This will count as one request against the user's quota.
     */
    @PostMapping(value = "/process", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> processDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "equations", defaultValue = "false") boolean hasEquations
    ) throws IOException, InvalidLLMResponseException {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File cannot be empty."));
        }

        // Step 1: Parse the document
        logger.info("Parsing document '{}'", file.getOriginalFilename());
        String documentText = parsingService.parseDocument(file, hasEquations);

        if (documentText.isEmpty()) {
            logger.warn("Parsed document text is empty.");
            return ResponseEntity.badRequest().body(Map.of("error", "Parsed document text is empty."));
        }

        // Step 2: Extract tasks from the text
        logger.info("Starting task extraction from document text.");
        ExtractedDocDataDTO docData = taskRouterService.processDocument(documentText);

        if (docData == null) {
            logger.info("Extraction complete. No tasks found.");
            ExtractedDocDataDTO emptyDto = new ExtractedDocDataDTO("No Title Found", "Not Started", "No tasks were found in the document.", List.of());
            return ResponseEntity.ok(emptyDto);
        }

        logger.info("Extraction complete. Found {} tasks.", docData.tasks().size());
        return ResponseEntity.ok(docData);
    }
}