package com.ewallet.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

/**
 * P2P transfer request DTO.
 *
 * Validates that:
 * - receiverUsername is provided (resolved to a Wallet by service layer)
 * - amount is positive (> 0) and uses BigDecimal precision
 * - description is optional but capped at 255 characters
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class TransferRequest {

    @NotBlank(message = "Receiver username is required")
    private String receiverUsername;

    @DecimalMin(value = "0.0001", message = "Transfer amount must be at least 0.0001")
    private BigDecimal amount;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;
}
