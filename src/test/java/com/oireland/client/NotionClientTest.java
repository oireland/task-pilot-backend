package com.oireland.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oireland.config.NotionApiConfig;
import com.oireland.model.ExtractedDocDataDTO;
import com.oireland.model.NotionApiV1;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.List;
import java.util.Map;

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
        NotionApiConfig testConfig = new NotionApiConfig("test-notion-token", "test-db-id", "test-version", mockApiBaseUrl);
        notionClient = new NotionClient(WebClient.builder(), testConfig);
    }

    @Test
    void createTasksPage_shouldSendCorrectlyFormattedRequest() throws IOException, InterruptedException {
        // 1. ARRANGE
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));

        var docToCreate = new ExtractedDocDataDTO(
                "Review new feature",
                "Not started",
                "Review the new prompt routing feature",
                List.of("Check everything works as expected", "Ensure no regressions")
        );

        // 2. ACT
        notionClient.createTasksPage(docToCreate);

        // 3. ASSERT
        RecordedRequest recordedRequest = mockWebServer.takeRequest();

        // Assert method and path
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        assertThat(recordedRequest.getPath()).isEqualTo("/v1/pages");

        // Assert headers
        assertThat(recordedRequest.getHeader("Authorization")).isEqualTo("Bearer test-notion-token");
        assertThat(recordedRequest.getHeader("Notion-Version")).isEqualTo("test-version");

        // Assert the request body
        String requestBodyJson = recordedRequest.getBody().readUtf8();
        NotionApiV1.PageCreateRequest parsedRequest = objectMapper.readValue(requestBodyJson, NotionApiV1.PageCreateRequest.class);

        // Verify core properties
        assertThat(parsedRequest.parent().databaseId()).isEqualTo("test-db-id");

        // Verify task name
        assertThat(parsedRequest.properties().taskName().get("title")
                .getFirst().get("text").get("content"))
                .isEqualTo("Review new feature");

        // Verify status
        assertThat(parsedRequest.properties().status().get("status").get("name"))
                .isEqualTo("Not started");

        // Verify description
        assertThat(parsedRequest.properties().description().get("rich_text")
                .getFirst().get("text").get("content"))
                .isEqualTo("Review the new prompt routing feature");

        // Verify children to_do blocks
        List<Map<String, Object>> children = parsedRequest.children();
        assertThat(children).hasSize(2);

        // Verify first to_do block
        Map<String, Object> firstTodo = children.getFirst();
        assertThat(firstTodo.get("type")).isEqualTo("to_do");
        Map<String, Object> firstTodoContent = (Map<String, Object>) firstTodo.get("to_do");
        List<Map<String, Object>> firstTodoRichText = (List<Map<String, Object>>) firstTodoContent.get("rich_text");
        assertThat(((Map<String, String>) firstTodoRichText.getFirst().get("text")).get("content"))
                .isEqualTo("Check everything works as expected");

        // Verify second to_do block
        Map<String, Object> secondTodo = children.get(1);
        assertThat(secondTodo.get("type")).isEqualTo("to_do");
        Map<String, Object> secondTodoContent = (Map<String, Object>) secondTodo.get("to_do");
        List<Map<String, Object>> secondTodoRichText = (List<Map<String, Object>>) secondTodoContent.get("rich_text");
        assertThat(((Map<String, String>) secondTodoRichText.getFirst().get("text")).get("content"))
                .isEqualTo("Ensure no regressions");
    }
}