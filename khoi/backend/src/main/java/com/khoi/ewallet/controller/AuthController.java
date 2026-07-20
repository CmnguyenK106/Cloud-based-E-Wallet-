package com.khoi.ewallet.controller;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^0[0-9]{9}$");
    private static final BigDecimal INITIAL_BALANCE = new BigDecimal("10.00");

    private final JdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        String phone = request.phone() == null ? "" : request.phone().trim();
        String password = request.password() == null ? "" : request.password();
        String fullName = request.fullName() == null ? "" : request.fullName().trim();

        String validationError = validateRegisterRequest(phone, password, fullName);
        if (validationError != null) {
            return error(validationError, HttpStatus.BAD_REQUEST);
        }

        Integer existingUsers = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE phone = ?",
                Integer.class,
                phone
        );

        if (existingUsers != null && existingUsers > 0) {
            return error("Phone already exists", HttpStatus.CONFLICT);
        }

        try {
            String passwordHash = passwordEncoder.encode(password);
            KeyHolder userKeyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO users (phone, password, role, status) VALUES (?, ?, 'user', 'active')",
                        Statement.RETURN_GENERATED_KEYS
                );
                statement.setString(1, phone);
                statement.setString(2, passwordHash);
                return statement;
            }, userKeyHolder);

            int userId = Objects.requireNonNull(userKeyHolder.getKey()).intValue();

            jdbcTemplate.update(
                    "INSERT INTO user_profiles (user_id, full_name) VALUES (?, ?)",
                    userId,
                    fullName
            );

            jdbcTemplate.update(
                    "INSERT INTO wallets (user_id, balance) VALUES (?, ?)",
                    userId,
                    INITIAL_BALANCE
            );

            Map<String, Object> user = findUserByPhone(phone);
            Map<String, Object> wallet = findWalletByUserId(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Register successfully");
            response.put("token", buildDemoToken(userId));
            response.put("user", user);
            response.put("wallet", wallet);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (DuplicateKeyException exception) {
            return error("Phone already exists", HttpStatus.CONFLICT);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        String phone = request.phone() == null ? "" : request.phone().trim();
        String password = request.password() == null ? "" : request.password();

        if (!PHONE_PATTERN.matcher(phone).matches()) {
            return error("Phone must match ^0[0-9]{9}$", HttpStatus.BAD_REQUEST);
        }

        if (password.isBlank()) {
            return error("Password is required", HttpStatus.BAD_REQUEST);
        }

        List<Map<String, Object>> users = jdbcTemplate.queryForList(
                """
                SELECT
                    u.id,
                    u.phone,
                    u.password,
                    u.role,
                    u.status,
                    COALESCE(up.full_name, ap.full_name) AS full_name,
                    ap.position
                FROM users u
                LEFT JOIN user_profiles up ON u.id = up.user_id
                LEFT JOIN admin_profiles ap ON u.id = ap.user_id
                WHERE u.phone = ?
                LIMIT 1
                """,
                phone
        );

        if (users.isEmpty()) {
            return error("Invalid phone or password", HttpStatus.UNAUTHORIZED);
        }

        Map<String, Object> userRow = users.get(0);
        String storedHash = String.valueOf(userRow.get("password"));
        if (!passwordEncoder.matches(password, storedHash)) {
            return error("Invalid phone or password", HttpStatus.UNAUTHORIZED);
        }

        if ("blocked".equalsIgnoreCase(String.valueOf(userRow.get("status")))) {
            return error("User is blocked", HttpStatus.FORBIDDEN);
        }

        int userId = ((Number) userRow.get("id")).intValue();
        Map<String, Object> user = publicUser(userRow);
        Map<String, Object> wallet = findWalletByUserId(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Login successfully");
        response.put("token", buildDemoToken(userId));
        response.put("user", user);
        response.put("wallet", wallet);
        return ResponseEntity.ok(response);
    }

    private String validateRegisterRequest(String phone, String password, String fullName) {
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            return "Phone must match ^0[0-9]{9}$";
        }

        if (password.length() < 6) {
            return "Password must be at least 6 characters";
        }

        if (fullName.isBlank()) {
            return "Full name is required";
        }

        return null;
    }

    private Map<String, Object> findUserByPhone(String phone) {
        Map<String, Object> userRow = jdbcTemplate.queryForMap(
                """
                SELECT
                    u.id,
                    u.phone,
                    u.role,
                    u.status,
                    up.full_name
                FROM users u
                LEFT JOIN user_profiles up ON u.id = up.user_id
                WHERE u.phone = ?
                """,
                phone
        );

        return publicUser(userRow);
    }

    private Map<String, Object> publicUser(Map<String, Object> userRow) {
        Map<String, Object> user = new HashMap<>();
        user.put("id", userRow.get("id"));
        user.put("phone", userRow.get("phone"));
        user.put("role", userRow.get("role"));
        user.put("status", userRow.get("status"));
        user.put("fullName", userRow.get("full_name"));
        if ("admin".equalsIgnoreCase(String.valueOf(userRow.get("role")))) {
            user.put("position", userRow.get("position"));
        }
        return user;
    }

    private Map<String, Object> findWalletByUserId(int userId) {
        List<Map<String, Object>> wallets = jdbcTemplate.queryForList(
                "SELECT id, user_id, balance FROM wallets WHERE user_id = ? LIMIT 1",
                userId
        );

        if (wallets.isEmpty()) {
            return null;
        }

        Map<String, Object> walletRow = wallets.get(0);
        Map<String, Object> wallet = new HashMap<>();
        wallet.put("id", walletRow.get("id"));
        wallet.put("userId", walletRow.get("user_id"));
        wallet.put("balance", walletRow.get("balance"));
        return wallet;
    }

    private String buildDemoToken(int userId) {
        return "demo-token-" + userId;
    }

    private ResponseEntity<Map<String, Object>> error(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", message);
        return ResponseEntity.status(status).body(response);
    }

    public record RegisterRequest(String phone, String password, String fullName) {
    }

    public record LoginRequest(String phone, String password) {
    }
}
