package dev.totos.rag_hub.service;

import dev.totos.rag_hub.utils.CookieUtils;
import dev.totos.rag_hub.utils.RedisUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {
    @Value("${jwt.access-secret}")
    private String accessSecret;

    @Value("${jwt.access-expiration}")
    private long accessExpiration;

    @Value("${jwt.refresh-secret}")
    private String refreshSecret;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;
    private final CookieUtils cookieUtils;
    private final RedisUtil redisUtil;
    JwtService(RedisUtil redisUtil, CookieUtils cookieUtils){
        this.cookieUtils=cookieUtils;
        this.redisUtil=redisUtil;
    }
    public String generateAccessToken(UUID userId) {
        return buildToken(userId, accessSecret, accessExpiration);
    }

    public String generateRefreshToken(UUID userId) {
        return buildToken(userId, refreshSecret, refreshExpiration);
    }


    private String buildToken(UUID userId, String secretKey, long expirationMillis) {
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(key)
                .compact();
    }

    public boolean validateAccessToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(accessSecret.getBytes(StandardCharsets.UTF_8));
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    public boolean validateRefreshToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(refreshSecret.getBytes(StandardCharsets.UTF_8));
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    public UUID extractUserIdFromAccess(String token) {
        SecretKey key = Keys.hmacShaKeyFor(accessSecret.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        return UUID.fromString(claims.getSubject());
    }

    public UUID extractUserIdFromRefresh(String refreshToken) {
        SecretKey key = Keys.hmacShaKeyFor(refreshSecret.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(refreshToken).getPayload();
        return UUID.fromString(claims.getSubject());
    }
    public void saveRefreshTokenToRedis(UUID userId, String refreshToken) {
       redisUtil.saveRefreshTokenToRedis(userId,refreshToken);
    }
    public void deleteRefreshTokenFromRedis(UUID userId) {
        redisUtil.deleteFromRedis("refreshToken", userId,null);
    }

  public Boolean  validateRefreshTokenWithRedis(String refreshToken,UUID userId){
      if (userId == null || refreshToken == null) {
          return false;
      }
        String redisRefreshToken = redisUtil.getRefreshTokenFromredis(userId);

        return  refreshToken.equals(redisRefreshToken);
    }


}
