package dev.totos.rag_hub.security;

import dev.totos.rag_hub.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token = null;

        // 1. Extract the access token from cookies
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        // 2. Validate token and authenticate request
        if (token != null && jwtService.validateAccessToken(token)) {
            UUID userId = jwtService.extractUserIdFromAccess(token);

            // Create Authentication token (principal = userId.toString())
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId.toString(), // <-- This populates principal.getName() in your controller!
                            null,
                            Collections.emptyList() // Add user roles/authorities here if needed
                    );

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // Set authentication into Spring Security Context
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // 3. Continue the filter chain execution
        filterChain.doFilter(request, response);
    }
}