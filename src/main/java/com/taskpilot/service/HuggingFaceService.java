package com.taskpilot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskpilot.client.HuggingFaceClient;
import com.taskpilot.dto.task.HuggingFaceApiV1;
import com.taskpilot.exception.InvalidLLMResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class HuggingFaceService implements LLMService {

    private final Logger logger = LoggerFactory.getLogger(HuggingFaceService.class);
    private final HuggingFaceClient huggingFaceClient;
    private final ObjectMapper objectMapper;

    public HuggingFaceService(HuggingFaceClient huggingFaceClient, ObjectMapper objectMapper) {
        this.huggingFaceClient = huggingFaceClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Executes a given prompt against the Hugging Face API and deserializes the response.
     */
    public <T> T executePrompt(String prompt, Class<T> responseType) throws InvalidLLMResponseException {
        logger.info("Executing prompt against Hugging Face API");

        HuggingFaceApiV1.ChatCompletion apiResponse = huggingFaceClient.chatCompletion(prompt);

        if (apiResponse == null || apiResponse.choices().isEmpty()) {
            throw new InvalidLLMResponseException("API returned an empty or malformed response.");
        }

        try {
            String rawContent = apiResponse.choices().getFirst().message().content();
            logger.info("Raw API response content: {}", rawContent);

            // Clean the JSON string before parsing
            String cleanedContent = cleanLlmJsonResponse(rawContent);
            logger.info("Cleaned API response content: {}", cleanedContent);

            return objectMapper.readValue(cleanedContent, responseType);
        } catch (JsonProcessingException e) {
            throw new InvalidLLMResponseException("Failed to parse API response content", e);
        }
    }

    /**
     * Cleans the raw JSON string from the LLM.
     * It removes markdown code fences (```) and the optional "json" language identifier.
     */
    private String cleanLlmJsonResponse(String content) {
        String cleaned = content.trim();

        // First, check for and remove markdown code block fences
        if (cleaned.startsWith("```") && cleaned.endsWith("```")) {
            cleaned = cleaned.substring(3, cleaned.length() - 3).trim();
        }

        // After removing fences, check for and remove the "json" prefix
        if (cleaned.toLowerCase().startsWith("json")) {
            cleaned = cleaned.substring(4).trim();
        }

        return cleaned;
    }
}