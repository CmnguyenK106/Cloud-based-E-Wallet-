package com.ewallet.repository;

import com.ewallet.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Transaction entity operations.
 *
 * Design rationale:
 * - Transactions are append-only immutable records (the ledger).
 * - findByReferenceNumber() enables idempotency checks — before
 *   processing a transfer, the service checks if a transaction with
 *   the given idempotency key's derived reference number already exists.
 * - findBySenderWalletIdOrReceiverWalletIdOrderByCreatedAtDesc()
 *   provides paginated transaction history for a user's wallet,
 *   showing both sent and received entries sorted newest-first.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    /**
     * Find a transaction by its unique reference number.
     * Used for idempotency checks and customer support lookups.
     */
    Optional<Transaction> findByReferenceNumber(String referenceNumber);

    /**
     * Retrieve paginated transaction history for a wallet.
     * Returns transactions where the wallet is either the sender
     * or the receiver, ordered newest-first.
     */
    Page<Transaction> findBySenderWalletIdOrReceiverWalletIdOrderByCreatedAtDesc(
            UUID senderWalletId,
            UUID receiverWalletId,
            Pageable pageable
    );
}
