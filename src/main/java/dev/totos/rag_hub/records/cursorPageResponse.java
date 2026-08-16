package dev.totos.rag_hub.records;

import java.time.Instant;
import java.util.List;

public record cursorPageResponse(
        List<ChatMessageDto> messages,
        Instant nextCursor,
        boolean hasNext
) {}