package com.oireland.model;

import java.util.List;

// This record represents the top-level JSON object returned by the LLM.
public record TaskListDTO(List<TaskDTO> tasks) {
}
