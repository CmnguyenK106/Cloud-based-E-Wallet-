package com.ewallet.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

/**
 * JWT authentication filter that intercepts every HTTP request once.
 *
 * How it works:
 * 1. Extract the "Authorization: Bearer <token>" header.
 * 2. Validate the JWT using JwtTokenProvider.
 * 3. If valid, create a UsernamePasswordAuthenticationToken with
 *    the user's UUID as the principal and set it on the SecurityContext.
 * 4. The downstream filter chain can then access the authenticated
 *    user's identity via SecurityContextHolder.
 *
 * Design rationale:
 * - Extends OncePerRequestFilter (not GenericFilterBean) to guarantee
 *   single execution per request, even if the filter is invoked multiple times.
 * - Skips setting an authentication if the header is missing, letting
 *   Spring Security's configured rules decide the response (e.g., 401).
 * - No password is stored in the authentication object because JWT is
 *   stateless — the token itself is the credential.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    /** Standard Bearer token prefix */
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int BEARER_PREFIX_LENGTH = BEARER_PREFIX.length();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractTokenFromHeader(request);

        if (token != null && jwtTokenProvider.validateToken(token)) {
            UUID userId = jwtTokenProvider.getUserIdFromToken(token);

            // Create an authentication token with the user UUID as the principal.
            // No GrantedAuthority roles are set — this is a simple JWT auth system.
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId, null, Collections.emptyList());

            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request));

            // Set the authenticated principal on the security context
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Parse the raw JWT from the Authorization header.
     *
     * @return  The JWT string without "Bearer " prefix, or null if absent/malformed
     */
    private String extractTokenFromHeader(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX_LENGTH);
        }
        return null;
    }
}
