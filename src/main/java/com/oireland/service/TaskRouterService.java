package com.oireland.service;

import com.oireland.client.HuggingFaceClient;
import com.oireland.dto.TaskListDTO;
import com.oireland.prompt.PromptFactory;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class TaskRouterService {

    // Regex to find "Exercise <number>" case-insensitively.
    private static final Pattern EXERCISE_PATTERN = Pattern.compile("(?i)exercise\\s+\\d+(\\.\\d+)*");

    private final HuggingFaceClient hfClient;
    private final PromptFactory promptFactory;

    public TaskRouterService(HuggingFaceClient hfClient, PromptFactory promptFactory) {
        this.hfClient = hfClient;
        this.promptFactory = promptFactory;
    }

    public TaskListDTO processDocument(String documentText) {
        String chosenPrompt;

        // The core routing logic
        if (EXERCISE_PATTERN.matcher(documentText).find()) {
            chosenPrompt = String.format(promptFactory.exercisePatternPromptTemplate, documentText);
        } else {
            chosenPrompt = String.format(promptFactory.generalTaskPromptTemplate, documentText);
        }

        // Call the client with the selected, formatted prompt
        return hfClient.executePrompt(chosenPrompt, TaskListDTO.class);
    }
}
