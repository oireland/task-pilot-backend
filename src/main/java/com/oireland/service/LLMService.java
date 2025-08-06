package com.oireland.service;

import com.oireland.exception.InvalidLLMResponseException;

public interface LLMService {
    /**
     * Generates a response using the specified model and prompt.
     *
     * @param prompt  The prompt to generate a response for
     * @param responseType The DTO class type to deserialize the response into.
     * @return The generated response as an instance of the DTO.
     */
    <T> T executePrompt(String prompt, Class<T> responseType) throws InvalidLLMResponseException;

}
