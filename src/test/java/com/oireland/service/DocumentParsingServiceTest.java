package com.oireland.service;

import com.oireland.exception.InvalidLLMResponseException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class DocumentParsingServiceTest {

    @Autowired
    private DocumentParsingService parsingService;

    @Test
    void parseDocument_shouldRouteToCorrectParser_forDocx() throws IOException, InvalidLLMResponseException {
        var resource = new ClassPathResource("test-documents/sample.docx");
        var mockFile = new MockMultipartFile(
                "file",
                resource.getFilename(),
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                resource.getInputStream()
        );

        // We don't care about the result, just that it doesn't throw an error for a supported type
        // A more advanced test could use Mockito to verify the correct parser was called.
        String result = parsingService.parseDocument(mockFile, false);
        assertThat(result).isEqualTo("This is a Word document.\n");
    }

    @Test
    void parseDocument_shouldThrowException_forUnsupportedType() {
        var mockFile = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg", // An unsupported MIME type
                "Some image data".getBytes()
        );

        // Assert that our specific exception is thrown
        assertThatThrownBy(() -> parsingService.parseDocument(mockFile, false))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("File type not supported: image/jpeg");
    }
}