package dev.totos.rag_hub.utils;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Component
public class SaveToCacheUtil {
    private static final Logger log = LoggerFactory.getLogger(SaveToCacheUtil.class);
private  final VectorStore vectorStore;
SaveToCacheUtil(VectorStore vectorStore){
    this.vectorStore=vectorStore;
}
@Async
public void saveToChach(String message , UUID userId, String answer, List<String> sources){
    try {
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        org.springframework.ai.document.Document toBesaved = new Document(
                message,
                Map.of(
                        "userId", userId.toString(),
                        "type", "CACHE",
                        "answer", answer,
                        "sources", sources
                )
        );
        log.info("Successfully cached vector response for userId: {}", userId);
        vectorStore.add(List.of(toBesaved));
    } catch (Exception e) {
        log.error("Failed to save response to vector cache for userId: {}", userId, e);
        //throw new RuntimeException(e);
    }
}
}
