package com.oireland.controller;

import com.oireland.exception.InvalidHuggingFaceResponseException;
import com.oireland.service.DocumentProcessingService;
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
    private final DocumentProcessingService documentProcessingService;

    public TaskController(DocumentProcessingService documentProcessingService) {
        this.documentProcessingService = documentProcessingService;
    }

    @PostMapping(value = "/extract", consumes = "multipart/form-data")
    public ResponseEntity<?> extractTasksFromFile(@RequestParam("file") MultipartFile file) throws IOException, InvalidHuggingFaceResponseException {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File cannot be empty."));
        }

        logger.info("Received file upload: {}. Kicking off processing.", file.getOriginalFilename());
        documentProcessingService.processDocumentAndCreateTasks(file);

        // Return a 202 Accepted status to indicate the request has been accepted for processing.
        // Any exceptions thrown by the service will now be handled by the GlobalExceptionHandler.
        return ResponseEntity.accepted().body(Map.of("message", "File received and processing started."));
    }
}
