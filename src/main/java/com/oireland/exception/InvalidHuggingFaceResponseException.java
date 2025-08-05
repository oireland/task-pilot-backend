package com.oireland.exception;

public class InvalidHuggingFaceResponseException extends Exception {
    public InvalidHuggingFaceResponseException(String message) {
        super(message);
    }


    public InvalidHuggingFaceResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}
