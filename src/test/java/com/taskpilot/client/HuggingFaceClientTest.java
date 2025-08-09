package com.taskpilot.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskpilot.config.HuggingFaceApiConfig;
import com.taskpilot.dto.task.ExtractedDocDataDTO;
import com.taskpilot.exception.InvalidLLMResponseException;
import com.taskpilot.service.HuggingFaceService;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class HuggingFaceClientTest {

    private static MockWebServer mockWebServer;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private HuggingFaceService huggingFaceService;

    @BeforeAll
    static void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @BeforeEach
    void initialise() {
        String baseUrl = String.format("http://localhost:%s", mockWebServer.getPort());
        HuggingFaceApiConfig testConfig = new HuggingFaceApiConfig(baseUrl, "test-token");
        HuggingFaceClient testClient = new HuggingFaceClient(WebClient.builder(), testConfig);
        huggingFaceService = new HuggingFaceService(testClient, objectMapper);
    }

    @Test
    void executePrompt_shouldReturnDeserializedObject_whenApiSucceeds() throws InterruptedException, InvalidLLMResponseException {
        // 1. ARRANGE
        String mockApiResponse = """
        {
            "id": "chatcmpl-123",
            "object": "chat.completion",
            "created": 1677858242,
            "model": "moonshotai/Kimi-K2-Instruct",
            "choices": [
                {
                    "message": {
                        "role": "assistant",
                        "content": "{\\"Title\\": \\"Test Document\\", \\"Status\\": \\"In Progress\\", \\"Description\\": \\"This is a test description\\", \\"Tasks\\": [\\"Test Exercise 1\\"]}"
                    },
                    "index": 0,
                    "finish_reason": "stop"
                }
            ]
        }
        """;
        mockWebServer.enqueue(new MockResponse()
                .setBody(mockApiResponse)
                .addHeader("Content-Type", "application/json"));

        String dummyPrompt = "This is a dummy prompt for testing.";

        // 2. ACT
        ExtractedDocDataDTO result = huggingFaceService.executePrompt(dummyPrompt, ExtractedDocDataDTO.class);

        // 3. ASSERT
        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo("Test Document");
        assertThat(result.status()).isEqualTo("In Progress");
        assertThat(result.description()).isEqualTo("This is a test description");
        assertThat(result.tasks()).containsExactly("Test Exercise 1");

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        assertThat(recordedRequest.getHeader("Authorization")).isEqualTo("Bearer test-token");
    }

    @Test
    void executePrompt_shouldThrowException_whenApiReturnsEmptyResponse() {
        // ARRANGE
        String emptyResponse = """
                {
                    "id": "chatcmpl-123",
                    "choices": []
                }
                """;
        mockWebServer.enqueue(new MockResponse()
                .setBody(emptyResponse)
                .addHeader("Content-Type", "application/json"));

        // ACT & ASSERT
        assertThrows(InvalidLLMResponseException.class, () ->
                huggingFaceService.executePrompt("test prompt", ExtractedDocDataDTO.class)
        );
    }
}