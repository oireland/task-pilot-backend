package com.oireland.parser;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class PlainTextParser implements DocumentParser {
    @Override
    public boolean supports(String mimeType) {
        return mimeType != null && mimeType.startsWith("text/");
    }

    @Override
    public String parse(MultipartFile file) throws IOException {
        return new String(file.getBytes(), StandardCharsets.UTF_8);
    }
}