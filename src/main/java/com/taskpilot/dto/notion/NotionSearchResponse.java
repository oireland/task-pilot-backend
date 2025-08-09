package com.taskpilot.dto.notion;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Represents the top-level response from the Notion search API.
 */
public record NotionSearchResponse(
        String object,
        List<NotionDatabaseResult> results,
        @JsonProperty("next_cursor")
        String nextCursor,
        @JsonProperty("has_more")
        boolean hasMore
) {}