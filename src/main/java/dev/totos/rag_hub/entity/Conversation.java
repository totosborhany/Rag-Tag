package dev.totos.rag_hub.entity;

import dev.totos.rag_hub.entity.ChatMessage;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.time.Instant;

@Entity
@Table(
        name = "conversations",
        indexes = {
                @Index(name = "idx_conversations_user_updated", columnList = "user_id, updated_at DESC")
        }
)
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "title", nullable = false,length = 255)
    private String title ;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(
            mappedBy = "conversation",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @OrderBy("createdAt ASC")
    private List<ChatMessage> messages = new ArrayList<>();


    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Helper method to synchronize bidirectional entity mapping
    public void addMessage(ChatMessage message) {
        messages.add(message);
        message.setConversation(this);
        this.updatedAt = Instant.now();
    }

    // Constructors, Getters, and Setters
    public Conversation() {}

    public Conversation(User user, String title) {
        this.user = user;
        this.title = title;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return getUserId(); }
    public void setUserId(User user) { this.user = user; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<ChatMessage> getMessages() { return messages; }
}
