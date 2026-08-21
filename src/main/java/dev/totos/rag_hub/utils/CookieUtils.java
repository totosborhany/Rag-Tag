package dev.totos.rag_hub.utils;


import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import java.time.Duration;

@Component
public class CookieUtils {

    public ResponseCookie createAccessCookie(String token) {
        return ResponseCookie.from("accessToken", token)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofMinutes(15))
                .sameSite("Strict")
                .build();
    }

    public ResponseCookie createRefreshCookie(String token) {
        return ResponseCookie.from("refreshToken", token)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofDays(7))
                .sameSite("Strict")
                .build();
    }

    public ResponseCookie clearAccessCookie() {
        return ResponseCookie.from("accessToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();
    }

    public ResponseCookie clearRefreshCookie() {
        return ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0) // Instantly deletes the cookie
                .sameSite("Strict")
                .build();
    }
}