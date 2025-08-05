package com.oireland.client;

import com.oireland.config.HuggingFaceApiConfig;
import com.oireland.dto.TaskListDTO;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class HuggingFaceClient {

    private final WebClient webClient;

    public HuggingFaceClient(WebClient.Builder webClientBuilder, HuggingFaceApiConfig config) {
        this.webClient = webClientBuilder
                .baseUrl("https://api-inference.huggingface.co/models/mistralai/Mistral-7B-Instruct-v0.2") // model endpoint
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.token()) // set auth token
                .build();
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
    public <T> T executePrompt(String prompt, Class<T> responseType) {
        Map<String, String> requestBody = Map.of("inputs", prompt);

        return webClient
                .post()
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(responseType)
                .block();
    }
}
