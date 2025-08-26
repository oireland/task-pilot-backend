package com.taskpilot.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class PlainTextParserTest {

    private PlainTextParser plainTextParser;
    private static final String TEST_CONTENT = "Hello, World!\nThis is a test document.";

    @BeforeEach
    void setUp() {
        plainTextParser = new PlainTextParser();
    }

    @Test
    @DisplayName("supports() should return true for text mime types")
    void supports_ShouldReturnTrueForTextMimeTypes() {
        assertTrue(plainTextParser.supports("text/plain"));
        assertTrue(plainTextParser.supports("text/html"));
        assertTrue(plainTextParser.supports("text/csv"));
        assertTrue(plainTextParser.supports("text/javascript"));
    }

    @Test
    @DisplayName("supports() should return false for non-text mime types")
    void supports_ShouldReturnFalseForNonTextMimeTypes() {
        assertFalse(plainTextParser.supports("application/pdf"));
        assertFalse(plainTextParser.supports("image/jpeg"));
        assertFalse(plainTextParser.supports("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
    }

    @Test
    @DisplayName("supports() should return false for null mime type")
    void supports_ShouldReturnFalseForNullMimeType() {
        assertFalse(plainTextParser.supports(null));
    }

    @Test
    @DisplayName("parse() should extract text content correctly")
    void parse_ShouldExtractTextContent() throws IOException {
        MockMultipartFile textFile = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                TEST_CONTENT.getBytes()
        );

        String result = plainTextParser.parse(textFile);

        assertEquals(TEST_CONTENT, result);
    }

    @Test
    @DisplayName("parse() should handle empty file")
    void parse_ShouldHandleEmptyFile() throws IOException {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.txt",
                "text/plain",
                new byte[0]
        );

        String result = plainTextParser.parse(emptyFile);

        assertEquals("", result);
    }
}