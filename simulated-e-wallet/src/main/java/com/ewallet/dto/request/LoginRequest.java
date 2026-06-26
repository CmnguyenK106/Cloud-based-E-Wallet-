package com.ewallet.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Login request DTO.
 * Carries username + password for credential verification.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;
}
