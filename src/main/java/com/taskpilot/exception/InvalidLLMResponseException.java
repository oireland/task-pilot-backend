package com.taskpilot.exception;

public class InvalidLLMResponseException extends Exception {
    public InvalidLLMResponseException(String message) {
        super(message);
    }


    public InvalidLLMResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}
