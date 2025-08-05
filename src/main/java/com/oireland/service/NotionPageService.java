package com.oireland.service;

import com.oireland.client.NotionClient;
import com.oireland.dto.TaskDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotionPageService {

    private static final Logger logger = LoggerFactory.getLogger(NotionPageService.class);
    private final NotionClient notionClient;

    public NotionPageService(NotionClient notionClient) {
        this.notionClient = notionClient;
    }

    /**
     * Accepts a list of TaskDTOs and creates a corresponding page for each in Notion.
     * @param tasks The list of tasks to be created.
     */
    public void createPagesFromTasks(List<TaskDTO> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            logger.warn("createPagesFromTasks called with no tasks to create.");
            return;
        }

        logger.info("Received {} tasks. Creating pages in Notion...", tasks.size());
        tasks.forEach(task -> {
            try {
                logger.debug("Creating task: '{}'", task.taskName());
                notionClient.createTask(task);
            } catch (Exception e) {
                // The global exception handler will catch client exceptions,
                // but we might want to log here and continue processing other tasks.
                logger.error("Failed to create task '{}' in Notion, continuing with next.", task.taskName(), e);
            }
        });
        logger.info("Finished Notion page creation process.");
    }
}
