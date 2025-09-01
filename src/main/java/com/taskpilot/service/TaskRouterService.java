// src/main/java/com/taskpilot/service/TaskRouterService.java
package com.taskpilot.service;

import com.taskpilot.dto.task.ExtractedTaskListDTO;
import com.taskpilot.exception.InvalidLLMResponseException;
import com.taskpilot.prompt.PromptFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class TaskRouterService {

    // Regex to find "Exercise <number>" case-insensitively.
    private static final Pattern EXERCISE_PATTERN = Pattern.compile("(?i)exercise\\s+\\d+(\\.\\d+)*");
    private final Logger logger = LoggerFactory.getLogger(TaskRouterService.class);
    private final LLMService llmService;
    private final PromptFactory promptFactory;

    public TaskRouterService(@Qualifier("geminiService") LLMService llmService, PromptFactory promptFactory) {
        this.llmService = llmService;
        this.promptFactory = promptFactory;
    }

    public ExtractedTaskListDTO processDocument(String documentText) throws InvalidLLMResponseException {
        if (documentText == null) {
            return null;
        }

        List<String> chunks = splitText(documentText);

        if (chunks.size() <= 1) return processChunk(documentText);

        // Process multiple chunks concurrently
        List<CompletableFuture<ExtractedTaskListDTO>> futures = chunks.stream()
                .map(chunk -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return processChunk(chunk);
                    } catch (InvalidLLMResponseException e) {
                        throw new RuntimeException(e);
                    }
                }))
                .toList();

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        try {
            allFutures.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        List<ExtractedTaskListDTO> results = futures.stream()
                .map(future -> {
                    try {
                        return future.get();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();

        return combineResults(results);

    }

    private ExtractedTaskListDTO processChunk(String chunk) throws InvalidLLMResponseException {
        String chosenPrompt;

        // The core routing logic
        if (EXERCISE_PATTERN.matcher(chunk).find()) {
            logger.info("Detected 'Exercise' pattern in document text. Using exercise-specific prompt.");
            chosenPrompt = String.format(promptFactory.exercisePatternPromptTemplate, chunk);
        } else {
            logger.info("No 'Exercise' pattern detected. Using general task prompt.");
            chosenPrompt = String.format(promptFactory.generalTaskPromptTemplate, chunk);
        }

        // Call the client with the selected, formatted prompt
        return llmService.executePrompt(chosenPrompt, ExtractedTaskListDTO.class);
    }

    private List<String> splitText(String text) {
        List<String> chunks = new ArrayList<>();
        String[] paragraphs = text.split("\n\n"); // Split by double newline
        StringBuilder chunk = new StringBuilder();
        int targetChunkSize = 10000;
        for (String paragraph : paragraphs) {
            if (chunk.length() + paragraph.length() > targetChunkSize && !chunk.isEmpty()) {
                chunks.add(chunk.toString());
                chunk = new StringBuilder();
            }
            chunk.append(paragraph).append("\n\n");
        }
        if (!chunk.isEmpty()) {
            chunks.add(chunk.toString());
        }
        return chunks;
    }

    // src/main/java/com/taskpilot/service/TaskRouterService.java
    private ExtractedTaskListDTO combineResults(List<ExtractedTaskListDTO> results) {
        // Title of the first chunk likely to represent the full document
        String title = results.getFirst().title();

        // Summarise the other descriptions to produce one for the whole document
        String combinedDescription = results.stream()
                .map(ExtractedTaskListDTO::description)
                .collect(Collectors.joining("\n"));

        String finalDescription;
        try {
            finalDescription = llmService.executePrompt("Summarize the following text:\n" + combinedDescription, String.class);
        } catch (InvalidLLMResponseException e) {
            // Handle the exception, perhaps by falling back to the first description
            finalDescription = results.getFirst().description();
        }

        // Include all the todos from all chunks. There shouldn't be any duplicates since the chunks don't overlap.
        List<String> todos = new ArrayList<>();
        for (ExtractedTaskListDTO result : results) {
            todos.addAll(result.todos());
        }
        return new ExtractedTaskListDTO(title, finalDescription, todos);
    }
}