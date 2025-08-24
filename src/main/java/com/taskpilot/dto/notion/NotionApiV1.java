package com.taskpilot.dto.notion;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.taskpilot.dto.task.ExtractedTaskListDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class NotionApiV1 {

    // --- Records for Notion API JSON Structure ---

    // A generic block for any rich text content (text or equation)
    @JsonInclude(JsonInclude.Include.NON_NULL) // Omit null fields from JSON
    public record RichTextBlock(String type, TextContent text, EquationContent equation) {}
    public record TextContent(String content) {}
    public record EquationContent(String expression) {}

    // Property holders for Page properties
    public record TitleProperty(@JsonProperty("title") List<RichTextBlock> content) {}
    public record RichTextProperty(@JsonProperty("rich_text") List<RichTextBlock> content) {}
    public record StatusProperty(@JsonProperty("status") Status status) {}
    public record Status(String name) {}

    // Main properties object
    public record Properties(
            @JsonProperty("Title") TitleProperty title,
            @JsonProperty("Status") StatusProperty status,
            @JsonProperty("Description") RichTextProperty description
    ) {}

    // To-Do block for the page content (children)
    public record TodoBlock(@JsonProperty("to_do") Todo todo) {
        public String getObject() { return "block"; }
        public String getType() { return "to_do"; }
    }
    public record Todo(@JsonProperty("rich_text") List<RichTextBlock> richText, boolean checked) {}

    // Parent object for the page
    public record Parent(@JsonProperty("database_id") String databaseId) {}

    // The final request body for creating a page
    public record PageCreateRequest(
            Parent parent,
            Properties properties,
            List<TodoBlock> children
    ) {}


    // --- Builder and Parser Logic ---

    // Regex to find content inside (/ ... /)
    private static final Pattern EQUATION_PATTERN = Pattern.compile("\\(/\\s*(.*?)\\s*/\\)");

    /**
     * Parses a task string and converts it into a list of Notion RichText objects.
     * It handles both plain text and equations wrapped in (/ ... /).
     */
    private static List<RichTextBlock> parseTaskToRichText(String task) {
        List<RichTextBlock> richTextList = new ArrayList<>();
        Matcher matcher = EQUATION_PATTERN.matcher(task);
        int lastEnd = 0;

        while (matcher.find()) {
            // Add the plain text captured before the equation
            if (matcher.start() > lastEnd) {
                String textSegment = task.substring(lastEnd, matcher.start());
                richTextList.add(new RichTextBlock("text", new TextContent(textSegment), null));
            }

            // Add the equation itself from capture group 1
            String equationExpression = matcher.group(1);
            richTextList.add(new RichTextBlock("equation", null, new EquationContent(equationExpression)));

            lastEnd = matcher.end();
        }

        // Add any remaining plain text after the last equation
        if (lastEnd < task.length()) {
            String remainingText = task.substring(lastEnd);
            richTextList.add(new RichTextBlock("text", new TextContent(remainingText), null));
        }

        return richTextList;
    }

    private static final String DEFAULT_STATUS = "Not Started";

    /**
     * Builds the final request object to create a new page in Notion.
     */
    public static PageCreateRequest buildCreateTaskRequest(ExtractedTaskListDTO doc, String databaseId) {
        var properties = new Properties(
                new TitleProperty(List.of(new RichTextBlock("text", new TextContent(doc.title()), null))),
                new StatusProperty(new Status(DEFAULT_STATUS)),
                new RichTextProperty(List.of(new RichTextBlock("text", new TextContent(doc.description()), null)))
        );

        var children = doc.tasks().stream()
                .map(task -> new TodoBlock(new Todo(parseTaskToRichText(task), false)))
                .collect(Collectors.toList());

        var parent = new Parent(databaseId);

        return new PageCreateRequest(parent, properties, children);
    }
}