package com.taskpilot.parser;

import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;

class DocxParserTest {

    private DocxParser docxParser;

    @BeforeEach
    void setUp() {
        docxParser = new DocxParser();
    }

    @Test
    @DisplayName("supports() should return true for DOCX mime type")
    void supports_ShouldReturnTrueForDocxMimeType() {
        assertTrue(docxParser.supports("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
    }

    @Test
    @DisplayName("supports() should return false for other mime types")
    void supports_ShouldReturnFalseForOtherMimeTypes() {
        assertFalse(docxParser.supports("application/pdf"));
        assertFalse(docxParser.supports("text/plain"));
        assertFalse(docxParser.supports(null));
    }

    @Test
    @DisplayName("parse() should throw IOException for invalid DOCX file")
    void parse_ShouldThrowIOExceptionForInvalidFile() {
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file",
                "test.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "invalid docx content".getBytes()
        );

        assertThrows(NotOfficeXmlFileException.class, () -> docxParser.parse(invalidFile));
    }
}