package com.taskpilot.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.IOException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(UnsupportedFileTypeException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public ResponseEntity<ErrorResponse> handleUnsupportedFileType(Exception ex) {
        logger.warn("Attempted to upload an unsupported file type. Message: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(ErrorResponse.create(ex, HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported file type. Please upload a valid document format such as PDF, DOCX, or TXT."));
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<ErrorResponse> handleBadCredentials(Exception ex) {
        logger.warn("Attempted to login with invalid credentials. Message: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse.create(ex, HttpStatus.UNAUTHORIZED, "Invalid Email and Password"));
    }

    // Handles errors from corrupt files or general I/O problems during parsing
    @ExceptionHandler(IOException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR) // Sets the HTTP status to 500
    public ResponseEntity<ErrorResponse> handleFileParsingError(Exception ex) {
        logger.error("A file could not be read or parsed correctly.", ex);
        return ResponseEntity.internalServerError()
                .body(ErrorResponse.create(ex, HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred while reading or parsing the file. Please ensure the file is not corrupted and try again."));
    }

    @ExceptionHandler(InvalidLLMResponseException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleInvalidHuggingFaceResponse(Exception ex) {
        logger.error("Received empty or invalid response from Hugging Face API.");
        return ResponseEntity.internalServerError()
                .body(ErrorResponse.create(ex, HttpStatus.INTERNAL_SERVER_ERROR, "Received an empty or invalid response from the Hugging Face API. Please check the API configuration and try again."));
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(JsonProcessingException.class)
    public ResponseEntity<ErrorResponse> handleJsonProcessingException(Exception ex) {
        logger.error("Failed to parse Json");
        return ResponseEntity.internalServerError()
                .body(ErrorResponse.create(ex, HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred while processing JSON data. Please check the input format and try again."));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR) // Sets the HTTP status to 500 for all other exceptions
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        return ResponseEntity.internalServerError()
                .body(ErrorResponse.create(ex, HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred. Please try again later."));
    }
}
