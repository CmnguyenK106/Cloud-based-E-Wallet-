package com.ewallet.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

/**
 * JWT token provider — handles creation, parsing, and validation
 * of HMAC-SHA256 signed JSON Web Tokens.
 *
 * Security design:
 * - The signing key is a 256-bit base64-encoded secret externalised
 *   in application.properties (never hardcoded).
 * - Tokens carry the user's UUID as the "sub" (subject) claim.
 * - Expiration is set to 24 hours from issuance.
 * - JJWT library is used exclusively — no manual Base64 or HMAC code.
 *
 * Thread-safety: JJWT's DefaultJwtParserBuilder is immutable after build,
 * so this component is safely treated as a singleton.
 */
@Component
public class JwtTokenProvider {

    private final SecretKey signingKey;
    private final long expirationMs;

    /**
     * @param secret    Base64-encoded 256-bit HMAC secret
     * @param expirationMs  Token time-to-live in milliseconds (default: 86400000 = 24h)
     */
    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms:86400000}") long expirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expirationMs = expirationMs;
    }

    /**
     * Generate a JWT access token for the given user.
     *
     * @param userId  The user's UUID (stored as the "sub" claim)
     * @return  A signed JWT string in compact serialisation format
     */
    public String generateToken(UUID userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Extract the user UUID from a valid JWT.
     *
     * @param token  The raw JWT string (without "Bearer " prefix)
     * @return  The user UUID stored in the "sub" claim
     */
    public UUID getUserIdFromToken(String token) {
        String subject = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
        return UUID.fromString(subject);
    }

    /**
     * Validate a JWT token.
     *
     * @param token  The raw JWT string
     * @return  true if the token is valid (correct signature, not expired)
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // Covers: expired, malformed, invalid signature, unsupported format
            return false;
        }
    }
}
