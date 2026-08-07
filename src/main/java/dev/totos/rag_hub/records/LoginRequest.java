package dev.totos.rag_hub.records;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        @NotBlank(message = "Email is required")
        String email,


        @NotBlank(message = "Password is required")

        String password
) {
}
