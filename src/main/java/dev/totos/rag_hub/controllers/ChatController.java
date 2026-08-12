package dev.totos.rag_hub.controllers;

import dev.totos.rag_hub.records.ChatRequest;
import dev.totos.rag_hub.records.ChatResponse;
import dev.totos.rag_hub.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api/v1/chats")
public class ChatController {

    private  final ChatService chatService;
    ChatController(ChatService chatService){
        this.chatService=chatService;
    }

    @PostMapping
    Flux<ChatResponse> SendChatMessage( Principal principal , @Valid @RequestBody ChatRequest request) throws IOException {
        UUID userId = UUID.fromString(principal.getName());

    return  chatService.processIntialMessage(userId,request.message());
    }
    @DeleteMapping("/cache")
    ResponseEntity<Void> DelteCache( Principal principal ){

        UUID userId = UUID.fromString(principal.getName());
        chatService.deleteMycachedAnswers(userId);
        return ResponseEntity.noContent().build(); // 204 No Content
    }

}

