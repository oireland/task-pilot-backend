package com.taskpilot.parser;

import com.taskpilot.exception.InvalidLLMResponseException;
import com.taskpilot.prompt.PromptFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Objects;

@Component
public class EquationParser implements DocumentParser{

    private final ChatClient chatClient;
    private final PromptFactory promptFactory;

    public EquationParser(ChatClient.Builder builder, PromptFactory promptFactory) {
        this.chatClient = builder.build();
        this.promptFactory = promptFactory;
    }

    // Only want this to be used when specified, not based on supported mime type
    @Override
    public boolean supports(String mimeType) {
        return false;
    }

    @Override
    public String parse(MultipartFile file) throws IOException, InvalidLLMResponseException {
        ByteArrayResource fileResource = new ByteArrayResource(file.getBytes());

        String response = chatClient.prompt()
                .user(u -> u.text(promptFactory.pdfTextAndMathExtractor)
                        .media(MimeType.valueOf(Objects.requireNonNull(file.getContentType())), fileResource)).call().content();

        if (response == null || response.trim().isEmpty()) {
            throw new InvalidLLMResponseException("Received empty response from the LLM.");
        }

        return response;
    }
}
