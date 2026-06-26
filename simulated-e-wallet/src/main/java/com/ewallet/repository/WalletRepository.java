package com.ewallet.repository;

import com.ewallet.entity.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Wallet entity operations.
 *
 * Concurrency Design (CRITICAL):
 * This repository is the cornerstone of data consistency.
 *
 * PESSIMISTIC_WRITE locking:
 * - findByIdWithLock() uses SELECT ... FOR UPDATE to acquire an
 *   exclusive row-level lock on the wallet record.
 * - While the lock is held, no other transaction can read or write
 *   that row, preventing phantom reads, lost updates, and race conditions.
 *
 * Deadlock prevention:
 * - Callers MUST sort wallet UUIDs in ascending order before acquiring
 *   locks (smaller UUID first, larger UUID second). This guarantees a
 *   global lock-ordering convention that eliminates mutual deadlocks.
 *
 * findByUserId() is lock-free because it is used for read-only queries
 * (e.g., fetching balance for display) where strict isolation is not required.
 */
@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    /**
     * Look up a wallet by the owning user's ID (read-only, no lock).
     * Used for non-critical reads like balance enquiries.
     */
    Optional<Wallet> findByUserId(UUID userId);

    /**
     * Find a wallet by ID with an exclusive pessimistic write lock.
     * This acquires SELECT ... FOR UPDATE at the database level.
     *
     * Used during fund transfers and balance mutations where
     * absolute isolation is required to prevent race conditions.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.id = :id")
    Optional<Wallet> findByIdWithLock(@Param("id") UUID id);
}
