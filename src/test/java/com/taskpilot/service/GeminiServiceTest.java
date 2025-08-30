package com.taskpilot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskpilot.exception.InvalidLLMResponseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeminiServiceTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ObjectMapper objectMapper;

    private GeminiService geminiService;

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        geminiService = new GeminiService(chatClientBuilder, objectMapper);
    }

    // A simple DTO for testing deserialization
    private static class TestResponse {
        public String field;
    }

    @Test
    void executePrompt_shouldReturnDeserializedObject_whenResponseIsCleanJson() throws Exception {
        // Arrange
        String prompt = "test prompt";
        String jsonResponse = "{\"field\":\"value\"}";
        TestResponse expectedResponse = new TestResponse();
        expectedResponse.field = "value";

        ChatClient.ChatClientRequestSpec mockRequest = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec mockResponse = mock(ChatClient.CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(mockRequest);
        when(mockRequest.user(ArgumentMatchers.<Consumer<ChatClient.PromptUserSpec>>any())).thenReturn(mockRequest);
        when(mockRequest.call()).thenReturn(mockResponse);
        when(mockResponse.content()).thenReturn(jsonResponse);
        when(objectMapper.readValue(eq(jsonResponse), eq(TestResponse.class))).thenReturn(expectedResponse);

        // Act
        TestResponse actualResponse = geminiService.executePrompt(prompt, TestResponse.class);

        // Assert
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.field).isEqualTo(expectedResponse.field);
    }

    @Test
    void executePrompt_shouldReturnDeserializedObject_whenResponseIsWrappedInMarkdown() throws Exception {
        // Arrange
        String prompt = "test prompt";
        String rawResponse = "```json\n{\"field\":\"value\"}\n```";
        String cleanedResponse = "{\"field\":\"value\"}";
        TestResponse expectedResponse = new TestResponse();
        expectedResponse.field = "value";

        ChatClient.ChatClientRequestSpec mockRequest = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec mockResponse = mock(ChatClient.CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(mockRequest);
        when(mockRequest.user(ArgumentMatchers.<Consumer<ChatClient.PromptUserSpec>>any())).thenReturn(mockRequest);
        when(mockRequest.call()).thenReturn(mockResponse);
        when(mockResponse.content()).thenReturn(rawResponse);
        when(objectMapper.readValue(eq(cleanedResponse), eq(TestResponse.class))).thenReturn(expectedResponse);

        // Act
        TestResponse actualResponse = geminiService.executePrompt(prompt, TestResponse.class);

        // Assert
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.field).isEqualTo(expectedResponse.field);
    }

    @Test
    void executePrompt_shouldReturnDeserializedObject_whenResponseIsWrappedInMarkdownWithoutLangId() throws Exception {
        // Arrange
        String prompt = "test prompt";
        String rawResponse = "```\n{\"field\":\"value\"}\n```";
        String cleanedResponse = "{\"field\":\"value\"}";
        TestResponse expectedResponse = new TestResponse();
        expectedResponse.field = "value";

        ChatClient.ChatClientRequestSpec mockRequest = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec mockResponse = mock(ChatClient.CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(mockRequest);
        when(mockRequest.user(ArgumentMatchers.<Consumer<ChatClient.PromptUserSpec>>any())).thenReturn(mockRequest);
        when(mockRequest.call()).thenReturn(mockResponse);
        when(mockResponse.content()).thenReturn(rawResponse);
        when(objectMapper.readValue(eq(cleanedResponse), eq(TestResponse.class))).thenReturn(expectedResponse);

        // Act
        TestResponse actualResponse = geminiService.executePrompt(prompt, TestResponse.class);

        // Assert
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.field).isEqualTo(expectedResponse.field);
    }

    @Test
    void executePrompt_shouldThrowInvalidLLMResponseException_whenResponseIsNull() {
        // Arrange
        String prompt = "test prompt";

        ChatClient.ChatClientRequestSpec mockRequest = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec mockResponse = mock(ChatClient.CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(mockRequest);
        when(mockRequest.user(ArgumentMatchers.<Consumer<ChatClient.PromptUserSpec>>any())).thenReturn(mockRequest);
        when(mockRequest.call()).thenReturn(mockResponse);
        when(mockResponse.content()).thenReturn(null);

        // Act & Assert
        InvalidLLMResponseException exception = assertThrows(InvalidLLMResponseException.class, () -> geminiService.executePrompt(prompt, TestResponse.class));
        assertThat(exception.getMessage()).isEqualTo("Received empty response from the Gemini.");
    }

    @Test
    void executePrompt_shouldThrowInvalidLLMResponseException_whenResponseIsEmpty() {
        // Arrange
        String prompt = "test prompt";

        ChatClient.ChatClientRequestSpec mockRequest = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec mockResponse = mock(ChatClient.CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(mockRequest);
        when(mockRequest.user(ArgumentMatchers.<Consumer<ChatClient.PromptUserSpec>>any())).thenReturn(mockRequest);
        when(mockRequest.call()).thenReturn(mockResponse);
        when(mockResponse.content()).thenReturn("   ");

        // Act & Assert
        InvalidLLMResponseException exception = assertThrows(InvalidLLMResponseException.class, () -> geminiService.executePrompt(prompt, TestResponse.class));
        assertThat(exception.getMessage()).isEqualTo("Received empty response from the Gemini.");
    }

    @Test
    void executePrompt_shouldThrowInvalidLLMResponseException_whenParsingFails() throws JsonProcessingException {
        // Arrange
        String prompt = "test prompt";
        String malformedJsonResponse = "{\"field\":\"value\""; // Malformed JSON

        ChatClient.ChatClientRequestSpec mockRequest = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec mockResponse = mock(ChatClient.CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(mockRequest);
        when(mockRequest.user(ArgumentMatchers.<Consumer<ChatClient.PromptUserSpec>>any())).thenReturn(mockRequest);
        when(mockRequest.call()).thenReturn(mockResponse);
        when(mockResponse.content()).thenReturn(malformedJsonResponse);

        JsonProcessingException cause = new JsonProcessingException("parsing error") {};
        when(objectMapper.readValue(eq(malformedJsonResponse), eq(TestResponse.class))).thenThrow(cause);

        // Act & Assert
        InvalidLLMResponseException exception = assertThrows(InvalidLLMResponseException.class, () -> geminiService.executePrompt(prompt, TestResponse.class));
        assertThat(exception.getMessage()).isEqualTo("Failed to parse API response content");
        assertThat(exception.getCause()).isEqualTo(cause);
    }
}