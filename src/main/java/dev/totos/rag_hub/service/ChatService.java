package dev.totos.rag_hub.service;

import dev.totos.rag_hub.exception.ApiException;
import dev.totos.rag_hub.records.ChatResponse;
import dev.totos.rag_hub.repository.DocumentRepository;
import dev.totos.rag_hub.repository.UserRepository;
import dev.totos.rag_hub.utils.SaveToCacheUtil;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChatService {

        private final ChatClient chatClient;
        private final VectorStore vectorStore;
        private final SaveToCacheUtil saveToCacheUtil;
        ChatService(VectorStore vectorStore,ChatClient.Builder chatClientBuilder,SaveToCacheUtil saveToCacheUtil) {

        this.chatClient=chatClientBuilder.build();
        this.vectorStore=vectorStore;
        this.saveToCacheUtil=saveToCacheUtil;
        }
        @RateLimiter(name = "ragLlmLimiter" , fallbackMethod = "processMessageFallback")
        public Flux<ChatResponse> processIntialMessage(UUID userId, String message) {

            return  checkCache(message, userId).switchIfEmpty(Flux.defer(()->processMessage(userId,message)));
        }
    public Flux<ChatResponse> processMessageFallback(UUID userId, String message, Throwable t) {
        return Flux.error(new ApiException(
                "You are sending queries too fast. Please wait before asking again.",
                HttpStatus.TOO_MANY_REQUESTS
        ));
    }


        private Flux<ChatResponse> processMessage(UUID userId, String message){

         FilterExpressionBuilder b = new FilterExpressionBuilder();
         SearchRequest searchRequest = SearchRequest.builder()
                 .query(message)
                 .topK(4)
                 .filterExpression(
                 b.and(b.eq("userId",userId.toString()),
                                 b.ne("type","CACHE"))
                 .build()).build();

         List<Document> similarDocs = vectorStore.similaritySearch(searchRequest);
         String context = similarDocs.stream()
                 .map(document -> document.getFormattedContent()).collect(Collectors.joining("\n\n---\n\n"));
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
         StringBuilder finalAnswer = new StringBuilder();
         return chatClient
                 .prompt()
                 .system(sp -> sp.text(systemPrompt).param("context", context.isBlank() ? "No matching documents found." : context))
                 .user(message)
                 .stream()
                 .content()
                 .doOnNext(chunk -> finalAnswer.append(chunk)) // Accumulate token by token
                 .doOnComplete(() -> saveToCacheUtil.saveToChach(message, userId, finalAnswer.toString(), sources)) // Save full answer on finish
                 .map(chunk->
                     new ChatResponse(chunk, sources)
                 );

        }
        private Flux<ChatResponse> checkCache(String message , UUID userId){
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        SearchRequest searchRequest = SearchRequest.builder()
                .similarityThreshold(0.93)
                .query(message)
                .topK(1).
                filterExpression(
                b.and(
                        b.eq("userId",userId.toString()),
                        b.eq("type","CACHE")
                ).build()
        ).build();
        List<Document> cacheHits = vectorStore.similaritySearch(searchRequest);
        if(!cacheHits.isEmpty()){
            Document hit = cacheHits.get(0);

            // Safe extraction with default fallback
            String answer = (String) hit.getMetadata().getOrDefault("answer", "");

            @SuppressWarnings("unchecked")
            List<String> sources = (List<String>) hit.getMetadata().getOrDefault("sources", List.of());
            return  Flux.just(new ChatResponse(answer,sources))  ;
        }
        return Flux.empty();
     }
    public void deleteMycachedAnswers(UUID userId){
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        vectorStore.delete(
                b.and(
                        b.eq("userId",userId.toString()),
                        b.eq("type","CACHE")
                ).build()
        );


        }

}

