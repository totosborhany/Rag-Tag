package dev.totos.rag_hub.service;

import dev.totos.rag_hub.exception.ApiException;
import dev.totos.rag_hub.records.ChatResponse;
import dev.totos.rag_hub.repository.DocumentRepository;
import dev.totos.rag_hub.repository.UserRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    ChatService(UserRepository userRepository, DocumentRepository documentRepository,VectorStore vectorStore,ChatClient.Builder chatClientBuilder) {
        this.documentRepository=documentRepository;
        this.userRepository=userRepository;
        this.chatClient=chatClientBuilder.build();
        this.vectorStore=vectorStore;
    }

     public Flux<ChatResponse> processMessage(UUID userId, String message){

         FilterExpressionBuilder b = new FilterExpressionBuilder();
         SearchRequest searchRequest = SearchRequest.builder().query(message).topK(4).filterExpression(b.eq("userId",userId.toString()).build()).build();
         List<Document> similarDocs = vectorStore.similaritySearch(searchRequest);
         String context = similarDocs.stream().map(document -> document.getFormattedContent()).collect(Collectors.joining("\n\n---\n\n"));
         if(context==null || context.isBlank() ){
             throw new ApiException(" \"You haven't uploaded any documents yet. Please upload a PDF to start asking questions.\"\n", HttpStatus.NOT_FOUND);
         }
         List<String> sources = similarDocs.stream().map(doc -> (String) doc.getMetadata().getOrDefault("fileName", "Unknown Source")).distinct()
                 .toList();

         String systemPrompt = """
                You are a helpful assistant for answering questions based on uploaded documents.
                Use ONLY the provided context below to answer the user's question.
                If the answer cannot be found in the context, clearly state that the information is not in the uploaded documents.
                
                Context:
                {context}
                """;

         return chatClient.prompt()
                 .system(sp -> sp.text(systemPrompt).param("context", context.isBlank() ? "No matching documents found." : context))
                 .user(message)
                 .stream()
                 .content()
                 .map(chunk-> new ChatResponse(chunk,sources));

     }

}

