package com.taskpilot.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class PdfParserTest {

    private PdfParser pdfParser;

    @BeforeEach
    void setUp() {
        pdfParser = new PdfParser();
    }

    @Test
    @DisplayName("supports() should return true for PDF mime type")
    void supports_ShouldReturnTrueForPdfMimeType() {
        assertTrue(pdfParser.supports("application/pdf"));
    }

    @Test
    @DisplayName("supports() should return false for other mime types")
    void supports_ShouldReturnFalseForOtherMimeTypes() {
        assertFalse(pdfParser.supports("text/plain"));
        assertFalse(pdfParser.supports("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        assertFalse(pdfParser.supports(null));
    }

    @Test
    @DisplayName("parse() should throw IOException for invalid PDF file")
    void parse_ShouldThrowIOExceptionForInvalidFile() {
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "invalid pdf content".getBytes()
        );

        assertThrows(IOException.class, () -> pdfParser.parse(invalidFile));
    }
}