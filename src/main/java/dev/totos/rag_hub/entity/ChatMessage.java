package dev.totos.rag_hub.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "chat_messages",
        indexes = {
                @Index(name = "idx_messages_conversation_created", columnList = "conversation_id, created_at ASC")
        }
)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_role", nullable = false, length = 20)
    private MessageRole role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    @Column(name = "parent_message_id")
    private UUID parentMessageId;;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    // Constructors, Getters, and Setters
    public ChatMessage() {}

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getParentMessageId() {
        return parentMessageId;
    }

    public void setParentMessageId(UUID parentMessageId) {
        this.parentMessageId = parentMessageId;
    }

    public ChatMessage(MessageRole role, String content, Integer tokenUsed, Conversation conversation) {
        this.role = role;
        this.content = content;
        this.conversation=conversation;
    }

    public UUID getId() { return id; }
    public Conversation getConversation() { return conversation; }
    public void setConversation(Conversation conversation) { this.conversation = conversation; }
    public MessageRole getRole() { return role; }
    public void setRole(MessageRole role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Instant getCreatedAt() { return createdAt; }
}