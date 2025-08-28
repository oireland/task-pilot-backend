package com.taskpilot.dto.notion;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * A helper class for building request objects for the Notion API.
 * This class provides static methods to construct payloads for creating pages and databases.
 * The returned objects can be serialized to JSON by a web client.
 */
public class NotionApiV1 {

    // --- Method 1: Create a new, empty page in a parent database ---

    /**
     * Builds the request object to create a new, empty page within an existing database.
     *
     * @param title            The text for the 'Title' property of the new page.
     * @param status           The value for the 'Status' property (e.g., "In Progress").
     * @param description      The text for the 'Description' property.
     * @param parentDatabaseId The ID of the database where this new page will be created.
     * @return A NotionPageRequest object ready to be serialized into JSON.
     */
    public static NotionPageRequest buildCreatePageRequest(String title, String status, String description, String parentDatabaseId) {
        var pageProperties = new ParentDBPageProperties(
                new TitlePropertyValue(List.of(new RichTextObject(new TextContent(title)))),
                new StatusPropertyValue(new StatusContent(status)),
                new RichTextPropertyValue(List.of(new RichTextObject(new TextContent(description))))
        );

        return new NotionPageRequest(new Parent("database_id", parentDatabaseId), pageProperties, null);
    }


    // --- Method 2: Create a new database on a specific page ---

    /**
     * Builds the request object to create a new, inline database on a specific page.
     *
     * @param parentPageId The ID of the page where this new database will be created.
     * @return A NotionDatabaseRequest object ready to be serialized into JSON.
     */
    public static NotionDatabaseRequest buildCreateDatabaseRequest(String parentPageId) {
        var dbProperties = new ChildDBProperties(
                new TitleDatabaseProperty(Map.of()),
                new SelectDatabaseProperty(new SelectOptions(List.of(
                        new SelectOption("Not Started", "gray"),
                        new SelectOption("In Progress", "blue"),
                        new SelectOption("Done", "green")
                ))),
                new DateDatabaseProperty(Map.of())
        );

        return new NotionDatabaseRequest(
                new Parent("page_id", parentPageId),
                true,
                List.of(new RichTextObject(new TextContent("Todo List"))), // Database Title
                dbProperties
        );
    }


    // --- Method 3: Add a new page (a to-do item) to the new database ---

    /**
     * Builds the request object to add a new page (a to-do item) to the newly created database.
     *
     * @param todoContent      The text for the 'Todo' title property.
     * @param status           The value for the 'Status' select property.
     * @param deadline         The optional deadline date string (YYYY-MM-DD). Can be null.
     * @param parentDatabaseId The ID of the newly created database where this to-do will be added.
     * @return A NotionPageRequest object ready to be serialized into JSON.
     */
    public static NotionPageRequest buildAddTodoRequest(String todoContent, String status, String deadline, String parentDatabaseId) {
        DatePropertyValue dateProperty = (deadline != null && !deadline.isEmpty())
                ? new DatePropertyValue(new DateContent(deadline))
                : null;

        var todoProperties = new TodoPageProperties(
                new TitlePropertyValue(List.of(new RichTextObject(new TextContent(todoContent)))),
                new SelectPropertyValue(new SelectContent(status)),
                dateProperty
        );

        return new NotionPageRequest(new Parent("database_id", parentDatabaseId), todoProperties, null);
    }


    // --- RECORDS FOR BUILDING THE NOTION API REQUESTS ---

    // --- Generic Records ---
    public record Parent(String type, @JsonProperty("database_id") @JsonInclude(JsonInclude.Include.NON_NULL) String databaseId, @JsonProperty("page_id") @JsonInclude(JsonInclude.Include.NON_NULL) String pageId) {
        public Parent(String type, String id) {
            this(type, "database_id".equals(type) ? id : null, "page_id".equals(type) ? id : null);
        }
    }
    public record TextContent(String content) {}
    public record RichTextObject(String type, TextContent text) {
        public RichTextObject(TextContent text) { this("text", text); }
    }

    // --- Page Creation Records ---
    public record TitlePropertyValue(List<RichTextObject> title) {}
    public record StatusContent(String name) {}
    public record StatusPropertyValue(StatusContent status) {}
    public record RichTextPropertyValue(@JsonProperty("rich_text") List<RichTextObject> richText) {}
    public record SelectContent(String name) {}
    public record SelectPropertyValue(SelectContent select) {}
    public record DateContent(String start) {}
    public record DatePropertyValue(DateContent date) {}

    // --- Top-Level Page Request Record ---
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record NotionPageRequest(Parent parent, Object properties, List<Object> children) {}

    // --- Property Sets for Different Page Types ---
    public record ParentDBPageProperties(
            @JsonProperty("Title") TitlePropertyValue title,
            @JsonProperty("Status") StatusPropertyValue status,
            @JsonProperty("Description") RichTextPropertyValue description
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record TodoPageProperties(
            @JsonProperty("Todo") TitlePropertyValue todo,
            @JsonProperty("Status") SelectPropertyValue status,
            @JsonProperty("Deadline") DatePropertyValue deadline
    ) {}

    // --- Database Creation Records ---
    public record SelectOption(String name, String color) {}
    public record SelectOptions(List<SelectOption> options) {}
    public record TitleDatabaseProperty(Map<String, Object> title) {}
    public record SelectDatabaseProperty(SelectOptions select) {}
    public record DateDatabaseProperty(Map<String, Object> date) {}

    public record ChildDBProperties(
            @JsonProperty("Todo") TitleDatabaseProperty todo,
            @JsonProperty("Status") SelectDatabaseProperty status,
            @JsonProperty("Deadline") DateDatabaseProperty deadline
    ) {}

    public record NotionDatabaseRequest(Parent parent, @JsonProperty("is_inline") boolean isInline, List<RichTextObject> title, ChildDBProperties properties) {}
}
