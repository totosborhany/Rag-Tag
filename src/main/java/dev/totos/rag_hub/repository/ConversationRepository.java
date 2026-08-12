package dev.totos.rag_hub.repository;

import dev.totos.rag_hub.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

}
