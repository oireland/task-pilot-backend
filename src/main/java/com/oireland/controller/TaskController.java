package com.oireland.controller;

import com.oireland.exception.InvalidLLMResponseException;
import com.oireland.model.ExtractedDocDataDTO;
import com.oireland.prompt.PromptFactory;
import com.oireland.service.DocumentParsingService;
import com.oireland.service.NotionPageService;
import com.oireland.service.TaskRouterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private static final Logger logger = LoggerFactory.getLogger(TaskController.class);
    private final DocumentParsingService parsingService;
    private final TaskRouterService taskRouterService;
    private final NotionPageService notionPageService;
    private final ChatClient chatClient;
    private final PromptFactory promptFactory;


    public TaskController(DocumentParsingService parsingService, TaskRouterService taskRouterService, NotionPageService notionPageService, ChatClient.Builder builder, PromptFactory promptFactory) {
        this.parsingService = parsingService;
        this.taskRouterService = taskRouterService;
        this.notionPageService = notionPageService;
        this.chatClient = builder.build();
        this.promptFactory = promptFactory;
    }

    @GetMapping
    public ResponseEntity<String> getWelcomeMessage() {
        return ResponseEntity.ok("Welcome to the Task Extraction API! Use POST /api/v1/tasks/extract to upload a document.");
    }

    @PostMapping(value = "/parseWithEquations", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> parseDocumentWithEquations(@RequestParam("file") MultipartFile file) throws IOException, InvalidLLMResponseException {
        logger.debug("Received request to /parseWithEquations endpoint with file: {}", file.getOriginalFilename());

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File cannot be empty."));
        }

        ByteArrayResource fileResource = new ByteArrayResource(file.getBytes());


        String response = chatClient.prompt()
                .user(u -> u.text(promptFactory.pdfTextAndMathExtractor)
                            .media(MimeType.valueOf(Objects.requireNonNull(file.getContentType())), fileResource)).call().content();

        if (response == null) {
            throw new InvalidLLMResponseException("Received empty response from the LLM.");
        }

        // Return the processed response
        return ResponseEntity.ok(Map.of("documentText", response));
    }

    @PostMapping(value = "/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> parseDocument(@RequestParam("file") MultipartFile file) throws IOException {
        logger.debug("Received request to /extract endpoint with file: {}", file.getOriginalFilename());

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File cannot be empty."));
        }

        // Step 1: Parse the document to get plaintext
        logger.info("Step 1: Parsing document '{}' with content type '{}'.", file.getOriginalFilename(), file.getContentType());
        String documentText = parsingService.parseDocument(file);

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

