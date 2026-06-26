package com.ewallet.service;

import com.ewallet.dto.request.TransferRequest;
import com.ewallet.dto.response.ApiResponse;
import com.ewallet.dto.response.BalanceResponse;
import com.ewallet.entity.*;
import com.ewallet.exception.InsufficientBalanceException;
import com.ewallet.exception.InvalidIdempotencyKeyException;
import com.ewallet.repository.UserRepository;
import com.ewallet.repository.WalletRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Wallet service — the core of financial business logic.
 *
 * Concurrency & Deadlock Prevention (CRITICAL):
 *
 * 1. PESSIMISTIC WRITE LOCKING:
 *    All balance mutations go through WalletRepository.findByIdWithLock(),
 *    which executes SELECT ... FOR UPDATE at the database level.
 *    This ensures that between reading the balance and writing the update,
 *    no other transaction can modify that row.
 *
 * 2. DEADLOCK PREVENTION (Ordered Lock Acquisition):
 *    During P2P transfers, both wallets must be locked. To prevent
 *    mutual deadlocks (Tx1: lock A→B, Tx2: lock B→A), we ALWAYS
 *    lock wallets in ASCENDING UUID order:
 *      - Smaller UUID locked first
 *      - Larger UUID locked second
 *    This guarantees a global lock-ordering convention.
 *
 * 3. DOUBLE-ENTRY LEDGER:
 *    Balance modifications are NEVER performed in isolation. Every
 *    debit or credit is coupled with an immutable Transaction row
 *    created via TransactionService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final TransactionService transactionService;
    private final IdempotencyService idempotencyService;

    /**
     * Retrieve the balance for the authenticated user's wallet.
     */
    @Transactional(readOnly = true)
    public BalanceResponse getBalance(UUID userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Wallet not found for user: " + userId));

        return BalanceResponse.builder()
                .walletId(wallet.getId())
                .balance(wallet.getBalance())
                .currency(wallet.getCurrency())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }

    /**
     * Top up (deposit funds into) the authenticated user's wallet.
     *
     * @param userId   The authenticated user's UUID
     * @param amount   The amount to deposit (must be > 0)
     * @return  Updated BalanceResponse
     */
    @Transactional
    public BalanceResponse topUp(UUID userId, BigDecimal amount) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Wallet not found for user: " + userId));

        // Lock the wallet for writing
        Wallet lockedWallet = walletRepository.findByIdWithLock(wallet.getId())
                .orElseThrow(() -> new EntityNotFoundException("Wallet not found: " + wallet.getId()));

        // Credit the balance
        lockedWallet.setBalance(lockedWallet.getBalance().add(amount));

        // Create audit ledger entry (TOPUP)
        transactionService.createTransaction(
                null,               // No sender for top-up
                lockedWallet,       // Receiver is the wallet owner
                amount,
                TransactionType.TOPUP,
                TransactionStatus.SUCCESS,
                "Wallet top-up"
        );

        walletRepository.save(lockedWallet);
        log.info("Top-up: userId={}, amount={}, newBalance={}",
                userId, amount, lockedWallet.getBalance());

        return BalanceResponse.builder()
                .walletId(lockedWallet.getId())
                .balance(lockedWallet.getBalance())
                .currency(lockedWallet.getCurrency())
                .updatedAt(lockedWallet.getUpdatedAt())
                .build();
    }

    /**
     * Execute a P2P fund transfer with full concurrency protection.
     *
     * Idempotency flow:
     * 1. Check X-Idempotency-Key — return cached result if duplicate
     * 2. Process the transfer
     * 3. Cache the result
     *
     * Locking protocol (deadlock prevention):
     * 1. Resolve sender + receiver wallets
     * 2. Sort wallet UUIDs ascending
     * 3. Lock smaller UUID first, larger UUID second
     * 4. Perform the transfer inside the same transaction
     *
     * @param senderUserId     The authenticated sender's UUID
     * @param request          Transfer details (receiver, amount, description)
     * @param idempotencyKey   Unique key to prevent duplicate processing
     * @return  ApiResponse with success message
     */
    @Transactional
    public ApiResponse<String> transfer(UUID senderUserId,
                                        TransferRequest request,
                                        String idempotencyKey) {

        // ── Idempotency check ──────────────────────────────────────────
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new InvalidIdempotencyKeyException("X-Idempotency-Key header is required");
        }

        Object cached = idempotencyService.getCachedResult(idempotencyKey);
        if (cached != null) {
            log.info("Idempotency hit: returning cached result for key={}", idempotencyKey);
            return ApiResponse.success("Transfer already processed");
        }

        // ── Resolve wallets ────────────────────────────────────────────
        Wallet senderWallet = walletRepository.findByUserId(senderUserId)
                .orElseThrow(() -> new EntityNotFoundException("Sender wallet not found"));

        User receiverUser = userRepository.findByUsername(request.getReceiverUsername())
                .orElseThrow(() -> new EntityNotFoundException("Receiver not found: " + request.getReceiverUsername()));

        Wallet receiverWallet = walletRepository.findByUserId(receiverUser.getId())
                .orElseThrow(() -> new EntityNotFoundException("Receiver wallet not found"));

        // Prevent self-transfer
        if (senderWallet.getId().equals(receiverWallet.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Cannot transfer to yourself");
        }

        // ── DEADLOCK PREVENTION: Sort UUIDs ascending ──────────────────
        // This is the critical ordering: we always lock the smaller UUID
        // first and the larger UUID second. This eliminates the possibility
        // of a circular wait between concurrent transactions involving the
        // same two wallets.
        Wallet firstLock  = Stream.of(senderWallet, receiverWallet)
                .min(Comparator.comparing(Wallet::getId))
                .orElseThrow();
        Wallet secondLock = Stream.of(senderWallet, receiverWallet)
                .max(Comparator.comparing(Wallet::getId))
                .orElseThrow();

        // Lock the first wallet (smaller UUID)
        Wallet lockedFirst = walletRepository.findByIdWithLock(firstLock.getId())
                .orElseThrow(() -> new EntityNotFoundException("Wallet not found: " + firstLock.getId()));

        // Lock the second wallet (larger UUID)
        Wallet lockedSecond = walletRepository.findByIdWithLock(secondLock.getId())
                .orElseThrow(() -> new EntityNotFoundException("Wallet not found: " + secondLock.getId()));

        // ── Re-identify sender/receiver after locking ──────────────────
        Wallet lockedSender   = lockedFirst.getId().equals(senderWallet.getId())
                ? lockedFirst : lockedSecond;
        Wallet lockedReceiver = lockedFirst.getId().equals(receiverWallet.getId())
                ? lockedFirst : lockedSecond;

        // ── Validate and execute transfer ──────────────────────────────
        BigDecimal amount = request.getAmount();

        if (lockedSender.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance. Available: " + lockedSender.getBalance()
                            + ", Required: " + amount);
        }

        // Debit sender
        lockedSender.setBalance(lockedSender.getBalance().subtract(amount));
        // Credit receiver
        lockedReceiver.setBalance(lockedReceiver.getBalance().add(amount));

        // ── Create double-entry ledger entries ─────────────────────────
        // TRANSFER entry (sender's perspective)
        transactionService.createTransaction(
                lockedSender, lockedReceiver, amount,
                TransactionType.TRANSFER, TransactionStatus.SUCCESS,
                request.getDescription() != null ? request.getDescription() : "P2P transfer"
        );

        // RECEIVE entry (receiver's perspective)
        transactionService.createTransaction(
                lockedSender, lockedReceiver, amount,
                TransactionType.RECEIVE, TransactionStatus.SUCCESS,
                request.getDescription() != null ? request.getDescription() : "P2P transfer"
        );

        // Persist both wallets
        walletRepository.save(lockedSender);
        walletRepository.save(lockedReceiver);

        log.info("Transfer completed: sender={}, receiver={}, amount={}",
                senderUserId, receiverUser.getId(), amount);

        // ── Cache idempotency result ───────────────────────────────────
        idempotencyService.cacheResult(idempotencyKey, "processed");

        return ApiResponse.success("Transfer successful");
    }
}
