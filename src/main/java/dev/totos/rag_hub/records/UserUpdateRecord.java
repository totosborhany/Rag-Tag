package dev.totos.rag_hub.records;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
public record UserUpdateRecord(
        String email,
        String username
) {
}
