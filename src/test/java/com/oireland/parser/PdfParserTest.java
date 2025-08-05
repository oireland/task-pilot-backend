package com.oireland.parser;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class PdfParserTest {

    private final PdfParser pdfParser = new PdfParser();

    @Test
    void parse_shouldExtractTextFromPdfFile() throws IOException {
        // ARRANGE
        // Load the sample .pdf file from our test resources folder.
        var resource = new ClassPathResource("test-documents/sample.pdf");
        var mockFile = new MockMultipartFile(
                "file",
                resource.getFilename(),
                "application/pdf", // The MIME type for PDF
                resource.getInputStream()
        );

        // ACT
        String text = pdfParser.parse(mockFile);

        // ASSERT
        // We check for a key phrase, as PDF parsing can sometimes include extra whitespace.
        assertThat(text).contains("This is a PDF document.");
    }

    @Test
    void supports_shouldReturnTrueForPdfMimeType() {
        assertThat(pdfParser.supports("application/pdf")).isTrue();
    }

    @Test
    void supports_shouldReturnFalseForNonPdfMimeTypes() {
        assertThat(pdfParser.supports("application/vnd.openxmlformats-officedocument.wordprocessingml.document")).isFalse();
        assertThat(pdfParser.supports("text/plain")).isFalse();
        assertThat(pdfParser.supports(null)).isFalse();
    }
}
