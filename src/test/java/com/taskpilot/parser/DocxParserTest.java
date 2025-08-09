package com.taskpilot.parser;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class DocxParserTest {

    private final DocxParser docxParser = new DocxParser();

    @Test
    void parse_shouldExtractTextFromDocxFile() throws IOException {
        // 1. ARRANGE
        // Load the sample docx file from our test resources folder.
        var resource = new ClassPathResource("test-documents/sample.docx");
        var mockFile = new MockMultipartFile(
                "file",
                resource.getFilename(),
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                resource.getInputStream()
        );

        // 2. ACT
        String text = docxParser.parse(mockFile);

        // 3. ASSERT
        // We just check for a key phrase, not the exact entire text.
        assertThat(text).contains("This is a Word document.");
    }

    @Test
    void supports_shouldReturnTrueForDocxMimeType() {
        assertThat(docxParser.supports("application/vnd.openxmlformats-officedocument.wordprocessingml.document")).isTrue();
    }

    @Test
    void supports_shouldReturnFalseForNonPdfMimeTypes() {
        assertThat(docxParser.supports("application/pdf")).isFalse();
        assertThat(docxParser.supports("text/plain")).isFalse();
        assertThat(docxParser.supports(null)).isFalse();
    }
}