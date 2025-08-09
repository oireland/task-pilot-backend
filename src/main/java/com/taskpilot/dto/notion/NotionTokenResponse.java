package com.taskpilot.dto.notion;

import com.fasterxml.jackson.annotation.JsonProperty;

// A record is a concise way to create an immutable data carrier class
public record NotionTokenResponse(
        @JsonProperty("access_token")
        String accessToken,

        @JsonProperty("workspace_id")
        String workspaceId,

        @JsonProperty("workspace_name")
        String workspaceName,

        @JsonProperty("workspace_icon")
        String workspaceIcon,

        @JsonProperty("bot_id")
        String botId,

        @JsonProperty("owner")
        Owner owner
) {
    public record Owner(
            @JsonProperty("type")
            String type,
            @JsonProperty("user")
            User user
    ) {}

    public record User(
            @JsonProperty("object")
            String object,
            @JsonProperty("id")
            String id,
            @JsonProperty("name")
            String name,
            @JsonProperty("avatar_url")
            String avatarUrl,
            @JsonProperty("type")
            String type,
            @JsonProperty("person")
            Person person
    ) {}

    public record Person(
            @JsonProperty("email")
            String email
    ) {}
}
