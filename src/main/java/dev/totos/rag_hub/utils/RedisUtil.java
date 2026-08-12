package dev.totos.rag_hub.utils;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
public class RedisUtil {
private final RedisTemplate<String ,String> redisTemplate;
RedisUtil(RedisTemplate<String ,String> redisTemplate){
    this.redisTemplate=redisTemplate;
}
    public void saveRefreshTokenToRedis(UUID userId, String refreshToken) {
        redisTemplate.opsForValue().set(
                "refreshToken:" + userId,
                refreshToken,
                Duration.ofDays(7)
        );
    }

    public String getRefreshTokenFromredis(UUID userId){
       return redisTemplate.opsForValue().get("refreshToken:" + userId);
    }
    public String getResetTokenFromredis( String token){
        return redisTemplate.opsForValue().get("resetToken:" + token);
    }
    public void deleteFromRedis(String type,UUID userId,String token){
        if("refreshToken".equalsIgnoreCase(type)){
            redisTemplate.delete("refreshToken:" + userId);

        }else if("resetToken".equalsIgnoreCase(type)){
            redisTemplate.delete("resetToken:" + token);

        }

    }
    public void    saveResetTokenToredis(UUID userid,String resetToken) {
        redisTemplate.opsForValue().set("resetToken:" + resetToken, userid.toString(), Duration.ofMinutes(15));
    }

}
