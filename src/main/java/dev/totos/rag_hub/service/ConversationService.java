package dev.totos.rag_hub.service;

import dev.totos.rag_hub.repository.ConversationRepository;
import org.springframework.stereotype.Service;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    ConversationService(ConversationRepository conversationRepository){
        this.conversationRepository=conversationRepository;
    }

}
