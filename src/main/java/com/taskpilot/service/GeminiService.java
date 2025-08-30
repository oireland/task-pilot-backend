package com.taskpilot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskpilot.exception.InvalidLLMResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class GeminiService implements LLMService{

    private final Logger logger = LoggerFactory.getLogger(GeminiService.class);

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;


    public GeminiService(ChatClient.Builder builder, ObjectMapper objectMapper) {
        this.chatClient = builder.build();
        this.objectMapper = objectMapper;
    }

    /**
     * Executes a given prompt with the Gemini API and deserializes the response.
     */
    @Override
    public <T> T executePrompt(String prompt, Class<T> responseType) throws InvalidLLMResponseException {
        logger.info("Executing prompt with Gemini.");

        String response = chatClient.prompt().user(u -> u.text(prompt)).call().content();

        if (response == null || response.trim().isEmpty()) {
            throw new InvalidLLMResponseException("Received empty response from the Gemini.");
        }

        String cleanedResponse = cleanLlmJsonResponse(response);

        try {
            return objectMapper.readValue(cleanedResponse, responseType);
        } catch (Exception e) {
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
