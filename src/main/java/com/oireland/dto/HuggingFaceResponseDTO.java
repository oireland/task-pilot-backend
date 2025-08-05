package com.oireland.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the structure of the object returned within the array from the
 * Hugging Face Inference API.
 */
public record HuggingFaceResponseDTO(
        @JsonProperty("generated_text") String generatedText
) {
}
