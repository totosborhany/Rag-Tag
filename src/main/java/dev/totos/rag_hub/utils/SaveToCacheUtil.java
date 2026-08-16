package dev.totos.rag_hub.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class SaveToCacheUtil {

    private static final Logger log = LoggerFactory.getLogger(SaveToCacheUtil.class);
    private final VectorStore vectorStore;

    public SaveToCacheUtil(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Async

    public void saveToChach(String message, UUID userId, String answer, List<String> sources, String id) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("userId", userId != null ? userId.toString() : null);
            metadata.put("type", "CACHE");
            metadata.put("answer", answer != null ? answer : "");
            metadata.put("sources", sources != null ? sources : List.of());
            metadata.put("id", id != null ? id.toString() : null);

            Document toBeSaved = new Document(message, metadata);

            vectorStore.add(List.of(toBeSaved));
            log.info("Successfully cached vector response for userId: {} and id: {}", userId, id);
        } catch (Exception e) {
            log.error("Failed to save response to vector cache for userId: {}", userId, e);
        }
    }
}