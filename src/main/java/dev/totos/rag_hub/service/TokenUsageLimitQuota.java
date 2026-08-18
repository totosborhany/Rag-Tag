package dev.totos.rag_hub.service;

import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public class TokenUsageLimitQuota {
    private static  final Long TOKEN_LIMIT=1000000L;
public final ReactiveRedisTemplate<String,Long> redisTemplate;

public TokenUsageLimitQuota(ReactiveRedisTemplate<String, Long> redisTemplate) {
    this.redisTemplate = redisTemplate;
}
    public Mono<Long> updateTokenUsageLimitQuota(UUID userId, long tokens) {
        String key = "USER:USAGE:" + userId.toString();

        return redisTemplate.hasKey(key)
                .flatMap(exists -> {
                    if (!Boolean.TRUE.equals(exists)) {
                        return redisTemplate.opsForValue()
                                .set(key, 0L, java.time.Duration.ofDays(30))
                                .then(redisTemplate.opsForValue().increment(key, tokens));
                    }
                    return redisTemplate.opsForValue().increment(key, tokens);
                });
    }

public Mono<Boolean> surpasedTokenUsageLimitQuota(UUID userId){
    String key = "USER:USAGE:" + userId.toString();

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
