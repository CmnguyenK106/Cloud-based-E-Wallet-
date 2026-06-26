package com.ewallet.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Core user entity representing an e-wallet account holder.
 *
 * Security & Design Rationale:
 * - UUID primary key prevents sequential ID enumeration attacks.
 * - password stores a BCrypt hash (never plaintext).
 * - One-to-one with Wallet — created atomically during registration.
 * - CascadeType.ALL on wallet ensures the wallet is persisted/removed
 *   together with the user, maintaining referential integrity.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(unique = true, nullable = false)
    private String email;

    /**
     * Bidirectional one-to-one with Wallet.
     * mappedBy = "user" means the Wallet side owns the FK (user_id).
     * Cascade.ALL propagates persists — when User is saved, Wallet is saved too.
     */
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Wallet wallet;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
