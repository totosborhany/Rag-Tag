package dev.totos.rag_hub.records;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;

public record CreateConversationRequest(
        @NotBlank(message = "Password is required")
        String title

) {
}
