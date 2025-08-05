package com.oireland.service;

import com.oireland.dto.TaskListDTO;
import com.oireland.exception.InvalidHuggingFaceResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class DocumentProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentProcessingService.class);
    private final DocumentParsingService parsingService;
    private final TaskRouterService taskRouterService;
    private final NotionPageService notionPageService;

    public DocumentProcessingService(DocumentParsingService parsingService, TaskRouterService taskRouterService, NotionPageService notionPageService) {
        this.parsingService = parsingService;
        this.taskRouterService = taskRouterService;
        this.notionPageService = notionPageService;
    }

    /**
     * The main entry point for the entire process. It extracts tasks from a document
     * and then orchestrates their creation in Notion.
     * @param file The input document.
     */
    public void processDocumentAndCreateTasks(MultipartFile file) throws IOException, InvalidHuggingFaceResponseException {
        // Step 1: Parse the document to get plaintext
        logger.info("Step 1: Parsing document '{}' with content type '{}'.", file.getOriginalFilename(), file.getContentType());
        String documentText = parsingService.parseDocument(file);

        // Step 2: Extract tasks using the router
        logger.info("Step 2: Starting task extraction.");
        TaskListDTO extractedTasks = taskRouterService.processDocument(documentText);

        if (extractedTasks == null || extractedTasks.tasks().isEmpty()) {
            logger.info("Extraction complete. No tasks found to create in Notion.");
            return;
        }

        // Step 3: Create pages in Notion using the dedicated service
        logger.info("Step 3: Passing {} extracted tasks to Notion page service.", extractedTasks.tasks().size());
        notionPageService.createPagesFromTasks(extractedTasks.tasks());

        logger.info("Document processing complete.");
    }
}
