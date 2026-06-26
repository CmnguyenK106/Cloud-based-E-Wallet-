package com.ewallet.dto.request;

import jakarta.validation.constraints.DecimalMin;
import lombok.*;

import java.math.BigDecimal;

/**
 * Top-up (deposit) request DTO.
 *
 * Validates that the deposit amount is positive.
 * The service layer will handle creating the wallet credit
 * and the corresponding audit ledger entry.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class TopUpRequest {

    @DecimalMin(value = "0.0001", message = "Top-up amount must be at least 0.0001")
    private BigDecimal amount;
}
