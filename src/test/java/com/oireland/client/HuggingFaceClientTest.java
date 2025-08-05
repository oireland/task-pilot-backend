package com.oireland.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oireland.config.HuggingFaceApiConfig;
import com.oireland.dto.TaskListDTO;
import com.oireland.exception.InvalidHuggingFaceResponseException;
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

public class HuggingFaceClientTest {

    private static MockWebServer mockWebServer;
    private HuggingFaceClient huggingFaceClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

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
        // For each test we create a new client pointing to our mock server
        String baseUrl = String.format("http://localhost:%s", mockWebServer.getPort());
        HuggingFaceApiConfig testConfig = new HuggingFaceApiConfig(baseUrl, "test-token"); // Use a dummy token
        huggingFaceClient = new HuggingFaceClient(WebClient.builder(), testConfig, objectMapper);
    }

    @Test
    void executePrompt_shouldReturnDeserializedObject_whenApiSucceeds() throws InterruptedException, InvalidHuggingFaceResponseException {
        // 1. ARRANGE
        String mockApiResponse = """
        [
          {
            "generated_text": "{\\"tasks\\":[{\\"Task Name\\":\\"Test Exercise 1\\",\\"Status\\":\\"Not started\\",\\"Description\\":\\"This is a test description.\\"}]}"
          }
        ]
        """;
        mockWebServer.enqueue(new MockResponse()
                .setBody(mockApiResponse)
                .addHeader("Content-Type", "application/json"));

        String dummyPrompt = "This is a dummy prompt for testing.";

        // 2. ACT
        // The key change: Call the new method and pass the expected class.
        TaskListDTO result = huggingFaceClient.executePrompt(dummyPrompt, TaskListDTO.class);

        // 3. ASSERT
        // All assertions remain the same and are still valid.
        assertThat(result).isNotNull();
        assertThat(result.tasks()).hasSize(1);
        assertThat(result.tasks().getFirst().taskName()).isEqualTo("Test Exercise 1");

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        assertThat(recordedRequest.getHeader("Authorization")).isEqualTo("Bearer test-token");
    }
}
