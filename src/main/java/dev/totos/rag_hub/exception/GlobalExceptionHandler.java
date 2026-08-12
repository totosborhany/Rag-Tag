package dev.totos.rag_hub.exception;

import dev.totos.rag_hub.records.ErrorResponse;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.concurrent.CompletionException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.of(ex.getMessage(), ex.getStatus(), request.getRequestURI());
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex, HttpServletRequest request) {
        // Log the real stack trace internally so you can debug it
        log.error("Unhandled exception occurred at path: {}", request.getRequestURI(), ex);
        ErrorResponse body = ErrorResponse.of(
                "An unexpected internal error occurred.",
                HttpStatus.INTERNAL_SERVER_ERROR,
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.of(
                "Authentication failed: " + ex.getMessage(),
                HttpStatus.UNAUTHORIZED,
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }
    @ExceptionHandler(CompletionException.class)
    public ResponseEntity<Map<String, Object>> handleCompletionException(CompletionException ex) {
        if (ex.getCause() instanceof ApiException apiException) {
            return ResponseEntity
                    .status(apiException.getStatus())
                    .body(Map.of("error", apiException.getMessage()));
        }

        // Fallback for unexpected background crashes
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "An unexpected error occurred during processing"));
    }
    @ExceptionHandler(RequestNotPermitted.class)
    public ResponseEntity<ErrorResponse> handleRateLimitError(RequestNotPermitted ex, HttpServletRequest request){
        log.error("rate limit exception occurred at path: {}", request.getRequestURI(), ex);

        ErrorResponse body =  ErrorResponse.of(
                "Too many Requests try again later"+ex.getMessage(),
                HttpStatus.TOO_MANY_REQUESTS
                ,
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", "60").body(body);
    }

}
