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

  //  private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private  final ChatService chatService;
    ChatController(ChatService chatService){
        this.chatService=chatService;

    }
    @PostMapping
    Flux<ChatResponse> SendChatMessage(@AuthenticationPrincipal Principal principal , @Valid @RequestBody ChatRequest request) throws IOException {
      //  SseEmitter sseEmitter = new SseEmitter(Long.MAX_VALUE);
       // emitters.add(sseEmitter);
//        sseEmitter.onCompletion(() -> emitters.remove(sseEmitter));
//        sseEmitter.onTimeout(() -> emitters.remove(sseEmitter));
//        sseEmitter.onError((e) -> emitters.remove(sseEmitter));
        UUID userId = UUID.fromString(principal.getName());

         //sseEmitter.send(SseEmitter.event().name("NEWS").data(chatResponse));;
    return  chatService.processMessage(userId,request.message());
    }
    @DeleteMapping("/cache")
    void DelteCache(@AuthenticationPrincipal Principal principal ){

        UUID userId = UUID.fromString(principal.getName());

    }

}

