package com.taskpilot.service;

import com.taskpilot.exception.InvalidLLMResponseException;
import com.taskpilot.exception.UnsupportedFileTypeException;
import com.taskpilot.parser.DocumentParser;
import com.taskpilot.parser.EquationParser;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class DocumentParsingService {
    private final List<DocumentParser> parsers;

    // Spring automatically injects all beans that implement the DocumentParser interface.
    public DocumentParsingService(List<DocumentParser> parsers) {
        this.parsers = parsers;

    }

    public String parseDocument(MultipartFile file, boolean hasEquations) throws IOException, InvalidLLMResponseException {
        if (hasEquations) {
            DocumentParser parser = parsers.stream().filter(p -> p.getClass().equals(EquationParser.class)).findFirst().orElseThrow(() -> new RuntimeException("EquationParser component was not found"));
            return parser.parse(file);

        }

        String mimeType = file.getContentType();
        DocumentParser appropriateParser = parsers.stream()
                .filter(p -> p.supports(mimeType))
                .findFirst()
                .orElseThrow(() -> new UnsupportedFileTypeException("File type not supported: " + mimeType));

        return appropriateParser.parse(file);
    }

}
