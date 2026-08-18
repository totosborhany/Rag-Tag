package dev.totos.rag_hub.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.totos.rag_hub.config.AiWbClient;
import dev.totos.rag_hub.entity.ChatMessage;
import dev.totos.rag_hub.entity.Conversation;
import dev.totos.rag_hub.entity.MessageRole;
import dev.totos.rag_hub.exception.ApiException;
import dev.totos.rag_hub.records.ChatResponse;
import dev.totos.rag_hub.repository.ChatMessageRepository;
import dev.totos.rag_hub.repository.ConversationRepository;
import dev.totos.rag_hub.utils.SaveToCacheUtil;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class ChatService {
    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);
        @Value("${spring.ai.openai.chat.options.model}")
        String myModel;
        private final ChatClient chatClient;
        private final VectorStore vectorStore;
        private final SaveToCacheUtil saveToCacheUtil;
        private final ReactiveRedisTemplate<String,String> redisTemplate;
        private final ChatMessageRepository chatMessageRepository;
        private final ConversationRepository conversationRepository;
        private final AiWbClient aiWebClient;
        private final TokenUsageLimitQuota tokenUsageLimitQuota;
    private final ObjectMapper objectMapper;
        ChatService(AiWbClient aiWebClient,ObjectMapper objectMapper,VectorStore vectorStore,ChatClient.Builder chatClientBuilder,ConversationRepository conversationRepository,ChatMessageRepository chatMessageRepository,SaveToCacheUtil saveToCacheUtil,ReactiveRedisTemplate<String,String> redisTemplate,TokenUsageLimitQuota tokenUsageLimitQuota) {
        this.chatMessageRepository=chatMessageRepository;
        this.chatClient=chatClientBuilder.build();
        this.vectorStore=vectorStore;
        this.saveToCacheUtil=saveToCacheUtil;
        this.redisTemplate=redisTemplate;
        this.conversationRepository=conversationRepository;
        this.aiWebClient=aiWebClient;
        this.objectMapper=objectMapper;
        this.tokenUsageLimitQuota=tokenUsageLimitQuota;
        }
        @RateLimiter(name = "ragLlmLimiter" , fallbackMethod = "processMessageFallback")
        public Flux<ServerSentEvent<Object>> processIntialMessage(UUID userId, String message, UUID conversationId) {
            return checkCache(message, userId, conversationId)
                    .switchIfEmpty(
                            tokenUsageLimitQuota.surpasedTokenUsageLimitQuota(userId)
                                    .flatMapMany(surpassed -> {
                                        if (Boolean.TRUE.equals(surpassed)) {
                                            return Flux.error(new ApiException(
                                                    "Sorry you have reached your token limit",
                                                    HttpStatus.BAD_REQUEST
                                            ));
                                        }

                                        return Flux.defer(() -> processMessage(userId, message, conversationId))
                                                .subscribeOn(Schedulers.boundedElastic());
                                    })
                    );
        }
    public Flux<ServerSentEvent<Object>> processMessageFallback(UUID userId, String message, UUID conversationId, Throwable t) {
        return Flux.error(new ApiException(
                "You are sending queries too fast. Please wait before asking again.",
                HttpStatus.TOO_MANY_REQUESTS
        ));
    }

    private Flux<ServerSentEvent<Object>> checkCache(String message , UUID userId,UUID conversationId){
        return Mono.fromCallable(() -> {
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
            if (cacheHits.isEmpty()) {
                return null;
            }            Conversation conversation = conversationRepository.findByIdAndUserId(conversationId,userId).orElseThrow(()->new ApiException("Soryy this conversation doesnt exist",HttpStatus.NOT_FOUND)) ;

            Document hit = cacheHits.get(0);

            String answer = (String) hit.getMetadata().getOrDefault("answer", "");
            List<String> sources = (List<String>) hit.getMetadata().getOrDefault("sources", List.of());
            ServerSentEvent<Object> sourcesEvent = ServerSentEvent.builder().event("sources").data(sources).build();
            ServerSentEvent<Object> deltaStream = ServerSentEvent.builder().event("delta").data(answer).build();
            ChatMessage chatMessage = new ChatMessage(MessageRole.USER, message, 0, conversation);
            ChatMessage savedChatMessage=     chatMessageRepository.save(chatMessage);
            ChatMessage LLmChatMessage = new ChatMessage(MessageRole.ASSISTANT, answer,null , conversation);
            LLmChatMessage.setParentMessageId(savedChatMessage.getId());
            chatMessageRepository.save(LLmChatMessage);
            String redisKey = "chat:history:" + conversationId;
            redisTemplate.opsForList().rightPush(redisKey, "User: " + message);
            redisTemplate.opsForList().rightPush(redisKey, "Assistant: " + answer);
            redisTemplate.opsForList().trim(redisKey, -10, -1);
            redisTemplate.expire(redisKey, java.time.Duration.ofDays(7));
            return Flux.just(sourcesEvent, deltaStream)
                    .doOnCancel(() -> {
                        logger.info("Client canceled streaming response for conversation {}", conversationId);
                    }).onErrorResume(e -> {
                logger.error("Error during streaming for conversation {}", conversationId, e);

                ServerSentEvent<Object> errorEvent = ServerSentEvent.builder()
                        .event("error")
                        .data("Stream interrupted: " + e.getMessage())
                        .build();

                return Flux.just(errorEvent);
            });
    }).subscribeOn(Schedulers.boundedElastic()).flatMapMany(flux->flux);
    }
        private Flux<ServerSentEvent<Object>> processMessage(UUID userId, String message, UUID conversationId){

         FilterExpressionBuilder b = new FilterExpressionBuilder();
         SearchRequest searchRequest = SearchRequest.builder()
                 .query(message)
                 .topK(4)
                 .filterExpression(
                 b.and(b.eq("userId",userId.toString())
                                 ,
                                 b.eq("type","NORMAL"))
                 .build()).build();
         Conversation conversation = conversationRepository.findByIdAndUserId(conversationId,userId).orElseThrow(()->new ApiException("Sorry this conversation doesnt exist",HttpStatus.NOT_FOUND)) ;
         List<Document> similarDocs = vectorStore.similaritySearch(searchRequest);
       //  logger.info("Similar docs: {}", similarDocs);
         String context = similarDocs.stream()
                 .map(document -> document.getFormattedContent()).collect(Collectors.joining("\n\n---\n\n"));
          //  logger.info("Context: {}", context);


         if(context==null || context.isBlank() ){
             throw new ApiException(" \"You haven't uploaded any documents yet. Please upload a PDF to start asking questions.\"\n" +  similarDocs, HttpStatus.NOT_FOUND);
         }
            ChatMessage chatMessage = new ChatMessage(MessageRole.USER,message,null,conversation);
            ChatMessage  savedChatMessage =  chatMessageRepository.save(chatMessage);

            List<String> sources = similarDocs.stream().map(doc -> (String) doc.getMetadata().getOrDefault("fileName", "Unknown Source")).distinct()
                 .toList();
            String redisKey = "chat:history:" + conversationId;
            List<String> historyList = redisTemplate.opsForList().range(redisKey, 0, -1).collectList().block();
            ServerSentEvent<Object> tokenEvent ;
            String historyText = (historyList == null || historyList.isEmpty())
                    ? "No previous conversation history."
                    : String.join("\n", historyList);
          AtomicReference< Long> totalTokens=new AtomicReference<>();
            AtomicReference< Long> promptTokens = new AtomicReference<>();
            AtomicReference< Long>completionTokens = new AtomicReference<>();
         String systemPrompt = """
                You are a helpful assistant for answering questions based on uploaded documents.
                Use ONLY the provided context below to answer the user's question.
                If the answer cannot be found in the context, clearly state that the information is not in the uploaded documents.
                 Recent Conversation History:
                             {history}
                Context:
                {context}
                """;
         StringBuilder finalAnswer = new StringBuilder();
            Map<String, Object> requestPayload = Map.of(
                    "model", myModel,
                    "stream_options", Map.of("include_usage", true) ,
                    "stream", true,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt + " Context: " + (context.isBlank() ? "No matching documents found." : context) + " History: " + (historyText.isBlank() ? "No history found." : historyText)),
                            Map.of("role", "user", "content", message)
                    )
            );
            ServerSentEvent<Object>  sourcesEvent = ServerSentEvent.builder().event("sources").data(sources).build();
            Flux<ServerSentEvent<Object>> deltaStream =   aiWebClient.webClient().post().uri("/chat/completions")
                    .bodyValue(requestPayload)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response ->
                            response.bodyToMono(String.class)
                                    .defaultIfEmpty("<empty body>")
                                    .flatMap(body -> {
                                        logger.error("Gemini API error {} - body: {}", response.statusCode(), body);
                                        return Mono.error(new ApiException(
                                                "Gemini API error: " + body,
                                                HttpStatus.valueOf(response.statusCode().value())
                                        ));
                                    })
                    )
                    .bodyToFlux(String.class)
                    .filter(chunk -> !"[DONE]".equals(chunk.trim()))

                    .mapNotNull(chunk -> {
                        try {

                            JsonNode node = objectMapper.readTree(chunk);
                            JsonNode contentNode = node.path("choices").path(0).path("delta").path("content");
                            JsonNode usageNode = node.path("usage");
                            if (!usageNode.isMissingNode() && !usageNode.isNull()) {
                                promptTokens.set(usageNode.path("prompt_tokens").asLong(0));
                                completionTokens.set(usageNode.path("completion_tokens").asLong(0));
                                totalTokens.set(usageNode.path("total_tokens").asLong(0));
                            }
                            return contentNode.isMissingNode() ? null : contentNode.asText();
                        } catch (Exception e) {
                            logger.warn("Failed to parse chunk: {}", chunk, e);
                            return null;
                        }
                    })
                    .filter(text -> text != null && !text.isEmpty())
                    .doOnNext(text -> {
                        finalAnswer.append(text);
                    })
                    .doOnComplete(() -> {
                        Mono.fromRunnable(() -> {
                                    tokenUsageLimitQuota.updateTokenUsageLimitQuota(userId,totalTokens.get());
                                    String fullAnswer = finalAnswer.toString();

                                    redisTemplate.opsForList().rightPush(redisKey, "User: " + message);
                                    redisTemplate.opsForList().rightPush(redisKey, "Assistant: " + fullAnswer);
                                    redisTemplate.opsForList().trim(redisKey, -10, -1);
                                    redisTemplate.expire(redisKey, java.time.Duration.ofDays(7));

                                    saveToCacheUtil.saveToChach(message, userId, fullAnswer, sources,savedChatMessage.getId().toString(),conversationId.toString());

                                    ChatMessage llmChatMessage = new ChatMessage(
                                            MessageRole.ASSISTANT,
                                            fullAnswer,
                                            null,
                                            conversation
                                    );
                                    llmChatMessage.setParentMessageId(savedChatMessage.getId());

                                    chatMessageRepository.save(llmChatMessage);
                                })
                                .subscribeOn(Schedulers.boundedElastic())
                                .subscribe();
                    })
                    .map(chunk -> ServerSentEvent.builder().data(chunk).event("delta").build());


            return Flux.concat(Mono.just(sourcesEvent), deltaStream).doOnCancel(() -> {
                logger.info("Client canceled streaming response for conversation {}", conversationId);
            }).onErrorResume(e -> {
                logger.error("Error during streaming for conversation {}", conversationId, e);

                ServerSentEvent<Object> errorEvent = ServerSentEvent.builder()
                        .event("error")
                        .data("Stream interrupted: " + e.getMessage())
                        .build();

                return Flux.just(errorEvent);
            });
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

