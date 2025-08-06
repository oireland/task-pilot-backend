package com.oireland.controller;

import com.oireland.exception.UnsupportedFileTypeException;
import com.oireland.service.DocumentProcessingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentProcessingService documentProcessingService;

    @Test
    void extractTasksFromFile_shouldReturnAccepted_whenFileIsValid() throws Exception {
        // ARRANGE
        var mockFile = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "Some file content".getBytes()
        );

        // We mock the service to do nothing when called.
        doNothing().when(documentProcessingService).processDocumentAndCreateTasks(any(MockMultipartFile.class));

        // ACT & ASSERT
        mockMvc.perform(multipart("/api/v1/tasks/extract").file(mockFile))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value("File received and processing started."));
    }

    @Test
    void extractTasksFromFile_shouldReturnBadRequest_whenFileIsEmpty() throws Exception {
        // ARRANGE
        var emptyFile = new MockMultipartFile(
                "file",
                "empty.txt",
                "text/plain",
                new byte[0]
        );

        // ACT & ASSERT
        mockMvc.perform(multipart("/api/v1/tasks/extract").file(emptyFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("File cannot be empty."));
    }

    @Test
    void extractTasksFromFile_shouldReturnUnsupportedMediaType_whenServiceThrows() throws Exception {
        // ARRANGE
        var unsupportedFile = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "image data".getBytes()
        );

        // Mock the service to throw the exception our global handler expects.
        doThrow(new UnsupportedFileTypeException("File type not supported: image/jpeg"))
                .when(documentProcessingService).processDocumentAndCreateTasks(any(MockMultipartFile.class));

        // ACT & ASSERT
        mockMvc.perform(multipart("/api/v1/tasks/extract").file(unsupportedFile))
                .andExpect(status().isUnsupportedMediaType());
        // A more specific test could also check the error message in the JSON body.
    }
}