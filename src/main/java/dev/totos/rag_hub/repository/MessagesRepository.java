package dev.totos.rag_hub.repository;

import dev.totos.rag_hub.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MessagesRepository extends JpaRepository<ChatMessage, UUID> {
}
