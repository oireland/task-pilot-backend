package com.taskpilot.exception;

public class FileTooLargeException extends RuntimeException {

    private final long fileSize;
    private final long maxFileSize;

    public FileTooLargeException(long fileSize, long maxFileSize, String message) {
        super(message);
        this.fileSize = fileSize;
        this.maxFileSize = maxFileSize;
    }

    public long getFileSize() {
        return fileSize;
    }

    public long getMaxFileSize() {
        return maxFileSize;
    }
}
