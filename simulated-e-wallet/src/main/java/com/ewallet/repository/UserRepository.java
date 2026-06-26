package com.ewallet.repository;

import com.ewallet.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User entity operations.
 *
 * Design rationale:
 * - findByUsername() is essential for authentication (login) — the
 *   Security layer looks up users by their unique username to verify
 *   credentials and generate JWT tokens.
 * - existsByUsername() and existsByEmail() are used during registration
 *   to enforce uniqueness constraints BEFORE persisting, returning a
 *   proper 409 Conflict rather than relying on a database constraint violation.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Retrieve a user by their unique username.
     * Used primarily by JwtAuthenticationFilter and AuthService during login.
     */
    Optional<User> findByUsername(String username);

    /**
     * Check if a username is already taken (registration validation).
     */
    boolean existsByUsername(String username);

    /**
     * Check if an email is already registered (registration validation).
     */
    boolean existsByEmail(String email);
}
