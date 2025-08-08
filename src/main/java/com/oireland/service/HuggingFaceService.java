package com.oireland.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oireland.client.HuggingFaceClient;
import com.oireland.dto.HuggingFaceApiV1;
import com.oireland.exception.InvalidLLMResponseException;
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
     * Executes a given prompt against the Hugging Face API and deserializes the response
     * into the provided class type.
     *
     * @param prompt       The full prompt string to send to the LLM.
     * @param responseType The Class of the object to deserialize the response into.
     * @param <T>          The generic type of the response object.
     * @return An object of the specified responseType.
     */
    public <T> T executePrompt(String prompt, Class<T> responseType) throws InvalidLLMResponseException {
        logger.info("Executing prompt against Hugging Face API");

        HuggingFaceApiV1.ChatCompletion apiResponse = huggingFaceClient.chatCompletion(prompt);

        if (apiResponse == null || apiResponse.choices().isEmpty()) {
            throw new InvalidLLMResponseException("API returned an empty or malformed response.");
        }

        try {
            String content = apiResponse.choices().getFirst().message().content();
            logger.info("Raw API response content: {}", content);
            return objectMapper.readValue(content, responseType);
        } catch (JsonProcessingException e) {
            throw new InvalidLLMResponseException("Failed to parse API response content", e);
        }
    }
}
