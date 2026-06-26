package com.ewallet.dto.response;

import com.ewallet.entity.TransactionStatus;
import com.ewallet.entity.TransactionType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Transaction history response DTO.
 *
 * Represents a single ledger entry in the transaction history.
 * The counterpartyName field identifies the other party involved
 * (sender for RECEIVE transactions, receiver for TRANSFER transactions).
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TransactionResponse {

    private UUID id;
    private String referenceNumber;
    private BigDecimal amount;
    private TransactionType type;
    private TransactionStatus status;
    private String description;
    private String counterpartyName;
    private LocalDateTime createdAt;
}
