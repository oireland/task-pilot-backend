package com.oireland.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oireland.config.HuggingFaceApiConfig;
import com.oireland.dto.HuggingFaceResponseDTO;
import com.oireland.exception.InvalidHuggingFaceResponseException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class HuggingFaceClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private static final String MODEL_PATH = "/models/mistralai/Mistral-7B-Instruct-v0.2";


    public HuggingFaceClient(WebClient.Builder webClientBuilder, HuggingFaceApiConfig config, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder
                .baseUrl(config.baseUrl()) // model endpoint
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.token()) // set auth token
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * Executes a given prompt against the Hugging Face API and deserializes the response
     * into the provided class type.
     *
     * @param prompt The full prompt string to send to the LLM.
     * @param responseType The Class of the object to deserialize the response into.
     * @param <T> The generic type of the response object.
     * @return An object of the specified responseType.
     */
    public <T> T executePrompt(String prompt, Class<T> responseType) throws InvalidHuggingFaceResponseException {
        Map<String, String> requestBody = Map.of("inputs", prompt);

        // 1. First Parse: Get the outer structure.
        HuggingFaceResponseDTO[] apiResponse = webClient
                .post()
                .uri(MODEL_PATH)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(HuggingFaceResponseDTO[].class) // <-- Deserialize into an array of our new DTO
                .block();

        if (apiResponse == null || apiResponse.length == 0 || apiResponse[0].generatedText() == null) {
            throw new InvalidHuggingFaceResponseException("API returned an empty or malformed response.");
        }

        // 2. Second Parse: Get the inner JSON string and parse it.
        String innerJson = apiResponse[0].generatedText();
        try {
            return objectMapper.readValue(innerJson, responseType);
        } catch (JsonProcessingException e) {
            // Or throw a custom exception
            throw new InvalidHuggingFaceResponseException("Failed to parse inner JSON from Hugging Face response.", e);
        }
    }
}
