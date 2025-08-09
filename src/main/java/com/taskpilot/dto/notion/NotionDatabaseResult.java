package com.taskpilot.dto.notion;

import java.util.List;

/**
 * Represents a single database object within the search results.
 */
public record NotionDatabaseResult(
        String object,
        String id,
        List<NotionTitle> title // The title is an array of rich text objects
) {}