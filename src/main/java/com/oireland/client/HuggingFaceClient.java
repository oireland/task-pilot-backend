package com.oireland.client;

import com.oireland.config.HuggingFaceApiConfig;
import com.oireland.model.HuggingFaceApiV1;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Component
public class HuggingFaceClient {
    private final WebClient webClient;

    public HuggingFaceClient(WebClient.Builder webClientBuilder, HuggingFaceApiConfig config) {
        this.webClient = webClientBuilder
                .baseUrl(config.baseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.token())
                .build();
    }

    public HuggingFaceApiV1.ChatCompletion chatCompletion(String prompt) {
        Map<String, Object> requestBody = Map.of(
                "messages", List.of(
                        Map.of(
                                "role", "user",
                                "content", prompt
                        )
                ),
                "model", "mistralai/Mistral-7B-Instruct-v0.2:featherless-ai",
                "stream", false
        );

        return webClient
                .post()
                .uri("/v1/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(HuggingFaceApiV1.ChatCompletion.class)
                .block();
    }
}
