package com.ewallet.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security configuration for the e-wallet application.
 *
 * Security design decisions:
 *
 * 1. STATELESS sessions — The API uses JWT tokens, so no HTTP session
 *    is created or used (SessionCreationPolicy.STATELESS).
 *
 * 2. CSRF disabled — REST APIs using JWT are immune to CSRF attacks
 *    because the browser does not automatically attach Bearer tokens
 *    to cross-origin requests (unlike cookies).
 *
 * 3. CORS — Explicitly configured to allow the S3 static site origin.
 *    In development, this can be relaxed to allow all origins.
 *
 * 4. Public endpoints — /api/auth/** is accessible without authentication
 *    (register and login). All other endpoints require a valid JWT.
 *
 * 5. JWT filter — Injected before UsernamePasswordAuthenticationFilter
 *    so that the JWT is validated before Spring Security attempts
 *    any further authentication processing.
 *
 * 6. BCrypt — PasswordEncoder bean with strength factor 10 for
 *    secure password hashing.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF (stateless JWT API)
            .csrf(csrf -> csrf.disable())

            // Enable CORS with the custom configuration below
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Stateless session management
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Route authorisation rules
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .anyRequest().authenticated())

            // Inject JWT filter before the standard authentication filter
            .addFilterBefore(jwtAuthenticationFilter,
                             UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS configuration.
     *
     * Allows the S3 static website origin to call the API.
     * All methods, headers, and credentials are permitted.
     * Update allowedOrigins with the actual S3 bucket URL in production.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
            "http://localhost:5500",          // Live Server (dev)
            "http://127.0.0.1:5500",          // Live Server (dev)
            "http://localhost:3000",          // Alternative dev port
            "http://localhost:8080"           // Same-origin dev
            // TODO: Add the actual S3 bucket URL for production, e.g.:
            // "http://my-e-wallet.s3-website.ap-southeast-1.amazonaws.com"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * BCrypt password encoder with strength factor 10.
     * Used to hash user passwords during registration and verify
     * them during login.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
