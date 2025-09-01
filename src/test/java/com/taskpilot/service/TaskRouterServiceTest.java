// src/test/java/com/taskpilot/service/TaskRouterServiceTest.java
package com.taskpilot.service;

import com.taskpilot.dto.task.ExtractedTaskListDTO;
import com.taskpilot.exception.InvalidLLMResponseException;
import com.taskpilot.prompt.PromptFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskRouterServiceTest {

    @Mock
    private LLMService llmService;

    private PromptFactory promptFactory;
    private TaskRouterService taskRouterService;

    private static final ExtractedTaskListDTO MOCK_RESPONSE = new ExtractedTaskListDTO(
            "Test Title",
            "Test Description",
            Arrays.asList("Task 1", "Task 2")
    );

    @BeforeEach
    void setUp() {
        // Use real PromptFactory since fields are final
        promptFactory = new PromptFactory();
        taskRouterService = new TaskRouterService(llmService, promptFactory);
    }

    @Test
    @DisplayName("processDocument() should use exercise prompt when 'Exercise' pattern is found")
    void processDocument_ShouldUseExercisePrompt_WhenExercisePatternFound() throws InvalidLLMResponseException {
        // ARRANGE
        String documentWithExercise = "This document contains Exercise 1 and some other content.";
        String expectedPrompt = String.format(promptFactory.exercisePatternPromptTemplate, documentWithExercise);

        when(llmService.executePrompt(expectedPrompt, ExtractedTaskListDTO.class)).thenReturn(MOCK_RESPONSE);

        // ACT
        ExtractedTaskListDTO result = taskRouterService.processDocument(documentWithExercise);

        // ASSERT
        assertSame(MOCK_RESPONSE, result);
        verify(llmService).executePrompt(expectedPrompt, ExtractedTaskListDTO.class);
        verifyNoMoreInteractions(llmService);
    }

    @Test
    @DisplayName("processDocument() should use exercise prompt for case-insensitive 'exercise' pattern")
    void processDocument_ShouldUseExercisePrompt_WhenExercisePatternFoundCaseInsensitive() throws InvalidLLMResponseException {
        // ARRANGE
        String documentWithExercise = "Please complete exercise 2.1 below.";
        String expectedPrompt = String.format(promptFactory.exercisePatternPromptTemplate, documentWithExercise);

        when(llmService.executePrompt(expectedPrompt, ExtractedTaskListDTO.class)).thenReturn(MOCK_RESPONSE);

        // ACT
        ExtractedTaskListDTO result = taskRouterService.processDocument(documentWithExercise);

        // ASSERT
        assertSame(MOCK_RESPONSE, result);
        verify(llmService).executePrompt(expectedPrompt, ExtractedTaskListDTO.class);
    }

    @Test
    @DisplayName("processDocument() should use exercise prompt for 'EXERCISE' pattern")
    void processDocument_ShouldUseExercisePrompt_WhenExercisePatternFoundUpperCase() throws InvalidLLMResponseException {
        // ARRANGE
        String documentWithExercise = "Complete EXERCISE 3 for homework.";
        String expectedPrompt = String.format(promptFactory.exercisePatternPromptTemplate, documentWithExercise);

        when(llmService.executePrompt(expectedPrompt, ExtractedTaskListDTO.class)).thenReturn(MOCK_RESPONSE);

        // ACT
        ExtractedTaskListDTO result = taskRouterService.processDocument(documentWithExercise);

        // ASSERT
        assertSame(MOCK_RESPONSE, result);
        verify(llmService).executePrompt(expectedPrompt, ExtractedTaskListDTO.class);
    }

    @Test
    @DisplayName("processDocument() should use exercise prompt for decimal exercise numbers")
    void processDocument_ShouldUseExercisePrompt_WhenExercisePatternFoundWithDecimal() throws InvalidLLMResponseException {
        // ARRANGE
        String documentWithExercise = "This is Exercise 4.2.1 from the textbook.";
        String expectedPrompt = String.format(promptFactory.exercisePatternPromptTemplate, documentWithExercise);

        when(llmService.executePrompt(expectedPrompt, ExtractedTaskListDTO.class)).thenReturn(MOCK_RESPONSE);

        // ACT
        ExtractedTaskListDTO result = taskRouterService.processDocument(documentWithExercise);

        // ASSERT
        assertSame(MOCK_RESPONSE, result);
        verify(llmService).executePrompt(expectedPrompt, ExtractedTaskListDTO.class);
    }

    @Test
    @DisplayName("processDocument() should use general prompt when no 'Exercise' pattern is found")
    void processDocument_ShouldUseGeneralPrompt_WhenNoExercisePatternFound() throws InvalidLLMResponseException {
        // ARRANGE
        String documentWithoutExercise = "This is a general document with tasks but no exercises.";
        String expectedPrompt = String.format(promptFactory.generalTaskPromptTemplate, documentWithoutExercise);

        when(llmService.executePrompt(expectedPrompt, ExtractedTaskListDTO.class)).thenReturn(MOCK_RESPONSE);

        // ACT
        ExtractedTaskListDTO result = taskRouterService.processDocument(documentWithoutExercise);

        // ASSERT
        assertSame(MOCK_RESPONSE, result);
        verify(llmService).executePrompt(expectedPrompt, ExtractedTaskListDTO.class);
        verifyNoMoreInteractions(llmService);
    }

    @Test
    @DisplayName("processDocument() should use general prompt when 'exercise' appears without number")
    void processDocument_ShouldUseGeneralPrompt_WhenExerciseWithoutNumber() throws InvalidLLMResponseException {
        // ARRANGE
        String documentWithExercise = "This document mentions exercise routines but no numbered exercises.";
        String expectedPrompt = String.format(promptFactory.generalTaskPromptTemplate, documentWithExercise);

        when(llmService.executePrompt(expectedPrompt, ExtractedTaskListDTO.class)).thenReturn(MOCK_RESPONSE);

        // ACT
        ExtractedTaskListDTO result = taskRouterService.processDocument(documentWithExercise);

        // ASSERT
        assertSame(MOCK_RESPONSE, result);
        verify(llmService).executePrompt(expectedPrompt, ExtractedTaskListDTO.class);
    }

    @Test
    @DisplayName("processDocument() should propagate InvalidLLMResponseException from LLMService")
    void processDocument_ShouldPropagateException_WhenLLMServiceThrows() throws InvalidLLMResponseException {
        // ARRANGE
        String testDocument = "Sample document text";
        String expectedPrompt = String.format(promptFactory.generalTaskPromptTemplate, testDocument);
        InvalidLLMResponseException expectedException = new InvalidLLMResponseException("LLM error");

        when(llmService.executePrompt(expectedPrompt, ExtractedTaskListDTO.class)).thenThrow(expectedException);

        // ACT & ASSERT
        InvalidLLMResponseException exception = assertThrows(
                InvalidLLMResponseException.class,
                () -> taskRouterService.processDocument(testDocument)
        );

        assertSame(expectedException, exception);
        verify(llmService).executePrompt(expectedPrompt, ExtractedTaskListDTO.class);
    }

    @Test
    @DisplayName("processDocument() should handle null document text")
    void processDocument_ShouldHandleNullDocumentText() throws InvalidLLMResponseException {
        // ARRANGE
        // No stubbing needed since the method returns early for null input

        // ACT
        ExtractedTaskListDTO result = taskRouterService.processDocument(null);

        // ASSERT
        assertNull(result);
        verifyNoInteractions(llmService);
    }

    @Test
    @DisplayName("processDocument() should handle empty document text")
    void processDocument_ShouldHandleEmptyDocumentText() throws InvalidLLMResponseException {
        // ARRANGE
        String emptyDocument = "";
        String expectedPrompt = String.format(promptFactory.generalTaskPromptTemplate, emptyDocument);

        when(llmService.executePrompt(expectedPrompt, ExtractedTaskListDTO.class)).thenReturn(MOCK_RESPONSE);

        // ACT
        ExtractedTaskListDTO result = taskRouterService.processDocument(emptyDocument);

        // ASSERT
        assertSame(MOCK_RESPONSE, result);
        verify(llmService).executePrompt(expectedPrompt, ExtractedTaskListDTO.class);
    }

    @Test
    @DisplayName("processDocument() should chunk large document and combine results")
    void processDocument_ShouldChunkLargeDocumentAndCombineResults() throws InvalidLLMResponseException {
        // ARRANGE
        String largeDocument = "Paragraph 1\n\nParagraph 2\n\n" + "Paragraph 3".repeat(1000);
        ExtractedTaskListDTO response1 = new ExtractedTaskListDTO("Title 1", "Desc 1", List.of("Task 1"));
        ExtractedTaskListDTO response2 = new ExtractedTaskListDTO("Title 2", "Desc 2", List.of("Task 2"));

        when(llmService.executePrompt(anyString(), eq(ExtractedTaskListDTO.class)))
                .thenReturn(response1, response2);
        when(llmService.executePrompt(contains("Summarise"), eq(String.class))).thenReturn("Final Description");

        // ACT
        ExtractedTaskListDTO result = taskRouterService.processDocument(largeDocument);

        // ASSERT
        assertEquals("Title 1", result.title());
        assertEquals("Final Description", result.description());
        assertEquals(2, result.todos().size());
        assertTrue(result.todos().contains("Task 1"));
        assertTrue(result.todos().contains("Task 2"));
        verify(llmService, times(3)).executePrompt(anyString(), any());
    }

    @Test
    @DisplayName("processDocument() should handle a single chunk")
    void processDocument_ShouldHandleSingleChunk() throws InvalidLLMResponseException {
        // ARRANGE
        String document = "This is a short document.";
        when(llmService.executePrompt(anyString(), eq(ExtractedTaskListDTO.class))).thenReturn(MOCK_RESPONSE);

        // ACT
        ExtractedTaskListDTO result = taskRouterService.processDocument(document);

        // ASSERT
        assertSame(MOCK_RESPONSE, result);
        verify(llmService, times(1)).executePrompt(anyString(), eq(ExtractedTaskListDTO.class));
    }
}