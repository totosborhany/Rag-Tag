package dev.totos.rag_hub.service;

import dev.totos.rag_hub.entity.ChatMessage;
import dev.totos.rag_hub.entity.Conversation;
import dev.totos.rag_hub.entity.MessageRole;
import dev.totos.rag_hub.entity.User;
import dev.totos.rag_hub.exception.ApiException;
import dev.totos.rag_hub.records.ChatMessageDto;
import dev.totos.rag_hub.records.ConversationDto;
import dev.totos.rag_hub.records.cursorPageResponse;
import dev.totos.rag_hub.repository.ChatMessageRepository;
import dev.totos.rag_hub.repository.ConversationRepository;
import dev.totos.rag_hub.repository.UserRepository;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.data.domain.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ConversationService {
    private final ChatMessageRepository chatMessageRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final VectorStore vectorStore;
    private final RedisTemplate<String, String> redisTemplate;
    ConversationService(VectorStore vectorStore,RedisTemplate<String, String> redisTemplate,ConversationRepository conversationRepository, UserRepository userRepository,ChatMessageRepository chatMessageRepository){
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
        this.chatMessageRepository=chatMessageRepository;
    this.vectorStore=vectorStore;
    this.redisTemplate=redisTemplate;
    }
    @Transactional
    public ConversationDto createConversation(UUID userId, String title) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        Conversation conversation = new Conversation(user, title);
        Conversation savedConversation = conversationRepository.save(conversation);

        return new ConversationDto(
                savedConversation.getId(),
                savedConversation.getTitle(),
                savedConversation.getCreatedAt()
        );
    }

    public Page<ConversationDto> getConversationsForUser(UUID userId, Pageable pageable) {
        Page<Conversation> conversations = conversationRepository.findByUserId(userId, pageable);

        return conversations.map(c -> new ConversationDto(c.getId(), c.getTitle(), c.getCreatedAt()));
    }
    @Transactional
    public void deleteConversation(UUID userId, UUID conversationId) {
        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ApiException("Conversation not found or does not belong to user", HttpStatus.NOT_FOUND));

        conversationRepository.delete(conversation);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                String redisKey = "chat:history:" + conversationId;
                redisTemplate.delete(redisKey);

                FilterExpressionBuilder b = new FilterExpressionBuilder();
                vectorStore.delete(
                        b.eq("conversationId", conversationId.toString()).build()
                );
            }
        });
    }
    public cursorPageResponse getMessagesForConversation(
            UUID userId,
            UUID conversationId,
            Instant cursor,
            int limit
    ) {
        Pageable pageable = PageRequest.of(0, limit + 1);
        List<ChatMessage> rawMessages= null;
        if(cursor == null){
           rawMessages = chatMessageRepository.findByConversationIdAndUserId(
                    conversationId,
                    userId,
                    pageable
            );
        }else {
            rawMessages = chatMessageRepository.findByConversationIdAndUserIdAndCreatedAtBefore(
                    conversationId,
                    userId,
                    cursor,
                    pageable
            );
        }

        // 2. Check for next page
        boolean hasNext = rawMessages.size() > limit;
        List<ChatMessage> pageMessages = hasNext
                ? rawMessages.subList(0, limit)
                : rawMessages;

        // 3. Get cursor from the last message in the sliced page
        Instant nextCursor = hasNext
                ? pageMessages.get(pageMessages.size() - 1).getCreatedAt()
                : null;

        // 4. Map entities to DTOs
        List<ChatMessageDto> messageDtos = pageMessages.stream()
                .map(m -> new ChatMessageDto(
                        m.getId(),
                        m.getRole(),
                        m.getContent(),
                        m.getCreatedAt()
                ))
                .toList();

        return new cursorPageResponse(messageDtos, nextCursor, hasNext);
    }

    @Transactional
    public void deleteMessage(UUID userId, UUID conversationId, UUID messageId) {
        ChatMessage message = chatMessageRepository.findByIdAndConversationIdAndUserId(messageId, conversationId, userId);

        if (message == null) {
            throw new ApiException("Message not found or does not belong to user", HttpStatus.NOT_FOUND);
        }

        if (message.getRole() == MessageRole.ASSISTANT) {
            throw new ApiException("Sorry you cant delete an answer", HttpStatus.FORBIDDEN);
        }

        List<ChatMessage> toBeDeleted = new ArrayList<>();
        toBeDeleted.add(message);

        List<String> idsToDelete = new ArrayList<>();
        idsToDelete.add(message.getId().toString());

        List<String> redisEntriesToRemove = new ArrayList<>();
        redisEntriesToRemove.add("User: " + message.getContent());

        ChatMessage childMessage = chatMessageRepository.findByParentMessageId(messageId);
        if (childMessage != null) {
            toBeDeleted.add(childMessage);
            idsToDelete.add(childMessage.getId().toString());
            redisEntriesToRemove.add("Assistant: " + childMessage.getContent());
        }

        chatMessageRepository.deleteAll(toBeDeleted);

        String redisKey = "chat:history:" + conversationId;

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (String entry : redisEntriesToRemove) {
                    redisTemplate.opsForList().remove(redisKey, 1, entry);
                }

                try {
                    FilterExpressionBuilder b = new FilterExpressionBuilder();
                    vectorStore.delete(
                            b.and(
                                    b.eq("userId", userId.toString()),
                                    b.in("id", messageId.toString())
                            ).build()
                    );
                } catch (Exception e) {
                    System.out.println(e);
//                    log.error("Failed to delete vectors for message IDs {}: {}", idsToDelete, e.getMessage(), e);
                }
            }
        });
    }
    public ConversationDto getConversationForUser(UUID id, UUID userId) {
        return conversationRepository.findByIdAndUserId(id, userId)
                .map(c -> new ConversationDto(c.getId(), c.getTitle(), c.getCreatedAt()))
                .orElseThrow(() -> new ApiException("Conversation not found",HttpStatus.NOT_FOUND));
    }
}
