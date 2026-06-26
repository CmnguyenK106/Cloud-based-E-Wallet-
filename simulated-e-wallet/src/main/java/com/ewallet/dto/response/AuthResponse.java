package com.ewallet.dto.response;

import lombok.*;

import java.util.UUID;

/**
 * Authentication response DTO.
 *
 * Returned after successful registration or login.
 * Contains the JWT access token and basic user profile fields
 * so the client can immediately use the token for subsequent requests.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuthResponse {

    private String token;
    private String tokenType;    // Always "Bearer"
    private UUID userId;
    private String username;
    private String fullName;
    private String email;
}
