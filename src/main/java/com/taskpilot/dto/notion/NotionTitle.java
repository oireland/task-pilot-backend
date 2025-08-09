package com.taskpilot.dto.notion;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a rich text object, which is used for the database title.
 */
public record NotionTitle(
        String type,
        @JsonProperty("plain_text")
        String plainText
) {}