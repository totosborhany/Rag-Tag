package dev.totos.rag_hub.records;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(

        @NotBlank(message = "message can't be empty")
        @Size(max = 500, message = "message can't be longer than 500 characters")
        String message
) {
}
