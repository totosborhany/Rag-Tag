package dev.totos.rag_hub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class securityConfig {
//    private final CustomAuthenticationEntryPoint authEntryPoint;
//    private final CustomAccessDeniedHandler accessDeniedHandler;
//
//    public SecurityConfig(CustomAuthenticationEntryPoint authEntryPoint,
//                          CustomAccessDeniedHandler accessDeniedHandler) {
//        this.authEntryPoint = authEntryPoint;
//        this.accessDeniedHandler = accessDeniedHandler;
//    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // 1. Disable CSRF for stateless REST endpoints
                .csrf(AbstractHttpConfigurer::disable)

                // 2. Enable CORS handling
                .cors(Customizer.withDefaults())

                // 3. Make session management stateless
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 4. Return JSON 401/403 instead of HTML login redirects
//                .exceptionHandling(exceptions -> exceptions
//                        .authenticationEntryPoint(authEntryPoint)
//                        .accessDeniedHandler(accessDeniedHandler)
//                )

                // 5. Explicit Catch-All Authorization Rule
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**", "/public/**").permitAll()
                        .anyRequest().authenticated()
                )

                // 6. Security Headers
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                )

                .build();
    }
    @Bean
    public PasswordEncoder passwordEncoder() {
        // Uses BCrypt with a default strength/log-rounds of 10
        return new BCryptPasswordEncoder();
    }
}
