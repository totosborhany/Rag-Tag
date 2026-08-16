package dev.totos.rag_hub.records;

import dev.totos.rag_hub.entity.MessageRole;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageDto(
        UUID id,
        MessageRole role,
        String content,
        Instant createdAt
) {
}
