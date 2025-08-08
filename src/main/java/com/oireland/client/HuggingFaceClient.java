package com.oireland.client;

import com.oireland.config.HuggingFaceApiConfig;
import com.oireland.dto.HuggingFaceApiV1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class HuggingFaceClient {
    private final Logger logger = LoggerFactory.getLogger(HuggingFaceClient.class);
    private final WebClient webClient;

    public HuggingFaceClient(WebClient.Builder webClientBuilder, HuggingFaceApiConfig config) {
        this.webClient = webClientBuilder
                .baseUrl(config.baseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.token())
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create()
                        .responseTimeout(Duration.ofSeconds(30))))
                .build();
    }

    public HuggingFaceApiV1.ChatCompletion chatCompletion(String prompt) {
        String MODEL_NAME = "moonshotai/Kimi-K2-Instruct";
        Map<String, Object> requestBody = Map.of(
                "messages", List.of(
                        Map.of(
                                "role", "user",
                                "content", prompt
                        )
                ),
                "model", MODEL_NAME,
                "stream", false
        );

        logger.debug("Sending request to HuggingFace API with body: {}", requestBody);

        return webClient
                .post()
                .uri("/v1/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(HuggingFaceApiV1.ChatCompletion.class)
                .doOnNext(response -> logger.debug("Received response: {}", response))
                .doOnError(error -> logger.error("Error calling HuggingFace API: ", error))
                .block();
    }
}
