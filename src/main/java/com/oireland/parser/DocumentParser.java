package com.oireland.parser;

import com.oireland.exception.InvalidLLMResponseException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface DocumentParser {
    /**
     * Checks if this parser can handle the given file's MIME type.
     * @param mimeType The MIME type of the file (e.g., "text/plain").
     * @return true if the parser supports this type, false otherwise.
     */
    boolean supports(String mimeType);

    /**
     * Parses the given file and extracts its text content.
     * @param file The file to parse.
     * @return A string containing the plaintext content of the file.
     * @throws IOException if an error occurs during reading.
     */
    String parse(MultipartFile file) throws IOException, InvalidLLMResponseException;
}
