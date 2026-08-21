package dev.totos.rag_hub.service;

import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

@Service
public class TokenUsageLimitQuota {

    private static final Long TOKEN_LIMIT = 1000000L;
    public final ReactiveRedisTemplate<String, Long> redisTemplate;

    public TokenUsageLimitQuota(ReactiveRedisTemplate<String, Long> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    public Mono<Long> updateTokenUsageLimitQuota(UUID userId, long tokens) {
        if (userId == null) {
            return Mono.error(new IllegalArgumentException("UserId cannot be null"));
        }
        String key = "USER:USAGE:" + userId;

        // Atomically increment. Redis creates key initialized to 0 if it doesn't exist.
        return redisTemplate.opsForValue().increment(key, tokens)
                .flatMap(newTotal -> {
                    // If newTotal equals tokens, key was just created in this call -> set TTL
                    if (newTotal == tokens) {
                        return redisTemplate.expire(key, Duration.ofDays(30))
                                .thenReturn(newTotal);
                    }
                    return Mono.just(newTotal);
                });
    }

    public Mono<Boolean> surpasedTokenUsageLimitQuota(UUID userId) {
        if (userId == null) {
            return Mono.just(false);
        }
        String key = "USER:USAGE:" + userId;

        return redisTemplate.opsForValue().get(key)
                .map(usage -> usage >= TOKEN_LIMIT)
                .defaultIfEmpty(false);
    }

    public Mono<Long> deleteUserLimitToken(UUID userId) {
        if (userId == null) {
            return Mono.error(new IllegalArgumentException("UserId cannot be null"));
        }
        String key = "USER:USAGE:" + userId;
        return redisTemplate.delete(key);
    }
}