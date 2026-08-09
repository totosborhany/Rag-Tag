package dev.totos.rag_hub.records;

import org.springframework.http.HttpStatus;

import java.time.Instant;

public record ErrorResponse(
        String error,
        int status,
        String path,
        Instant timestamp
) {
    public static ErrorResponse of(String message, HttpStatus status, String path) {
        return new ErrorResponse(message, status.value(), path, Instant.now());
    }
}