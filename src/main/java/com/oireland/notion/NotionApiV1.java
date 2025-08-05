package com.oireland.notion;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

// This class is just a container for our Notion-specific DTOs.
public final class NotionApiV1 {

    // Records for building the 'properties' object
    public record TitleProperty(String content) {
        public List<Map<String, Map<String, String>>> toRequestFormat() {
            return List.of(Map.of("text", Map.of("content", content)));
        }
    }

    public record StatusProperty(String name) {}

    public record RichTextProperty(String content) {
        public List<Map<String, Map<String, String>>> toRequestFormat() {
            return List.of(Map.of("text", Map.of("content", content)));
        }
    }

    // Record for the complete 'properties' section of the request
    public record Properties(
            @JsonProperty("Task Name") Map<String, List<Map<String, Map<String, String>>>> taskName,
            @JsonProperty("Status") StatusProperty status,
            @JsonProperty("Description") Map<String, List<Map<String, Map<String, String>>>> description
    ) {}

    // Record for the 'parent' section of the request
    public record Parent(@JsonProperty("database_id") String databaseId) {}

    // The top-level request body record
    public record PageCreateRequest(Parent parent, Properties properties) {}
}
