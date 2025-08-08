package com.oireland.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public final class NotionApiV1 {
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

    public record TodoBlock(String content, boolean checked) {
        public Map<String, Object> toRequestFormat() {
            return Map.of(
                    "object", "block",
                    "type", "to_do",
                    "to_do", Map.of(
                            "rich_text", List.of(Map.of("text", Map.of("content", content))),
                            "checked", checked
                    )
            );
        }
    }

    public record Properties(
            @JsonProperty("Title") Map<String, List<Map<String, Map<String, String>>>> taskName,
            @JsonProperty("Status") Map<String, Map<String, String>> status,
            @JsonProperty("Description") Map<String, List<Map<String, Map<String, String>>>> description
    ) {}

    public record Parent(@JsonProperty("database_id") String databaseId) {}

    public record PageCreateRequest(
            Parent parent,
            Properties properties,
            List<Map<String, Object>> children
    ) {}

    public static PageCreateRequest buildCreateTaskRequest(ExtractedDocDataDTO doc, String databaseId) {
        var titleProp = new TitleProperty(doc.title());
        var statusProp = new StatusProperty(doc.status());
        var descriptionProp = new RichTextProperty(doc.description());

        var properties = new Properties(
                titleProp.toRequestFormat(),
                statusProp.toRequestFormat(),
                descriptionProp.toRequestFormat()
        );

        var children = doc.tasks().stream()
                .map(task -> new TodoBlock(task, false).toRequestFormat())
                .toList();

        var parent = new Parent(databaseId);

        return new PageCreateRequest(parent, properties, children);
    }
}