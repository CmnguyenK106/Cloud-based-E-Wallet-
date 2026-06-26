package com.ewallet.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Wallet balance response DTO.
 *
 * Provides the current wallet state for balance enquiry endpoints.
 * BigDecimal is used to maintain precision consistency with the entity layer.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BalanceResponse {

    private UUID walletId;
    private BigDecimal balance;
    private String currency;
    private LocalDateTime updatedAt;
}
