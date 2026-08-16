package dev.totos.rag_hub.records;

import java.time.Instant;
import java.util.UUID;

public record ConversationDto(
        UUID id,
        String title,
        Instant createdAt) {
}
