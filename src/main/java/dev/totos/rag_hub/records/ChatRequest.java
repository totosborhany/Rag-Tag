package dev.totos.rag_hub.records;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(

        @NotBlank(message = "message can't be empty")
        String message
) {
}
