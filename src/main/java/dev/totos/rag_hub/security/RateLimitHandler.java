package dev.totos.rag_hub.security;

import dev.totos.rag_hub.exception.ApiException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Component
public class RateLimitHandler {

    private final HandlerExceptionResolver resolver;

    public RateLimitHandler(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }
    public  void handleRateLimitExceeded(HttpServletRequest request, HttpServletResponse response ,Long retryAfterSeconds){
        response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(retryAfterSeconds));

        resolver.resolveException(
                request,
                response,
                null,
                new ApiException(
                        "IP rate limit exceeded. Try again in %d seconds.".formatted(retryAfterSeconds),
                        HttpStatus.TOO_MANY_REQUESTS
                )
        );
    }
}
