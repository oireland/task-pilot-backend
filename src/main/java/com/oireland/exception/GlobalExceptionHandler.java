package com.oireland.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InvalidHuggingFaceResponseException.class)
    public ErrorInfo handleInvalidHuggingFaceResponse(HttpServletRequest request, Exception ex) {
        logger.error("Received empty or invalid response from Hugging Face API.");
        return new ErrorInfo(request, ex);
    }

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
