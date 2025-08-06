package com.oireland.model;

import com.fasterxml.jackson.annotation.JsonProperty;

// Represents a single task extracted by the LLM
public record TaskDTO(
        @JsonProperty("Task Name") String taskName,
        @JsonProperty("Status") String status,
        @JsonProperty("Description") String description
) {}
