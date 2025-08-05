package com.oireland.parser;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class PlainTextParserTest {

    private final PlainTextParser plainTextParser = new PlainTextParser();

    @Test
    void parse_shouldExtractFromPlainText() throws IOException {
        // 1. ARRANGE
        // Load the sample docx file from our test resources folder.
        var resource = new ClassPathResource("test-documents/sample.txt");
        var mockFile = new MockMultipartFile(
                "file",
                resource.getFilename(),
                "text/plain",
                resource.getInputStream()
        );

        // 2. ACT
        String text = plainTextParser.parse(mockFile);

        // 3. ASSERT
        // We just check for a key phrase, not the exact entire text.
        assertThat(text).contains("This is a plaintext file.");

    }

    @Test
    void supports_shouldReturnTrueForTextMimeTypes() {
        assertThat(plainTextParser.supports("text/plain")).isTrue();
        assertThat(plainTextParser.supports("text/markdown")).isTrue();
        assertThat(plainTextParser.supports("text/csv")).isTrue();
    }

    @Test
    void supports_shouldReturnFalseForNonTextMimeTypes() {
        assertThat(plainTextParser.supports("application/pdf")).isFalse();
        assertThat(plainTextParser.supports("image/jpeg")).isFalse();
        assertThat(plainTextParser.supports(null)).isFalse();
    }
}
