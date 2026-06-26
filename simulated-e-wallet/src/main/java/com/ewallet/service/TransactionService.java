package com.ewallet.service;

import com.ewallet.dto.response.TransactionResponse;
import com.ewallet.entity.*;
import com.ewallet.exception.InsufficientBalanceException;
import com.ewallet.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Transaction service — responsible for creating immutable audit ledger
 * entries and querying transaction history.
 *
 * Double-Entry Accounting:
 * Every balance-changing operation produces one or more Transaction rows.
 * For a P2P transfer, TWO rows are created atomically:
 *   1. TRANSFER  — sender's wallet (amount deducted)
 *   2. RECEIVE   — receiver's wallet (amount credited)
 * This ensures a complete audit trail — no balance is ever modified
 * without a corresponding ledger entry.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String REF_PREFIX = "TXN-";

    /**
     * Generate a unique human-readable reference number.
     *
     * Format: TXN-YYYYMMDD-XXXXX
     * Example: TXN-20260624-A3F2K
     *
     * The alphanumeric suffix provides 36^5 ≈ 60 million combinations
     * per day, making collisions negligible.
     */
    public String generateReferenceNumber() {
        String datePart = LocalDate.now().format(DATE_FMT);
        String randomPart = generateRandomAlphanumeric(5);
        return REF_PREFIX + datePart + "-" + randomPart;
    }

    /**
     * Create a single transaction ledger entry.
     *
     * @param senderWallet    The sender's wallet (nullable for TOPUP)
     * @param receiverWallet  The receiver's wallet (nullable for WITHDRAW)
     * @param amount          Transaction amount (always positive)
     * @param type            Transaction type (TRANSFER, RECEIVE, TOPUP, WITHDRAW)
     * @param status          Initial status (PENDING, SUCCESS, FAILED)
     * @param description     Optional description/memo
     * @return  The persisted Transaction entity
     */
    @Transactional
    public Transaction createTransaction(Wallet senderWallet,
                                         Wallet receiverWallet,
                                         BigDecimal amount,
                                         TransactionType type,
                                         TransactionStatus status,
                                         String description) {
        Transaction transaction = Transaction.builder()
                .referenceNumber(generateReferenceNumber())
                .senderWallet(senderWallet)
                .receiverWallet(receiverWallet)
                .amount(amount)
                .type(type)
                .status(status)
                .description(description)
                .build();

        Transaction saved = transactionRepository.save(transaction);
        log.info("Transaction created: ref={}, type={}, amount={}, status={}",
                saved.getReferenceNumber(), type, amount, status);
        return saved;
    }

    /**
     * Retrieve paginated transaction history for a wallet.
     * Returns both sent (sender) and received (receiver) entries,
     * ordered newest-first.
     */
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionHistory(UUID walletId, Pageable pageable) {
        Page<Transaction> transactions = transactionRepository
                .findBySenderWalletIdOrReceiverWalletIdOrderByCreatedAtDesc(
                        walletId, walletId, pageable);

        return transactions.map(this::toTransactionResponse);
    }

    /**
     * Map a Transaction entity to a TransactionResponse DTO.
     * The counterpartyName is dynamically resolved based on the
     * transaction type relative to the requesting wallet.
     */
    private TransactionResponse toTransactionResponse(Transaction tx) {
        String counterpartyName = null;

        // For TRANSFER, the counterparty is the receiver's owner
        if (tx.getReceiverWallet() != null
                && tx.getReceiverWallet().getUser() != null) {
            counterpartyName = tx.getReceiverWallet().getUser().getFullName();
        }
        // For RECEIVE, the counterparty is the sender
        if (tx.getSenderWallet() != null
                && tx.getSenderWallet().getUser() != null) {
            counterpartyName = tx.getSenderWallet().getUser().getFullName();
        }

        return TransactionResponse.builder()
                .id(tx.getId())
                .referenceNumber(tx.getReferenceNumber())
                .amount(tx.getAmount())
                .type(tx.getType())
                .status(tx.getStatus())
                .description(tx.getDescription())
                .counterpartyName(counterpartyName)
                .createdAt(tx.getCreatedAt())
                .build();
    }

    /**
     * Generate a random alphanumeric string of the given length.
     * Uses SecureRandom for cryptographic-quality randomness.
     */
    private String generateRandomAlphanumeric(int length) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
