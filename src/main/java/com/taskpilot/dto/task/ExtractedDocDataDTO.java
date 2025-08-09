package com.taskpilot.dto.task;

import java.util.List;

// This record represents the top-level JSON object returned by the LLM.
public record ExtractedDocDataDTO(
         String title,
         String status,
         String description,
         List<String> tasks){
}
