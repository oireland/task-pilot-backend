package com.oireland.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

// This class is just a container for our Notion-specific DTOs.
public final class NotionApiV1 {

    // Records for building the 'properties' object
    public record TitleProperty(String content) {
        public Map<String, List<Map<String, Map<String, String>>>> toRequestFormat() {
            return Map.of("title", List.of(Map.of("text", Map.of("content", content))));
        }
    }

    public record StatusProperty(String name) {
        public Map<String, Map<String, String>> toRequestFormat() {
            return Map.of("status", Map.of("name", name));
        }
    }

    public record RichTextProperty(String content) {
        public Map<String, List<Map<String, Map<String, String>>>> toRequestFormat() {
            return Map.of("rich_text", List.of(Map.of("text", Map.of("content", content))));
        }
    }

    // Record for the complete 'properties' section of the request
    public record Properties(
            @JsonProperty("Task Name") Map<String, List<Map<String, Map<String, String>>>> taskName,
            @JsonProperty("Status") Map<String, Map<String, String>> status,
            @JsonProperty("Description") Map<String, List<Map<String, Map<String, String>>>> description
    ) {}

    // Record for the 'parent' section of the request
    public record Parent(@JsonProperty("database_id") String databaseId) {}

    // The top-level request body record
    public record PageCreateRequest(Parent parent, Properties properties) {}

    public static PageCreateRequest buildCreateTaskRequest(TaskDTO task, String databaseId) {
        var titleProp = new NotionApiV1.TitleProperty(task.taskName());
        var statusProp = new NotionApiV1.StatusProperty(task.status());
        var descriptionProp = new NotionApiV1.RichTextProperty(task.description());

        var properties = new NotionApiV1.Properties(
                titleProp.toRequestFormat(),
                statusProp.toRequestFormat(),
                descriptionProp.toRequestFormat()
        );

        var parent = new NotionApiV1.Parent(databaseId);

        return new NotionApiV1.PageCreateRequest(parent, properties);
    }
}
