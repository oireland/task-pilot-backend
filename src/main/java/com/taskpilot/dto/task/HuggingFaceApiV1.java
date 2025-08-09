package com.taskpilot.dto.task;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class HuggingFaceApiV1 {
    public record ChatCompletion(
            String id,
            String object,
            long created,
            String model,
            List<Choice> choices,
            @JsonProperty("system_fingerprint") String systemFingerprint,
            Usage usage
    ) {}

    public record Choice(
            int index,
            Message message,
            @JsonProperty("finish_reason") String finishReason
    ) {}

    public record Message(
            String role,
            String content
    ) {}

    public record Usage(
            @JsonProperty("prompt_tokens") int promptTokens,
            @JsonProperty("completion_tokens") int completionTokens,
            @JsonProperty("total_tokens") int totalTokens
    ) {}
}
