You are an expert FinTech Backend Engineer and AWS Cloud Solutions Architect. You are being onboarded to help me build a "Simulated Cloud-Based E-Wallet Application" under a strict 3-week timeline. 

Your objective is to ingest the following system context, architecture requirements, and pre-defined data models. Do not write any code yet. Acknowledge this context at the end and wait for my specific module tasks.

---

### 1. SYSTEM ARCHITECTURE & TECH STACK

- **Frontend:** Single-Page Application (SPA) driven by a single `index.html` file, styled with utility-first Tailwind CSS via CDN, and powered by native Vanilla JavaScript (Fetch API). Hosted entirely via AWS S3 Static Website Hosting.
- **API Proxy Layer:** AWS API Gateway acting as the public entry point. CORS must be explicitly configured to allow the S3 domain to safely execute HTTP requests.
- **Backend Infrastructure:** Java 17+ with Spring Boot 3.x (Dependencies: Spring Web, Spring Data JPA, Spring Security, Validation). Deployed on an AWS EC2 instance (`t3.micro` Ubuntu environment) running as a persistent background service via systemd or nohup.
- **Database Storage:** AWS RDS PostgreSQL (`db.t3.micro`, Single-AZ deployment). It serves as the single source of truth enforcing strict ACID properties.
- **Monitoring & Logging:** AWS CloudWatch tracking application runtime streams. The Spring Boot logging engine will output structured event metrics into a designated `spring.log` file, which is actively captured by the CloudWatch Agent.
- **Document Store:** AWS S3 Private Bucket used to isolate compiled transaction records (PDF/CSV format).

---

### 2. CORE FINANCIAL & BUSINESS LOGIC RULES (NON-NEGOTIABLE)

- **Strict Data Consistency & Concurrency Control:** System fund transfers must enforce a data-safe isolation state. Database mutations must use Pessimistic Locking (`@Lock(LockModeType.PESSIMISTIC_WRITE)`) on the wallet records.
- **Deadlock Mitigation Strategy:** During P2P transfers, to prevent multi-threaded mutual deadlocks, row-locking operations MUST execute sequentially based on the Wallet UUIDs sorted in ascending order (smaller UUID locked first, larger UUID locked second).
- **Auditability / Double-Entry Ledger:** Direct balance modifications are strictly prohibited. Every balance increment or decrement must be coupled with an immutable audit entry inserted into the `transactions` ledger table.
- **Currency Data Precision:** You must never use floating-point primitive values (`float`, `double`) for transactional currency handling. All balance calculations must utilize `java.math.BigDecimal` with a database schema definition of `precision = 19, scale = 4`.
- **Idempotency Protection:** The fund transfer endpoint must require and validate an `X-Idempotency-Key` header parameter. Duplicate execution requests utilizing the same key within a 5-minute window must return the cached result immediately without mutating balances again.
- **Cryptographic Security:** User access management must utilize BCrypt for secure password hashing. Authenticated sessions must issue state-independent JSON Web Tokens (JWT) mapped to the `Authorization: Bearer <token>` HTTP request header.

---

### 3. BASE DATA MODEL SPECIFICATIONS (JPA ENTITIES)

The database schema maps exactly to the following Hibernate configurations:

```java
package com.ewallet.entity;

public enum TransactionType { TRANSFER, RECEIVE, TOPUP, WITHDRAW }

public enum TransactionStatus { PENDING, SUCCESS, FAILED }

```

```java
package com.ewallet.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(unique = true, nullable = false, length = 50)
    private String username;
    @Column(nullable = false)
    private String password;
    @Column(name = "full_name", nullable = false)
    private String fullName;
    @Column(unique = true, nullable = false)
    private String email;
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Wallet wallet;
    @CreationTimestamp @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

```

```java
package com.ewallet.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "wallets")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Wallet {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;
    @Column(nullable = false, length = 3)
    private String currency; // Defaults to "VND"
    @UpdateTimestamp @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

```

```java
package com.ewallet.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_tx_sender_wallet", columnList = "sender_wallet_id"),
    @Index(name = "idx_tx_receiver_wallet", columnList = "receiver_wallet_id"),
    @Index(name = "idx_tx_ref_num", columnList = "reference_number")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Transaction {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "reference_number", unique = true, nullable = false, length = 20)
    private String referenceNumber;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "sender_wallet_id")
    private Wallet senderWallet;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "receiver_wallet_id")
    private Wallet receiverWallet;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private TransactionType type;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private TransactionStatus status;
    @Column(length = 255)
    private String description;
    @CreationTimestamp @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

```

---

### 4. REGISTER & LOGIN — AUTHENTICATION FLOW SPECIFICATION

#### 4.1 Registration — `POST /api/auth/register`

**Purpose:** Create a new user account with an auto-initialized wallet.

**Request Body (JSON):**
```json
{
  "username": "johndoe",
  "password": "SecurePass123!",
  "fullName": "John Doe",
  "email": "john@example.com"
}
```

**Validation Rules:**
- `username` — required, 3–50 chars, must be unique
- `password` — required, 8–100 chars, must contain at least one uppercase letter, one lowercase letter, and one digit
- `fullName` — required, 1–100 chars
- `email` — required, valid email format, must be unique

**Server-Side Processing:**
1. Validate input constraints (Spring `@Valid`)
2. Check username & email uniqueness — throw `409 Conflict` if taken
3. Hash password with `BCryptPasswordEncoder` (strength = 10)
4. Create `User` entity → persist
5. Create associated `Wallet` entity with `balance = 0.0000`, `currency = "VND"` → persist
6. Generate JWT access token (subject = user UUID, expiry = 24h)
7. Return `201 Created` response with JWT + user profile

**Success Response (201):**
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "userId": "uuid-here",
    "username": "johndoe",
    "fullName": "John Doe",
    "email": "john@example.com"
  }
}
```

**Error Responses:**
- `400 Bad Request` — validation failures (e.g., password too short)
- `409 Conflict` — username or email already exists

#### 4.2 Login — `POST /api/auth/login`

**Purpose:** Authenticate existing credentials and issue a new JWT.

**Request Body (JSON):**
```json
{
  "username": "johndoe",
  "password": "SecurePass123!"
}
```

**Validation Rules:**
- `username` — required, non-blank
- `password` — required, non-blank

**Server-Side Processing:**
1. Look up `User` by `username` — throw `401 Unauthorized` if not found
2. Verify password with `BCryptPasswordEncoder.matches(raw, hash)` — throw `401 Unauthorized` if mismatch
3. Generate fresh JWT access token (subject = user UUID, expiry = 24h)
4. Return `200 OK` with token + user profile

**Success Response (200):**
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "userId": "uuid-here",
    "username": "johndoe",
    "fullName": "John Doe",
    "email": "john@example.com"
  }
}
```

**Error Responses:**
- `400 Bad Request` — missing fields
- `401 Unauthorized` — invalid credentials

#### 4.3 JWT Token Specification

| Field | Value |
|---|---|
| Signing Algorithm | HMAC-SHA256 (`HS256`) |
| Secret Key | 256-bit base64-encoded string (externalized in `application.properties`) |
| Claims | `sub` = user UUID, `iat` = issued-at, `exp` = issued-at + 24h |
| Header Format | `Authorization: Bearer <token>` |
| Token Expiry | 24 hours from issuance |

#### 4.4 Request DTO Definitions

```java
package com.ewallet.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class RegisterRequest {
    @NotBlank @Size(min = 3, max = 50)
    private String username;
    @NotBlank @Size(min = 8, max = 100)
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
             message = "Password must contain uppercase, lowercase, and digit")
    private String password;
    @NotBlank @Size(max = 100)
    private String fullName;
    @NotBlank @Email @Size(max = 255)
    private String email;
}

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class LoginRequest {
    @NotBlank
    private String username;
    @NotBlank
    private String password;
}
```

#### 4.5 Response DTO Definitions

```java
package com.ewallet.dto.response;

import lombok.*;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuthResponse {
    private String token;
    private String tokenType;       // "Bearer"
    private UUID userId;
    private String username;
    private String fullName;
    private String email;
}

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private String message;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message);
    }
}
```

---

### 5. PACKAGE ARCHITECTURE

```
com.ewallet.
├── entity/                  # JPA Entities + Enums
│   ├── User.java
│   ├── Wallet.java
│   ├── Transaction.java
│   ├── TransactionType.java
│   └── TransactionStatus.java
│
├── repository/              # Spring Data JPA Repositories
│   ├── UserRepository.java
│   ├── WalletRepository.java
│   └── TransactionRepository.java
│
├── dto/
│   ├── request/             # Inbound DTOs with validation
│   │   ├── RegisterRequest.java
│   │   ├── LoginRequest.java
│   │   ├── TransferRequest.java
│   │   └── TopUpRequest.java
│   └── response/            # Outbound DTOs
│       ├── ApiResponse.java
│       ├── AuthResponse.java
│       ├── BalanceResponse.java
│       └── TransactionResponse.java
│
├── service/                 # Business logic layer
│   ├── AuthService.java           # Registration + login orchestration
│   ├── WalletService.java         # Balance ops, P2P transfer with locking
│   ├── TransactionService.java    # Ledger creation, reference numbers
│   └── IdempotencyService.java    # Idempotency-key cache (5-min TTL)
│
├── security/                # JWT authentication & authorization
│   ├── JwtTokenProvider.java      # Token generation + validation
│   ├── JwtAuthenticationFilter.java  # OncePerRequestFilter
│   └── SecurityConfig.java        # Filter chain, CORS, BCrypt bean
│
├── controller/              # REST API endpoints
│   ├── AuthController.java        # POST /api/auth/register & /login
│   ├── WalletController.java      # GET /api/wallet/balance, POST /topup
│   └── TransferController.java    # POST /api/transfers
│
├── exception/               # Global error handling
│   ├── GlobalExceptionHandler.java    # @ControllerAdvice
│   ├── InsufficientBalanceException.java
│   ├── DuplicateResourceException.java
│   └── InvalidIdempotencyKeyException.java
│
└── config/                  # Application configuration
    ├── CorsConfig.java
    └── WebConfig.java
```

---

### YOUR CURRENT MISSION

1. Do not generate code blocks or boilerplate interfaces yet.
2. Confirm that you fully comprehend the technical strategies we will employ to maintain strict data consistency and prevent runtime deadlocks during parallel transactions.
3. Once confirmed, state that you are ready and ask me which module component (Repositories, Services, Security Filters, Controllers, or the Frontend UI Shell) we should generate first.
4. When implementing, you should also add comment to explain what you do here and the logic behind it.



