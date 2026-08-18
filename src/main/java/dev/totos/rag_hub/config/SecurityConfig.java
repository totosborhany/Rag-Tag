package dev.totos.rag_hub.config;

// Make sure to adjust this import to match your JwtAuthenticationFilter location
import dev.totos.rag_hub.security.DelegatingAuthenticationEntryPoint;
import dev.totos.rag_hub.security.IpRateLimitingFilter;
import dev.totos.rag_hub.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig { // Renamed to PascalCase (SecurityConfig)

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final DelegatingAuthenticationEntryPoint authEntryPoint;
    private final IpRateLimitingFilter ipRateLimitingFilter;
    // 1. Inject your custom JWT filter
    public SecurityConfig(IpRateLimitingFilter ipRateLimitingFilter,JwtAuthenticationFilter jwtAuthFilter,DelegatingAuthenticationEntryPoint authEntryPoint) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.authEntryPoint=authEntryPoint;
        this.ipRateLimitingFilter=ipRateLimitingFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(request -> {
                    // Ensure this is org.springframework.web.cors.CorsConfiguration
                    org.springframework.web.cors.CorsConfiguration opt = new org.springframework.web.cors.CorsConfiguration();
                    opt.setAllowedOrigins(List.of("*"));
                    opt.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    opt.setAllowedHeaders(List.of("*"));
                    opt.setAllowCredentials(true);

                    return opt;
                }))


                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                ).exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authEntryPoint)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**", "/actuator/health", "/actuator/info","/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(ipRateLimitingFilter, JwtAuthenticationFilter.class.getClass().equals(JwtAuthenticationFilter.class) ? UsernamePasswordAuthenticationFilter.class : UsernamePasswordAuthenticationFilter.class)


                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                )
                .securityContext(security -> security
                        .requireExplicitSave(false)
                )
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}