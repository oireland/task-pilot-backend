package com.taskpilot.dto.task;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// This record represents the top-level JSON object returned by the LLM.
public record ExtractedDocDataDTO(
        @JsonProperty("Title") String title,
        @JsonProperty("Status") String status,
        @JsonProperty("Description") String description,
        @JsonProperty("Tasks") List<String> tasks){
}
