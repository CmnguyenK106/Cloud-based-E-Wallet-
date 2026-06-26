package com.ewallet.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Wallet entity representing a user's digital balance store.
 *
 * Concurrency & Precision Design:
 * - Uses BigDecimal (precision=19, scale=4) — NEVER float/double —
 *   to guarantee exact arithmetic for financial calculations.
 * - balance is modified ONLY through the service layer with
 *   @Lock(PESSIMISTIC_WRITE) to prevent race conditions.
 * - currency defaults to "VND" (Vietnam Dong) as a 3-letter ISO code.
 * - @UpdateTimestamp automatically refreshes updated_at on every write.
 */
@Entity
@Table(name = "wallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Owning side of the one-to-one relationship.
     * The foreign key "user_id" references the users table.
     * Unique constraint ensures one wallet per user.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /**
     * Current wallet balance.
     * precision=19, scale=4 → supports up to 15 trillion VND with 4 decimal places.
     * ALWAYS manipulated via BigDecimal — never double/float.
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    /**
     * ISO 4217 3-letter currency code (e.g., "VND", "USD").
     * Defaults to "VND" for this system.
     */
    @Column(nullable = false, length = 3)
    private String currency;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
