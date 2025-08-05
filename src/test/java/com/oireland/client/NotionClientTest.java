package com.oireland.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oireland.config.NotionApiConfig;
import com.oireland.dto.TaskDTO;
import com.oireland.notion.NotionApiV1;
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

class NotionClientTest {

    private static MockWebServer mockWebServer;
    private NotionClient notionClient;
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
    void initialize() {
        String mockApiBaseUrl = String.format("http://localhost:%s", mockWebServer.getPort());
        // Create a test-specific config for Notion
        NotionApiConfig testConfig = new NotionApiConfig("test-notion-token", "test-db-id", "test-version", mockApiBaseUrl);
        notionClient = new NotionClient(WebClient.builder(), testConfig);
    }

    @Test
    void createTask_shouldSendCorrectlyFormattedRequest() throws IOException, InterruptedException {
        // 1. ARRANGE
        // The Notion API returns a 200 OK with the created page object, but our client
        // doesn't use it, so an empty 200 is fine for this test.
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));

        // The input DTO for our method
        var taskToCreate = new TaskDTO("Review new feature", "Not started", "Review the new prompt routing feature.");

        // 2. ACT
        notionClient.createTask(taskToCreate);

        // 3. ASSERT
        // Verify the request that was sent TO our mock server.
        RecordedRequest recordedRequest = mockWebServer.takeRequest();

        // Assert method and path
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        assertThat(recordedRequest.getPath()).isEqualTo("/v1/pages");

        // Assert headers
        assertThat(recordedRequest.getHeader("Authorization")).isEqualTo("Bearer test-notion-token");
        assertThat(recordedRequest.getHeader("Notion-Version")).isEqualTo("test-version");

        // Assert the request body by parsing it back into our API DTOs
        String requestBodyJson = recordedRequest.getBody().readUtf8();
        NotionApiV1.PageCreateRequest parsedRequest = objectMapper.readValue(requestBodyJson, NotionApiV1.PageCreateRequest.class);

        assertThat(parsedRequest.parent().databaseId()).isEqualTo("test-db-id");
        assertThat(parsedRequest.properties().taskName().get("title").get(0).get("text").get("content"))
                .isEqualTo("Review new feature");
        assertThat(parsedRequest.properties().status().name()).isEqualTo("Not started");
        assertThat(parsedRequest.properties().description().get("rich_text").get(0).get("text").get("content"))
                .isEqualTo("Review the new prompt routing feature.");
    }
}
