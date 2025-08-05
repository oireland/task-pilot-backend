package com.oireland.parser;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@Component
public class PdfParser implements DocumentParser {
    @Override
    public boolean supports(String mimeType) {
        return "application/pdf".equals(mimeType);
    }

    @Override
    public String parse(MultipartFile file) throws IOException {
        // We use a try-with-resources block to ensure the document is always closed.
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
}