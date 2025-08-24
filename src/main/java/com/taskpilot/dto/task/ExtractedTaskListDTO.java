package com.taskpilot.dto.task;

import java.util.List;

// This record represents the top-level JSON object returned by the LLM.
public record ExtractedTaskListDTO(
         String title,
         String description,
         List<String> tasks){
}
