package com.oireland.service;

import com.oireland.client.NotionClient;
import com.oireland.dto.ExtractedDocDataDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotionPageService {

    private static final Logger logger = LoggerFactory.getLogger(NotionPageService.class);
    private final NotionClient notionClient;

    public NotionPageService(NotionClient notionClient) {
        this.notionClient = notionClient;
    }

    /**
     * Accepts ExtractedDocDataDTO and creates a Notion page with a to_do list of tasks
     * @param data The data extracted from the document, containing tasks to create in Notion.
     */
    public void createTasksPage(ExtractedDocDataDTO data) {
        if (data == null || data.tasks().isEmpty()) {
            logger.warn("createPagesFromTasks called with no tasks to create.");
            return;
        }

        logger.info("Received {} tasks. Creating pages in Notion...", data.tasks().size());

        try {
            logger.debug("Creating tasks: '{}'", data.title());
            notionClient.createTasksPage(data);
        } catch (Exception e) {
            // The global exception handler will catch client exceptions,
            // but we might want to log here and continue processing other tasks.
            logger.error("Failed to create tasks '{}' in Notion", data.title(), e);
        }

        logger.info("Finished Notion page creation process.");
    }
}
