package com.oireland.service;

import com.oireland.exception.UnsupportedFileTypeException;
import com.oireland.parser.DocumentParser;
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

    public String parseDocument(MultipartFile file) throws IOException {
        String mimeType = file.getContentType();
        DocumentParser appropriateParser = parsers.stream()
                .filter(p -> p.supports(mimeType))
                .findFirst()
                .orElseThrow(() -> new UnsupportedFileTypeException("File type not supported: " + mimeType));

        return appropriateParser.parse(file);
    }
}
