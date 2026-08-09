package dev.totos.rag_hub.records;

import java.util.List;

public record ChatResponse(
        String answer,
        List<String> sources
) {
}
