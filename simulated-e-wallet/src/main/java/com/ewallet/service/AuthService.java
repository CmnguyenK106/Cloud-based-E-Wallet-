package com.ewallet.service;

import com.ewallet.dto.request.LoginRequest;
import com.ewallet.dto.request.RegisterRequest;
import com.ewallet.dto.response.AuthResponse;
import com.ewallet.entity.User;
import com.ewallet.entity.Wallet;
import com.ewallet.exception.DuplicateResourceException;
import com.ewallet.repository.UserRepository;
import com.ewallet.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Authentication service — handles user registration and login.
 *
 * Registration flow:
 * 1. Validate uniqueness (username + email)
 * 2. Hash password with BCrypt
 * 3. Create User entity
 * 4. Create associated Wallet with zero balance
 * 5. Generate JWT
 * 6. Return AuthResponse
 *
 * Login flow:
 * 1. Look up user by username
 * 2. Verify BCrypt hash
 * 3. Generate JWT
 * 4. Return AuthResponse
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Register a new user account with an auto-initialised wallet.
     *
     * @param request  Registration payload (validated by controller)
     * @return  AuthResponse with JWT token and user profile
     * @throws DuplicateResourceException if username or email already exists
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 1. Enforce uniqueness constraints
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException(
                    "Username '" + request.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(
                    "Email '" + request.getEmail() + "' is already registered");
        }

        // 2. Hash the password with BCrypt
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        // 3. Build the User entity (Wallet is cascade-persisted)
        User user = User.builder()
                .username(request.getUsername())
                .password(hashedPassword)
                .fullName(request.getFullName())
                .email(request.getEmail())
                .build();

        // 4. Build and associate the Wallet (cascade = ALL on User.wallet)
        Wallet wallet = Wallet.builder()
                .user(user)
                .balance(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                .currency("VND")
                .build();
        user.setWallet(wallet);

        userRepository.save(user);

        // 5. Generate JWT
        String token = jwtTokenProvider.generateToken(user.getId());

        log.info("User registered: username={}, userId={}", user.getUsername(), user.getId());

        // 6. Build response
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .build();
    }

    /**
     * Authenticate an existing user with username + password.
     *
     * @param request  Login payload
     * @return  AuthResponse with JWT token and user profile
     * @throws NotAuthorizedException if credentials are invalid
     */
    public AuthResponse login(LoginRequest request) {
        // 1. Look up user
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid username or password"));

        // 2. Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        // 3. Generate JWT
        String token = jwtTokenProvider.generateToken(user.getId());

        log.info("User logged in: username={}, userId={}", user.getUsername(), user.getId());

        // 4. Build response
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .build();
    }
}
