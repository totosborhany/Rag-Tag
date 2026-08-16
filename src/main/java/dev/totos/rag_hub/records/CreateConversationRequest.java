package dev.totos.rag_hub.records;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;

public record CreateConversationRequest(
        @NotBlank(message = "title is required")
        String title

) {
}
