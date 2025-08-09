package com.taskpilot.service;

import com.taskpilot.dto.task.ExtractedDocDataDTO;
import com.taskpilot.exception.InvalidLLMResponseException;
import com.taskpilot.prompt.PromptFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class TaskRouterService {

    private static final Pattern EXERCISE_PATTERN = Pattern.compile("(?i)exercise\\s+\\d+(\\.\\d+)*");
    // Regex to find "Exercise <number>" case-insensitively.
    private final Logger logger = LoggerFactory.getLogger(TaskRouterService.class);
    private final LLMService llmService;
    private final PromptFactory promptFactory;

    public TaskRouterService(LLMService llmService, PromptFactory promptFactory) {
        this.llmService = llmService;
        this.promptFactory = promptFactory;
    }

    public ExtractedDocDataDTO processDocument(String documentText) throws InvalidLLMResponseException {
        String chosenPrompt;

        // The core routing logic
        if (EXERCISE_PATTERN.matcher(documentText).find()) {
            logger.info("Detected 'Exercise' pattern in document text. Using exercise-specific prompt.");
            chosenPrompt = String.format(promptFactory.exercisePatternPromptTemplate, documentText);
        } else {
            logger.info("No 'Exercise' pattern detected. Using general task prompt.");
            chosenPrompt = String.format(promptFactory.generalTaskPromptTemplate, documentText);
        }

        // Call the client with the selected, formatted prompt
        return llmService.executePrompt(chosenPrompt, ExtractedDocDataDTO.class);
    }
}
