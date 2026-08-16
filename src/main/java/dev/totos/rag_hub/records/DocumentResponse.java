package dev.totos.rag_hub.records;

import dev.totos.rag_hub.entity.DocumentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        String fileName,
        LocalDateTime uploadedAt,
        String fileType,
        Long fileSize,
        DocumentStatus status
) {
}
