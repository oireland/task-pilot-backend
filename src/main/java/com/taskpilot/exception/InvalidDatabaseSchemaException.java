package com.taskpilot.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.CONFLICT, reason = "The selected Notion database has an invalid schema.")
public class InvalidDatabaseSchemaException extends RuntimeException {
    public InvalidDatabaseSchemaException(String message) {
        super(message);
    }
}