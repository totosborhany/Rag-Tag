package dev.totos.rag_hub.security;
import dev.totos.rag_hub.exception.ApiException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Component
public class IpRateLimitingFilter extends OncePerRequestFilter {

    private final ProxyManager<String> proxyManager;
    private final RateLimitHandler rateLimitHandler;
    public IpRateLimitingFilter(ProxyManager<String> proxyManager,RateLimitHandler rateLimitHandler) {
        this.proxyManager = proxyManager;
        this.rateLimitHandler=rateLimitHandler;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String clientIp = extractClientIp(request);
        String redisKey = "rate_limit:ip:" + clientIp;

        // Bucket Configuration: 10 requests per 1 minute
        BucketConfiguration bucketConfig = BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(10)
                        .refillIntervally(10, Duration.ofMinutes(1))
                        .build())
                .build();

        // Retrieve or initialize bucket from Redis
        ConsumptionProbe probe = proxyManager.builder()
                .build(redisKey, () -> bucketConfig)
                .tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
        } else {
            long waitForRefillSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;

            rateLimitHandler.handleRateLimitExceeded(request,response,waitForRefillSeconds);
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}