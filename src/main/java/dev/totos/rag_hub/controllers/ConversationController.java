package dev.totos.rag_hub.controllers;

import dev.totos.rag_hub.records.ConversationDto;
import dev.totos.rag_hub.records.CreateConversationRequest;
import dev.totos.rag_hub.service.ConversationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/conversations")
public class ConversationController {
    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    // 1. Create Conversation (Mandatory before chatting)
    @PostMapping
    public ResponseEntity<ConversationDto> createConversation(
            @AuthenticationPrincipal String userIdStr,
            @RequestBody CreateConversationRequest request) {

        UUID userId = UUID.fromString(userIdStr);
        ConversationDto conversation = conversationService.createConversation(userId, request.title());
        return ResponseEntity.status(HttpStatus.CREATED).body(conversation);
    }

    // 2. Get All Conversations for User Sidebar
    @GetMapping
    public ResponseEntity<List<ConversationDto>> getUserConversations(
            @AuthenticationPrincipal String userIdStr) {

        UUID userId = UUID.fromString(userIdStr);
        return ResponseEntity.ok(conversationService.getConversationsForUser(userId));
    }

    // 3. Delete Conversation
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConversation(
            @AuthenticationPrincipal String userIdStr,
            @PathVariable UUID id) {

        UUID userId = UUID.fromString(userIdStr);
        conversationService.deleteConversation(userId, id);
        return ResponseEntity.noContent().build();
    }
}
