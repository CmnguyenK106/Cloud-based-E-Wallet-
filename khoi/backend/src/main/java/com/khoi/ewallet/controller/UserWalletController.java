package com.khoi.ewallet.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/user/wallet")
@CrossOrigin(origins = "http://localhost:5173")
public class UserWalletController {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^0[0-9]{9}$");
    private static final BigDecimal MAX_TRANSFER_AMOUNT = new BigDecimal("10000000");
    private static final DateTimeFormatter TRANSACTION_CODE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final JdbcTemplate jdbcTemplate;

    public UserWalletController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMyWallet(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        AuthResult authResult = authenticate(authorizationHeader);
        if (authResult.errorResponse() != null) {
            return authResult.errorResponse();
        }

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                SELECT
                    u.id,
                    u.phone,
                    u.role,
                    u.status,
                    up.full_name,
                    w.id AS wallet_id,
                    w.balance
                FROM users u
                JOIN user_profiles up ON u.id = up.user_id
                JOIN wallets w ON u.id = w.user_id
                WHERE u.id = ?
                LIMIT 1
                """,
                authResult.user().id()
        );

        if (rows.isEmpty()) {
            return error("Wallet not found", HttpStatus.BAD_REQUEST);
        }

        Map<String, Object> row = rows.get(0);
        Map<String, Object> response = new HashMap<>();
        response.put("user", buildUser(row));
        response.put("wallet", buildWallet(row));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/transfer")
    @Transactional
    public ResponseEntity<Map<String, Object>> transferMoney(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestBody TransferRequest request
    ) {
        AuthResult authResult = authenticate(authorizationHeader);
        if (authResult.errorResponse() != null) {
            return authResult.errorResponse();
        }

        String receiverPhone = request.receiverPhone() == null ? "" : request.receiverPhone().trim();
        BigDecimal amount = request.amount();

        if (!PHONE_PATTERN.matcher(receiverPhone).matches()) {
            return error("Receiver phone invalid", HttpStatus.BAD_REQUEST);
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0
                || amount.compareTo(MAX_TRANSFER_AMOUNT) > 0) {
            return error("Amount invalid", HttpStatus.BAD_REQUEST);
        }

        List<Map<String, Object>> receivers = jdbcTemplate.queryForList(
                """
                SELECT
                    u.id,
                    u.phone,
                    u.status,
                    w.id AS wallet_id,
                    w.balance
                FROM users u
                JOIN user_profiles up ON u.id = up.user_id
                JOIN wallets w ON u.id = w.user_id
                WHERE u.phone = ?
                LIMIT 1
                """,
                receiverPhone
        );

        if (receivers.isEmpty()) {
            return error("Receiver does not exist", HttpStatus.BAD_REQUEST);
        }

        Map<String, Object> receiver = receivers.get(0);
        int receiverUserId = ((Number) receiver.get("id")).intValue();

        if (receiverUserId == authResult.user().id()) {
            return error("Sender cannot transfer to self", HttpStatus.BAD_REQUEST);
        }

        List<Map<String, Object>> senderWallets = jdbcTemplate.queryForList(
                "SELECT id, balance FROM wallets WHERE user_id = ? FOR UPDATE",
                authResult.user().id()
        );

        if (senderWallets.isEmpty()) {
            return error("Wallet not found", HttpStatus.BAD_REQUEST);
        }

        Map<String, Object> senderWallet = senderWallets.get(0);
        int senderWalletId = ((Number) senderWallet.get("id")).intValue();
        int receiverWalletId = ((Number) receiver.get("wallet_id")).intValue();
        BigDecimal balanceBefore = (BigDecimal) senderWallet.get("balance");

        if (balanceBefore.compareTo(amount) < 0) {
            return error("Balance is not enough", HttpStatus.BAD_REQUEST);
        }

        BigDecimal balanceAfter = balanceBefore.subtract(amount);
        String description = request.description() == null || request.description().isBlank()
                ? "Transfer money"
                : request.description().trim();
        String transactionCode = buildTransactionCode(authResult.user().id());

        jdbcTemplate.update(
                "UPDATE wallets SET balance = balance - ? WHERE id = ?",
                amount,
                senderWalletId
        );

        jdbcTemplate.update(
                "UPDATE wallets SET balance = balance + ? WHERE id = ?",
                amount,
                receiverWalletId
        );

        jdbcTemplate.update(
                """
                INSERT INTO transactions (
                    transaction_code,
                    sender_wallet_id,
                    receiver_wallet_id,
                    service_id,
                    amount,
                    balance_before,
                    balance_after,
                    type,
                    status,
                    description,
                    created_by
                )
                VALUES (?, ?, ?, NULL, ?, ?, ?, 'transfer', 'success', ?, ?)
                """,
                transactionCode,
                senderWalletId,
                receiverWalletId,
                amount,
                balanceBefore,
                balanceAfter,
                description,
                authResult.user().id()
        );

        Map<String, Object> transaction = new HashMap<>();
        transaction.put("transactionCode", transactionCode);
        transaction.put("amount", amount);
        transaction.put("receiverPhone", receiverPhone);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Transfer successfully");
        response.put("balance", balanceAfter);
        response.put("transaction", transaction);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/transactions")
    public ResponseEntity<Map<String, Object>> getTransactions(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        AuthResult authResult = authenticate(authorizationHeader);
        if (authResult.errorResponse() != null) {
            return authResult.errorResponse();
        }

        List<Map<String, Object>> wallets = jdbcTemplate.queryForList(
                "SELECT id FROM wallets WHERE user_id = ? LIMIT 1",
                authResult.user().id()
        );

        if (wallets.isEmpty()) {
            return error("Wallet not found", HttpStatus.BAD_REQUEST);
        }

        int walletId = ((Number) wallets.get(0).get("id")).intValue();

        List<Map<String, Object>> transactionRows = jdbcTemplate.queryForList(
                """
                SELECT
                    t.id,
                    t.transaction_code,
                    t.type,
                    sender_user.phone AS sender_phone,
                    receiver_user.phone AS receiver_phone,
                    s.name AS service_name,
                    t.amount,
                    t.balance_before,
                    t.balance_after,
                    t.status,
                    t.description,
                    t.created_at
                FROM transactions t
                LEFT JOIN wallets sender_wallet ON t.sender_wallet_id = sender_wallet.id
                LEFT JOIN users sender_user ON sender_wallet.user_id = sender_user.id
                LEFT JOIN wallets receiver_wallet ON t.receiver_wallet_id = receiver_wallet.id
                LEFT JOIN users receiver_user ON receiver_wallet.user_id = receiver_user.id
                LEFT JOIN services s ON t.service_id = s.id
                WHERE t.sender_wallet_id = ?
                   OR t.receiver_wallet_id = ?
                ORDER BY t.created_at DESC
                """,
                walletId,
                walletId
        );

        List<Map<String, Object>> transactions = transactionRows.stream()
                .map(this::buildTransaction)
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("transactions", transactions);
        return ResponseEntity.ok(response);
    }

    private AuthResult authenticate(String authorizationHeader) {
        Integer userId = extractUserId(authorizationHeader);
        if (userId == null) {
            return new AuthResult(null, error("Unauthorized", HttpStatus.UNAUTHORIZED));
        }

        List<Map<String, Object>> users = jdbcTemplate.queryForList(
                "SELECT id, status FROM users WHERE id = ? LIMIT 1",
                userId
        );

        if (users.isEmpty()) {
            return new AuthResult(null, error("Unauthorized", HttpStatus.UNAUTHORIZED));
        }

        Map<String, Object> user = users.get(0);
        String status = String.valueOf(user.get("status"));

        if ("blocked".equalsIgnoreCase(status)) {
            return new AuthResult(null, error("Account blocked", HttpStatus.FORBIDDEN));
        }

        if (!"active".equalsIgnoreCase(status)) {
            return new AuthResult(null, error("Unauthorized", HttpStatus.UNAUTHORIZED));
        }

        return new AuthResult(new AuthenticatedUser(userId), null);
    }

    private Integer extractUserId(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }

        String token = authorizationHeader.substring("Bearer ".length()).trim();
        if (!token.startsWith("demo-token-")) {
            return null;
        }

        String tokenValue = token.substring("demo-token-".length());
        String userIdText = tokenValue.contains("-")
                ? tokenValue.substring(0, tokenValue.indexOf("-"))
                : tokenValue;

        try {
            return Integer.parseInt(userIdText);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String buildTransactionCode(int userId) {
        return "TXN" + LocalDateTime.now().format(TRANSACTION_CODE_FORMAT) + userId;
    }

    private Map<String, Object> buildUser(Map<String, Object> row) {
        Map<String, Object> user = new HashMap<>();
        user.put("id", row.get("id"));
        user.put("phone", row.get("phone"));
        user.put("fullName", row.get("full_name"));
        user.put("role", row.get("role"));
        user.put("status", row.get("status"));
        return user;
    }

    private Map<String, Object> buildWallet(Map<String, Object> row) {
        Map<String, Object> wallet = new HashMap<>();
        wallet.put("id", row.get("wallet_id"));
        wallet.put("balance", row.get("balance"));
        return wallet;
    }

    private Map<String, Object> buildTransaction(Map<String, Object> row) {
        Map<String, Object> transaction = new HashMap<>();
        transaction.put("id", row.get("id"));
        transaction.put("transactionCode", row.get("transaction_code"));
        transaction.put("type", row.get("type"));
        transaction.put("senderPhone", row.get("sender_phone"));
        transaction.put("receiverPhone", row.get("receiver_phone"));
        transaction.put("serviceName", row.get("service_name"));
        transaction.put("amount", row.get("amount"));
        transaction.put("balanceBefore", row.get("balance_before"));
        transaction.put("balanceAfter", row.get("balance_after"));
        transaction.put("status", row.get("status"));
        transaction.put("description", row.get("description"));
        transaction.put("createdAt", row.get("created_at"));
        return transaction;
    }

    private ResponseEntity<Map<String, Object>> error(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", message);
        return ResponseEntity.status(status).body(response);
    }

    private record AuthenticatedUser(int id) {
    }

    private record AuthResult(
            AuthenticatedUser user,
            ResponseEntity<Map<String, Object>> errorResponse
    ) {
    }

    public record TransferRequest(String receiverPhone, BigDecimal amount, String description) {
    }
}
