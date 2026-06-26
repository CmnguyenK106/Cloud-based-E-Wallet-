package com.ewallet.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Registration request DTO.
 *
 * Validation rules:
 * - username: 3-50 chars, unique (enforced by service layer)
 * - password: 8-100 chars, must contain uppercase + lowercase + digit
 * - fullName: 1-100 chars
 * - email: valid format, unique (enforced by service layer)
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
             message = "Password must contain at least one uppercase letter, one lowercase letter, and one digit")
    private String password;

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name must not exceed 100 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;
}
