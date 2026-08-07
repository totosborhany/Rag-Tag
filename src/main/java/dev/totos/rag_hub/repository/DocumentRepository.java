package dev.totos.rag_hub.repository;

import dev.totos.rag_hub.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
    long deleteByUserId(UUID userId);
}
