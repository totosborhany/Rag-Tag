package dev.totos.rag_hub.controllers;

import dev.totos.rag_hub.records.*;
import dev.totos.rag_hub.service.ChatService;
import dev.totos.rag_hub.service.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.security.Principal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/conversations")
public class ConversationController {

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final ConversationService conversationService;
    private  final ChatService chatService;
    public ConversationController(ConversationService conversationService,ChatService chatService) {
        this.conversationService = conversationService;

            this.chatService=chatService;
    }
    @Operation(
            summary = "Creates conversation"
    )
    @PostMapping
    public ResponseEntity<ConversationDto> createConversation(
             Principal principal,
            @RequestBody CreateConversationRequest request) {

        UUID userId = UUID.fromString(principal.getName());
        ConversationDto conversation = conversationService.createConversation(userId, request.title());
        return ResponseEntity.status(HttpStatus.CREATED).body(conversation);
    }
    @Operation(
            summary = "Get all my conversations"
    )
    @GetMapping
    public ResponseEntity<Map<String, Object>> getUserConversations(
            Principal principal,
            @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        UUID userId = UUID.fromString(principal.getName());

        Page<ConversationDto> conversationPage = conversationService.getConversationsForUser(userId, pageable);

        Map<String, Object> response = Map.of(
                "conversations", conversationPage.getContent(),
                "currentPage", conversationPage.getNumber(),
                "totalItems", conversationPage.getTotalElements(),
                "totalPages", conversationPage.getTotalPages(),
                "pageSize", conversationPage.getSize()
        );

        return ResponseEntity.ok(response);
    }
    @Operation(
            summary = "Get single conversation"
    )
    @GetMapping("/{id}")
    public ResponseEntity<ConversationDto> getConversationById(
            @PathVariable UUID id,
            Principal principal
    ) {
        UUID userId = UUID.fromString(principal.getName());
        ConversationDto conversation = conversationService.getConversationForUser(id, userId);
        return ResponseEntity.ok(conversation);
    }
    @Operation(
            summary = "delete single conversation"
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConversation(
             Principal principal,
            @PathVariable UUID id) {

        UUID userId = UUID.fromString(principal.getName());
        conversationService.deleteConversation(userId, id);

        return ResponseEntity.noContent().build();
    }
    @Operation(
            summary = "Get conversation messages"
    )
    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<cursorPageResponse> getConversationMessages(
             Principal principal,
            @PathVariable UUID conversationId,
            @RequestParam(required = false) Instant cursor,
            @RequestParam(defaultValue = "20") int limit) {

        UUID userId = UUID.fromString(principal.getName());

        cursorPageResponse response = conversationService.getMessagesForConversation(
                userId, conversationId, cursor, limit);

        return ResponseEntity.ok(response);
    }
    @Operation(
            summary = "Send prompt to llm"
    )
    @PostMapping(path = "/{conversationId}/messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Object>> sendChatMessage(
            Principal principal,
            @PathVariable UUID conversationId,
            @Valid @RequestBody ChatRequest request
    ) {
        UUID userId = UUID.fromString(principal.getName());

        return chatService.processIntialMessage(userId, request.message(), conversationId)
                .doOnCancel(() -> log.debug("Client closed the connection / cancelled SSE stream for user {}", userId))
                .doOnError(ex -> log.debug("Error during SSE stream: {}", ex.getMessage()))
                .doOnComplete(() -> log.debug("SSE stream completed successfully"));
    }
    @Operation(
            summary = "Delete message"
    )
    @DeleteMapping("/{conversationId}/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(
             Principal userIdStr,
            @PathVariable UUID conversationId,
            @PathVariable UUID messageId) {

        UUID userId = UUID.fromString(userIdStr.getName());
        conversationService.deleteMessage(userId, conversationId, messageId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Delete my cached questions and answers"
    )
    @DeleteMapping("/cache")
    public ResponseEntity<Void> deleteConversationCache(
            Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        chatService.deleteMycachedAnswers(userId);
        return ResponseEntity.noContent().build();
    }

}
