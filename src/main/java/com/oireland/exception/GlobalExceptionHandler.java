package com.oireland.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.IOException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(UnsupportedFileTypeException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public ErrorInfo handleUnsupportedFileType(HttpServletRequest request, Exception ex) {
        logger.warn("Attempted to upload an unsupported file type. Message: {}", ex.getMessage());
        return new ErrorInfo(request, ex);
    }

    // Handles errors from corrupt files or general I/O problems during parsing
    @ExceptionHandler(IOException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR) // Sets the HTTP status to 500
    public ErrorInfo handleFileParsingError(HttpServletRequest request, Exception ex) {
        logger.error("A file could not be read or parsed correctly.", ex);
        return new ErrorInfo(request, ex);
    }

    @ExceptionHandler(InvalidHuggingFaceResponseException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorInfo handleInvalidHuggingFaceResponse(HttpServletRequest request, Exception ex) {
        logger.error("Received empty or invalid response from Hugging Face API.");
        return new ErrorInfo(request, ex);
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(JsonProcessingException.class)
    public ErrorInfo handleJsonProcessingException(HttpServletRequest request, Exception ex) {
        logger.error("Failed to parse Json");
        return new ErrorInfo(request, ex);
    }

    @ExceptionHandler(Exception.class)
    public ErrorInfo handleException(HttpServletRequest request, Exception ex) {
        return new ErrorInfo(request, ex);
    }
}
