package com.taskpilot.parser;

import com.taskpilot.exception.InvalidLLMResponseException;
import com.taskpilot.prompt.PromptFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EquationParserTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    @Mock
    private PromptFactory promptFactory;

    private EquationParser equationParser;
    private static final String TEST_RESPONSE = "Extracted text with equations: E=mc²";

    @BeforeEach
    void setUp() {
        // Only stub what's always needed - the builder
        when(chatClientBuilder.build()).thenReturn(chatClient);
        equationParser = new EquationParser(chatClientBuilder, promptFactory);
    }

    private void setupChatClientMocks() {
        // Set up the ChatClient method chaining for parse() tests
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(ArgumentMatchers.<Consumer<ChatClient.PromptUserSpec>>any())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
    }

    @Test
    @DisplayName("supports() should always return false")
    void supports_ShouldAlwaysReturnFalse() {
        assertFalse(equationParser.supports("application/pdf"));
        assertFalse(equationParser.supports("text/plain"));
        assertFalse(equationParser.supports("image/jpeg"));
        assertFalse(equationParser.supports(null));
    }

    @Test
    @DisplayName("parse() should return LLM response for valid input")
    void parse_ShouldReturnLLMResponse() throws IOException, InvalidLLMResponseException {
        // ARRANGE
        setupChatClientMocks();
        MockMultipartFile testFile = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "test content".getBytes()
        );

        when(callResponseSpec.content()).thenReturn(TEST_RESPONSE);

        // ACT
        String result = equationParser.parse(testFile);

        // ASSERT
        assertEquals(TEST_RESPONSE, result);
        verify(chatClient).prompt();
        verify(requestSpec).user(any(Consumer.class));
        verify(requestSpec).call();
        verify(callResponseSpec).content();
    }

    @Test
    @DisplayName("parse() should throw InvalidLLMResponseException for null response")
    void parse_ShouldThrowExceptionForNullResponse() {
        // ARRANGE
        setupChatClientMocks();
        MockMultipartFile testFile = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "test content".getBytes()
        );

        when(callResponseSpec.content()).thenReturn(null);

        // ACT & ASSERT
        InvalidLLMResponseException exception = assertThrows(
                InvalidLLMResponseException.class,
                () -> equationParser.parse(testFile)
        );

        assertEquals("Received empty response from the LLM.", exception.getMessage());
    }

    @Test
    @DisplayName("parse() should throw InvalidLLMResponseException for empty response")
    void parse_ShouldThrowExceptionForEmptyResponse() {
        // ARRANGE
        setupChatClientMocks();
        MockMultipartFile testFile = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "test content".getBytes()
        );

        when(callResponseSpec.content()).thenReturn("");

        // ACT & ASSERT
        InvalidLLMResponseException exception = assertThrows(
                InvalidLLMResponseException.class,
                () -> equationParser.parse(testFile)
        );

        assertEquals("Received empty response from the LLM.", exception.getMessage());
    }

    @Test
    @DisplayName("parse() should throw InvalidLLMResponseException for whitespace-only response")
    void parse_ShouldThrowExceptionForWhitespaceOnlyResponse() {
        // ARRANGE
        setupChatClientMocks();
        MockMultipartFile testFile = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "test content".getBytes()
        );

        when(callResponseSpec.content()).thenReturn("   \n\t  ");

        // ACT & ASSERT
        InvalidLLMResponseException exception = assertThrows(
                InvalidLLMResponseException.class,
                () -> equationParser.parse(testFile)
        );

        assertEquals("Received empty response from the LLM.", exception.getMessage());
    }
}