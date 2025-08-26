package com.taskpilot.service;

import com.taskpilot.exception.InvalidLLMResponseException;
import com.taskpilot.exception.UnsupportedFileTypeException;
import com.taskpilot.parser.DocumentParser;
import com.taskpilot.parser.EquationParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentParsingServiceTest {

    @Mock
    private DocumentParser pdfParser;

    @Mock
    private DocumentParser plainTextParser;

    @Mock
    private EquationParser equationParser;

    private DocumentParsingService documentParsingService;
    private static final String PARSED_CONTENT = "Parsed document content";

    @BeforeEach
    void setUp() {
        List<DocumentParser> parsers = Arrays.asList(pdfParser, plainTextParser, equationParser);
        documentParsingService = new DocumentParsingService(parsers);
    }

    @Test
    @DisplayName("parseDocument() should use EquationParser when hasEquations is true")
    void parseDocument_ShouldUseEquationParserWhenHasEquations() throws IOException, InvalidLLMResponseException {
        // ARRANGE
        MockMultipartFile testFile = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "test content".getBytes()
        );

        when(equationParser.parse(testFile)).thenReturn(PARSED_CONTENT);

        // ACT
        String result = documentParsingService.parseDocument(testFile, true);

        // ASSERT
        assertEquals(PARSED_CONTENT, result);
        verify(equationParser).parse(testFile);
        verifyNoInteractions(pdfParser, plainTextParser);
    }

    @Test
    @DisplayName("parseDocument() should find appropriate parser by mime type")
    void parseDocument_ShouldFindParserByMimeType() throws IOException, InvalidLLMResponseException {
        // ARRANGE
        MockMultipartFile testFile = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "test content".getBytes()
        );

        when(pdfParser.supports("application/pdf")).thenReturn(true);
        when(pdfParser.parse(testFile)).thenReturn(PARSED_CONTENT);

        // ACT
        String result = documentParsingService.parseDocument(testFile, false);

        // ASSERT
        assertEquals(PARSED_CONTENT, result);
        verify(pdfParser).supports("application/pdf");
        verify(pdfParser).parse(testFile);

        // Only verify that plainTextParser.supports() was NOT called,
        // since the service found a matching parser already
        verifyNoInteractions(plainTextParser);
    }

    @Test
    @DisplayName("parseDocument() should throw UnsupportedFileTypeException for unsupported mime type")
    void parseDocument_ShouldThrowExceptionForUnsupportedType() {
        // ARRANGE
        MockMultipartFile testFile = new MockMultipartFile(
                "file",
                "test.xyz",
                "application/unknown",
                "test content".getBytes()
        );

        when(pdfParser.supports("application/unknown")).thenReturn(false);
        when(plainTextParser.supports("application/unknown")).thenReturn(false);

        // ACT & ASSERT
        UnsupportedFileTypeException exception = assertThrows(
                UnsupportedFileTypeException.class,
                () -> documentParsingService.parseDocument(testFile, false)
        );

        assertEquals("File type not supported: application/unknown", exception.getMessage());
    }

    @Test
    @DisplayName("parseDocument() should propagate IOException from parser")
    void parseDocument_ShouldPropagateIOException() throws IOException, InvalidLLMResponseException {
        // ARRANGE
        MockMultipartFile testFile = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "test content".getBytes()
        );

        IOException testException = new IOException("Test IO error");
        when(plainTextParser.supports("text/plain")).thenReturn(true);
        when(plainTextParser.parse(testFile)).thenThrow(testException);

        // ACT & ASSERT
        IOException exception = assertThrows(
                IOException.class,
                () -> documentParsingService.parseDocument(testFile, false)
        );

        assertEquals(testException, exception);
    }

    @Test
    @DisplayName("parseDocument() should propagate InvalidLLMResponseException from EquationParser")
    void parseDocument_ShouldPropagateInvalidLLMResponseException() throws IOException, InvalidLLMResponseException {
        // ARRANGE
        MockMultipartFile testFile = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "test content".getBytes()
        );

        InvalidLLMResponseException testException = new InvalidLLMResponseException("LLM error");
        when(equationParser.parse(testFile)).thenThrow(testException);

        // ACT & ASSERT
        InvalidLLMResponseException exception = assertThrows(
                InvalidLLMResponseException.class,
                () -> documentParsingService.parseDocument(testFile, true)
        );

        assertEquals(testException, exception);
    }
}