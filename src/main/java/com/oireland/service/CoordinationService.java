package com.oireland.service;

import com.oireland.dto.TaskListDTO;
import com.oireland.exception.InvalidHuggingFaceResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CoordinationService {

    private static final Logger logger = LoggerFactory.getLogger(CoordinationService.class);
    private final TaskRouterService taskRouterService;
    private final NotionPageService notionPageService;

    public CoordinationService(TaskRouterService taskRouterService, NotionPageService notionPageService) {
        this.taskRouterService = taskRouterService;
        this.notionPageService = notionPageService;
    }

    /**
     * The main entry point for the entire process. It extracts tasks from a document
     * and then orchestrates their creation in Notion.
     * @param documentText The raw text from the input document.
     */
    public void processDocumentAndCreateTasks(String documentText) throws InvalidHuggingFaceResponseException {
        // Step 1: Extract tasks using the router
        logger.info("Step 1: Starting task extraction.");
        TaskListDTO extractedTasks = taskRouterService.processDocument(documentText);

        if (extractedTasks == null || extractedTasks.tasks().isEmpty()) {
            logger.info("Extraction complete. No tasks found to create in Notion.");
            return;
        }

        // Step 2: Create pages in Notion using the dedicated service
        logger.info("Step 2: Passing {} extracted tasks to Notion page service.", extractedTasks.tasks().size());
        notionPageService.createPagesFromTasks(extractedTasks.tasks());

        logger.info("Document processing complete.");
    }
}
