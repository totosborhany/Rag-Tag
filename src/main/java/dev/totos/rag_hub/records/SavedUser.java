package dev.totos.rag_hub.records;

import java.util.UUID;

public record SavedUser(
        UUID id, String username, String email, String createdat
) {
}
