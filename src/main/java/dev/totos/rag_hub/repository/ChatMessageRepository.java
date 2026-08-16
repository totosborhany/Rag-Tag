package dev.totos.rag_hub.repository;

import dev.totos.rag_hub.entity.ChatMessage;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {


    @Query("""
    SELECT m FROM ChatMessage m
    WHERE m.conversation.id = :conversationId
      AND m.conversation.user.id = :userId
      AND (:createdAt IS NULL OR m.createdAt < :createdAt)
    ORDER BY m.createdAt DESC
""")
    List<ChatMessage> findMessages(
            @Param("conversationId") UUID conversationId,
            @Param("userId") UUID userId,
            @Param("createdAt") LocalDateTime createdAt,
            Pageable pageable
    );

    @Query("""
    SELECT m FROM ChatMessage m
    WHERE m.id = :messageId
      AND m.conversation.id = :conversationId
      AND m.conversation.user.id = :userId
""")
    ChatMessage findByIdAndConversationIdAndUserId(
            @Param("messageId") UUID messageId,
            @Param("conversationId") UUID conversationId,
            @Param("userId") UUID userId
    );
    @Query("""
        SELECT cm FROM ChatMessage cm 
        JOIN cm.conversation c 
        WHERE cm.conversation.id = :conversationId 
          AND c.user.id = :userId 
          AND cm.createdAt < :before 
        ORDER BY cm.createdAt DESC
    """)
    List<ChatMessage> findByConversationIdAndUserIdAndCreatedAtBefore(
            @Param("conversationId") UUID conversationId,
            @Param("userId") UUID userId,
            @Param("before") Instant before,
            Pageable pageable
    );

    @Query("""
        SELECT cm FROM ChatMessage cm 
        JOIN cm.conversation c 
        WHERE cm.conversation.id = :conversationId 
          AND c.user.id = :userId 
        ORDER BY cm.createdAt DESC
    """)
    List<ChatMessage> findByConversationIdAndUserId(
            @Param("conversationId") UUID conversationId,
            @Param("userId") UUID userId,
            Pageable pageable
    );

    ChatMessage findByParentMessageId(UUID parentMessageId);
}
