# Cloud E-wallet Project Status

Last source review: 2026-07-20

This file is the handoff reference for future development sessions. Feature status below was checked against the current source, not copied from older project notes.

## 1. Project Summary

This repository contains a small simulated cloud-based e-wallet web application. It has one shared frontend for regular users and administrators, a Spring Boot REST backend, and a MySQL development database running in Docker.

The local MVP implements account management, wallet operations, service payments, user transaction history, and administrative user, transaction, and service management. It is a learning/demo system and is not suitable for real money.

## 2. Technology Stack

| Area | Current technology |
| --- | --- |
| Frontend | React 19, TypeScript, Vite, React Router, Axios, Zustand, Zod |
| Backend | Java 17, Spring Boot 4.1, Spring Web MVC, `JdbcTemplate` |
| Security | Spring Security, BCrypt password hashing, signed JWT access tokens (JJWT) |
| Database | MySQL 8 in Docker |
| Styling | Shared CSS in `frontend/src/App.css`, including responsive layouts |
| Build | npm/Vite for frontend, Maven for backend |

## 3. Current Architecture

- One React frontend serves both user and admin experiences.
- Route guards separate public, user-only, and admin-only pages.
- The frontend stores the current account and JWT access token in Zustand and `localStorage`.
- Axios uses a shared client and clears invalid/blocked sessions.
- A 10-second account polling loop refreshes account state and logs out blocked accounts.
- Spring controllers currently contain SQL and business logic directly; there is no service/repository layer.
- A stateless Spring Security filter validates signed bearer tokens, then reloads account role/status from MySQL on every protected request.
- Core wallet mutations use Spring transactions and `JdbcTemplate`.
- MySQL data persists in a named Docker volume and is initialized from `database/schema.sql` only when the volume is first created.

## 4. Completed Features

### User Features

- [x] Registration with phone, password, and profile creation
- [x] Required normalized email registration with local email verification
- [x] Resend-verification and forgot/reset-password flows using hashed, expiring, single-use tokens
- [x] User login and logout
- [x] BCrypt password hashing and verification
- [x] Role-aware post-login navigation
- [x] Profile viewing and editing
- [x] Wallet information and current balance
- [x] Transfer between user wallets
- [x] Simulated deposit/top-up
- [x] Transaction history with deposit, transfer, and payment presentation
- [x] User transaction history exposes and displays only the authenticated wallet's balance before and after each transaction
- [x] Active service list loaded from MySQL
- [x] Service payment using the database price
- [x] Inactive-service payment rejection
- [x] Wallet and transaction refresh after successful operations
- [x] Unverified users may log in and use read-only account pages but cannot deposit, transfer, or pay

### Admin Features

- [x] Admin login and logout
- [x] Admin-only route guard and backend role checks
- [x] Admin dashboard and system totals
- [x] User list with search/status filtering
- [x] Ban and unban regular users
- [x] System-wide transaction list
- [x] Transaction search by code, phones, names, service, and description
- [x] Transaction type, status, date, and sort filters
- [x] Stable transaction sorting and zero-based pagination
- [x] Responsive transaction table/cards and read-only detail modal
- [x] Admin service list including active and inactive services
- [x] Create and edit services
- [x] Activate and deactivate services without deleting rows
- [x] Service name/description search and status filter
- [x] Frontend Zod service validation and backend `BigDecimal` validation
- [x] Case-insensitive, trimmed duplicate service-name rejection

### Shared Features

- [x] One shared frontend for user and admin
- [x] Public, protected, user-only, and admin-only route behavior
- [x] Account-status checks against the database
- [x] Automatic logout when an account becomes blocked
- [x] Page-refresh persistence through Zustand and `localStorage`
- [x] Shared toast notifications
- [x] Centered confirmation modals with Escape handling
- [x] Responsive desktop/mobile layouts
- [x] Null-safe transaction and profile rendering

## 5. In Progress

No local-MVP product feature or verification task is currently in progress. Local email verification and password reset were implemented and automatically verified on 2026-07-20. The existing persistent MySQL volume still requires the documented non-destructive migration before this feature can be run manually.

The next recommended work is email registration/verification and password-reset functionality. Production-grade idempotency, real-database concurrency stress tests, and broader financial audit work remain scheduled for Phase 3.

## 6. Current Database Structure

| Table | Purpose | Important notes |
| --- | --- | --- |
| `users` | Shared user/admin accounts | Unique phone, normalized unique regular-user email, verification state, BCrypt password, role/status |
| `account_tokens` | Verification and recovery tokens | SHA-256 token hash, type, expiry, single-use timestamp, user relationship |
| `user_profiles` | Regular-user profile | One-to-one with `users` |
| `admin_profiles` | Administrator profile | One-to-one with `users`; admins do not have wallets |
| `wallets` | User balance | One wallet per regular user, non-negative balance constraint |
| `services` | Simulated payment services | Price, optional description, `is_active`, timestamps |
| `transactions` | Deposit, transfer, and payment history | Sender/receiver/service relationships are nullable; rows are preserved |

The schema includes foreign keys and indexes for transaction relationships, time, type, and account role/status. No migration framework is configured; schema initialization currently relies on the Docker initialization script.

## 7. API Overview

### Authentication and Account

| Method | Endpoint | Purpose |
| --- | --- | --- |
| POST | `/api/auth/register` | Register regular user and wallet |
| POST | `/api/auth/login` | Login user or admin |
| POST | `/api/auth/verify-email` | Verify a regular-user email with a single-use token |
| POST | `/api/auth/resend-verification` | Generically request a replacement verification token |
| POST | `/api/auth/forgot-password` | Generically request password recovery by email |
| POST | `/api/auth/reset-password` | Reset a regular-user password with a single-use token |
| GET | `/api/account/me` | Reload current account and status |
| PATCH | `/api/account/me` | Edit user/admin profile fields |

### User Wallet

| Method | Endpoint | Purpose |
| --- | --- | --- |
| GET | `/api/user/wallet/me` | Wallet and account information |
| POST | `/api/user/wallet/deposit` | Simulated deposit |
| POST | `/api/user/wallet/transfer` | Transfer to another user |
| GET | `/api/user/wallet/transactions` | Current user's transaction history |
| GET | `/api/user/wallet/services` | Active services only |
| POST | `/api/user/wallet/payments` | Pay an active service using its database price |

### Administration

| Method | Endpoint | Purpose |
| --- | --- | --- |
| GET | `/api/admin/dashboard` | Admin profile and summary |
| GET | `/api/admin/users` | Regular-user list |
| PATCH | `/api/admin/users/{userId}/status` | Ban or unban user |
| GET | `/api/admin/transactions` | Paginated system transaction list with filters |
| GET | `/api/admin/services` | All services |
| POST | `/api/admin/services` | Create service |
| PATCH | `/api/admin/services/{serviceId}` | Edit service fields |
| PATCH | `/api/admin/services/{serviceId}/status` | Activate/deactivate service |

Development-only `/api/test/**` endpoints also exist and must be removed or protected before deployment.

## 8. Frontend Routes

| Route | Access | Purpose |
| --- | --- | --- |
| `/` | Public | Home page |
| `/login` | Public | Login |
| `/register` | Public | Registration |
| `/verify-email` | Public | Verify email from a token query parameter |
| `/forgot-password` | Public | Request a password-reset link |
| `/reset-password` | Public | Set a new password from a token query parameter |
| `/profile` | Authenticated | User/admin profile |
| `/dashboard` | Regular user | Wallet tabs: wallet, transfer, deposit, services, history |
| `/admin` | Admin | Admin dashboard |
| `/admin/users` | Admin | User management |
| `/admin/transactions` | Admin | Transaction management |
| `/admin/services` | Admin | Service management |

Unauthenticated protected navigation redirects to `/login`. A regular user opening an admin route is redirected to `/dashboard`. Admin accounts cannot use user wallet routes because the backend verifies role and the frontend uses `UserRoute`.

## 9. Authentication Status

Status: **signed JWT access-token implementation verified locally**.

- Passwords are stored and checked with BCrypt.
- Successful login and registration return a signed HS256 JWT access token with the account ID as its subject.
- Access-token lifetime is configurable with `JWT_EXPIRATION` and defaults to 3600 seconds.
- `JWT_SECRET` is required at backend startup and must provide at least 32 bytes; no real secret is committed by the JWT implementation.
- Spring Security validates the signature and expiration, and reloads the account from MySQL so nonexistent, blocked, and role-inappropriate accounts are rejected.
- Missing/invalid authentication returns `401`; blocked accounts and wrong roles return `403` where applicable.
- Session polling and the shared Axios error handler log out invalid or blocked sessions.
- The frontend retains refresh persistence by storing the access token in the existing `localStorage` session state and attaches it through the shared Axios client.
- There is no refresh token or server-side JWT revocation list. Password reset does not invalidate already-issued JWTs.

## 10. Known Limitations

- JWT access tokens are stored in `localStorage`, so an XSS defect could expose them.
- There is no refresh-token flow or server-side per-token revocation; account status changes are enforced through the database check on each protected request.
- Configuration and development credentials are committed in local configuration files rather than injected from environment variables.
- Frontend API base URL and backend CORS origin are hardcoded for local development.
- Local development depends on Docker MySQL.
- Deposit is simulated and does not contact a provider.
- No real payment provider, webhook, reconciliation, refund, or payment-order workflow exists.
- Email delivery is local-development logging only; Amazon SES is not integrated.
- Authentication and recovery endpoints do not yet have rate limiting.
- There is no audit log for admin actions.
- There is no idempotency key or duplicate-request protection for wallet operations.
- Automated coverage now includes a Spring context test, focused isolated wallet-controller safety tests, and JWT/Spring Security integration tests; it does not yet include browser E2E tests or real-MySQL concurrency stress tests.
- No AWS deployment, SES integration, SQS workflow, Lambda feature, or production observability is implemented.
- This application must not handle real money in its current state.

## 11. Known Issues

| Severity | Issue | Impact / action |
| --- | --- | --- |
| High | Core mutation endpoints lack idempotency | Double submissions can create duplicate operations; add request-level idempotency in Phase 3 |
| High | Concurrency coverage is isolated rather than a real-MySQL stress test | Deterministic tests verify sender locking behavior, but database-level stress testing remains Phase 3 work |
| Medium | `/api/test/**` endpoints are unauthenticated | Remove or restrict them before deployment |
| Medium | DB/API/CORS configuration is hardcoded | Move to environment-specific configuration |
| Medium | SQL/business logic lives in controllers | Refactor when complexity or test coverage grows |
| Medium | Duplicate service names are application-checked only | Concurrent creates could race; consider a normalized unique key in a migration |
| Medium | UTF-8/mojibake appears in existing SQL comments and older documentation when read by some tools | Verify actual file encoding and browser/API output before rewriting data |
| Low | `start-dev.ps1` uses fixed waits instead of health checks | Replace waits with readiness checks when improving developer tooling |

## 12. Next Recommended Task

**Continue Phase 2 with rate limiting and environment-specific configuration, then implement the Amazon SES email-service adapter.**

JWT access-token authentication is now ready as the base for that work. Keep email business logic local first and integrate Amazon SES only after deployment preparation.

## 13. Development Roadmap

### Phase 1 — Finish Local MVP

1. [x] Finish Admin Service Management
2. [x] Run main-flow manual regression testing (reported successful by the user)
3. [x] Run frontend/backend automated checks and fix confirmed errors
4. [x] Verify UTF-8 source decoding, SQL/HTTP encoding configuration, Vietnamese API mapping, and frontend compilation
5. [x] Add isolated transfer, insufficient-balance, rollback, and concurrency safety tests
6. [x] Confirm inactive-service payment rejection through an automated controller test

Verification scope note: the UTF-8 checks included fatal byte decoding with no replacement characters, intended Vietnamese seed text, backend UTF-8 configuration, a Vietnamese service API mapping test, and a successful frontend production build. Real-browser visual inspection was part of the user's manual main-flow testing but was not automated. Concurrency and rollback tests isolate controller/transaction behavior without mutating the persistent Docker database; real-MySQL stress testing remains Phase 3 work.

### Phase 2 — Authentication and Security

1. [x] Replace the demo token with JWT.
2. [x] Add access-token expiration.
3. [x] Continue validating role/status from the backend.
4. [x] Add email to registration.
5. [x] Add email verification and resend.
6. [x] Add forgot/reset-password flows.
7. Move secrets and credentials to environment variables.
8. Restrict CORS by environment.
9. Remove or protect test endpoints.
10. Add basic rate limiting if practical.
11. Review password and authentication error handling.

Preferred direction: keep Spring Boot authentication, implement JWT and local email business logic, then integrate Amazon SES after deployment. Cognito is not currently planned.

### Phase 3 — Financial Consistency and Audit

1. Add idempotency for deposit, transfer, and payment.
2. Prevent duplicate transactions from repeated clicks/requests.
3. Review and test concurrent transfer behavior and wallet locking.
4. Prove balances cannot become negative under concurrency.
5. Verify rollback on every partial-failure path.
6. Review transaction/reference-code guarantees.
7. Add admin audit logs for user status and service changes.
8. Improve failed-transaction modeling.
9. Add explicit transaction limits and validation policy.

Do not introduce real-money behavior before this phase is stable.

### Phase 4 — Deployment Preparation

1. Move frontend API URL, database settings, and CORS to environment-specific configuration; keep the JWT secret environment-only.
2. Add development and production Spring profiles.
3. Dockerize the backend and decide whether the frontend also needs a runtime container.
4. Confirm repeatable production builds.
5. Add deployment documentation and health checks.
6. Introduce a database migration strategy.
7. Document backup and recovery.
8. Audit the repository for committed secrets.

### Phase 5 — Initial AWS Deployment

Target a cost-conscious first deployment:

- React: Amazon S3 and CloudFront
- Spring Boot: a small Amazon EC2 instance
- MySQL: a small, single-AZ Amazon RDS for MySQL instance

Validate HTTPS, frontend/backend connectivity, EC2/RDS connectivity, CORS, authentication, wallet/admin functions, persistence, backup, logs, and cost alerts. Initially avoid a NAT Gateway, Application Load Balancer, and Multi-AZ unless requirements justify them.

### Phase 6 — AWS Email Integration

1. Verify an Amazon SES sender identity.
2. Configure least-privilege IAM permissions and an SES region.
3. Connect the locally developed email service to SES.
4. Send verification and password-reset emails.
5. Test sandbox restrictions and request production access when needed.
6. Add templates without committing AWS credentials.

### Phase 7 — Serverless AWS Extensions

Only after the core deployment is stable:

- Notifications: Spring Boot → SQS → Lambda → SES or notification storage
- Daily reports: EventBridge → Lambda → CSV/JSON in S3
- Fraud alerts: Spring Boot → SQS → Lambda → alert record/admin notification

Initial Lambda fraud rules may identify rapid transactions, high daily totals, repeated failures, post-unban activity, or repeated receivers. Lambda must not directly change balances or automatically ban users.

### Phase 8 — Payment Sandbox

Before considering real money, add payment orders, pending/success/failed states, provider IDs, idempotency keys, an HTTPS webhook, signature verification, duplicate-webhook prevention, reconciliation, an admin payment-order view, and sandbox tests. Credit a wallet only after a verified backend webhook—never from a frontend success response.

### Phase 9 — Real Payment Consideration

**Deferred and outside the current MVP.**

Do not implement real-money top-up until JWT, email verification, HTTPS, webhook verification, idempotency, audit logs, concurrency tests, limits, reconciliation, failure/refund handling, and legal/provider review are complete.

## 14. AWS Target Architecture

```text
Users
  → CloudFront
  → S3-hosted React application
  → HTTPS Spring Boot API on EC2
  → RDS for MySQL

Optional asynchronous extensions after stabilization:
Spring Boot → SQS → Lambda → SES / S3 / alert storage
EventBridge → scheduled Lambda → S3 reports
```

Keep transfer, deposit, payment, and balance logic in Spring Boot. Lambda is reserved for auxiliary asynchronous processing.

## 15. Cost-Control Notes

- Create AWS Budget alerts before deployment.
- Start with small EC2 and single-AZ RDS resources.
- Avoid NAT Gateway and load-balancer costs until needed.
- Set explicit CloudWatch log-retention periods.
- Remove unused resources and public IPv4 addresses.
- Monitor RDS storage, snapshots, data transfer, SES, SQS, and Lambda usage.
- Do not leave temporary environments running without an owner or shutdown plan.

## 16. Manual Test Checklist

### Authentication and Account

- [ ] Register and login as a regular user
- [ ] Login/logout as admin
- [ ] Refresh both roles and confirm session persistence
- [ ] Edit user and admin profiles
- [ ] Block a logged-in account and confirm automatic logout
- [ ] Confirm user/admin route redirection and API role rejection

### Wallet and Services

- [ ] Deposit and verify balance plus transaction
- [ ] Transfer and verify both balances plus transaction history
- [ ] Pay an active service and verify database price usage
- [ ] Deactivate a service and confirm it disappears after user refetch
- [ ] Call payment directly for an inactive service and confirm rejection
- [ ] Activate the service and confirm it becomes usable again
- [ ] Verify failed/repeated requests do not cause unexplained balance changes

### Administration

- [ ] Search/filter and ban/unban users
- [ ] Exercise transaction search, filters, sorting, pagination, and details
- [ ] Create, edit, activate, and deactivate a service
- [ ] Verify service duplicate and validation errors
- [ ] Confirm historical payments still display deactivated service names

### Quality

- [ ] Test responsive user/admin layouts
- [ ] Verify Vietnamese text from schema through API to browser
- [ ] Compare wallet balances with transaction records
- [ ] Run frontend build/lint and backend tests/compile

## 17. Commands

Run from the repository root unless noted.

Start the existing local development stack:

```powershell
.\start-dev.ps1
```

Start or stop only the Docker services while preserving data:

```powershell
docker compose up -d
docker compose stop
```

Start backend manually:

```powershell
cd backend
$env:JWT_SECRET='<at-least-32-byte-secret>'
$env:JWT_EXPIRATION='3600'
.\mvnw.cmd spring-boot:run
```

`JWT_SECRET` is required. `JWT_EXPIRATION` is optional and is expressed in seconds; it defaults to `3600`.

Start frontend manually:

```powershell
cd frontend
npm run dev
```

Validate frontend:

```powershell
cd frontend
npm run build
npm run lint
```

Validate backend:

```powershell
cd backend
mvn clean test
mvn -DskipTests compile
```

`docker compose down -v` is destructive because it removes the development database volume. It is not a routine startup/shutdown command and must not be run unless database deletion is explicitly intended and approved.

Before starting this version against an existing persistent MySQL volume, apply the one-time non-destructive migration (do not rerun it):

```powershell
Get-Content -Raw database/migrations/V001__email_verification_and_password_reset.sql | docker compose exec -T mysql mysql -uroot -proot ewallet_db
```

Use the actual configured MySQL service name and credentials if they differ. Existing regular users receive unique `@local.invalid` placeholder emails and remain verified so their wallet access is preserved; update those placeholders manually if those accounts need password recovery.

Most recent automated validation on 2026-07-20 after local email verification and password-reset implementation:

- Frontend build: passed
- Frontend lint: passed
- Backend tests (`mvn clean test`): passed (44 total: 4 token-service tests, 11 email-auth controller tests, 12 wallet safety/history/verification tests, 1 application-context test, and 16 JWT/Spring Security integration tests)
- Backend compile (`mvn -DskipTests compile`): passed

Focused automated coverage added:

- Successful transfer updates sender/receiver state and records one transaction
- Insufficient transfer balance performs no mutation
- Inactive service payment is rejected before wallet mutation
- Blocked user cannot access a wallet mutation endpoint
- A wallet-operation failure triggers the Spring transaction rollback boundary
- Two concurrent outgoing transfers cannot drive the modeled sender balance below zero
- Vietnamese service name and description survive controller/API response mapping
- Outgoing transfer history exposes the authenticated sender's balance before and after
- Incoming transfer history exposes the authenticated receiver's balance before and after and omits stored sender values
- User transaction history does not expose separate sender/receiver balance fields
- Deposit and payment history expose the authenticated user's correct balance before and after
- Registration requires a valid email, normalizes it, and rejects case-insensitive duplicates
- New regular users are unverified and receive a local verification URL through the email abstraction
- Verification and reset tokens are hashed, expiring, single-use, and replaced on reissue
- Unverified users can log in but receive `EMAIL_NOT_VERIFIED` for deposit, transfer, and payment
- Forgot-password responses do not reveal account existence
- Password reset stores a BCrypt hash that matches the new password and rejects the old password

JWT/Spring Security coverage added:

- Login returns a signed access token with expiry metadata
- A valid token can access a protected endpoint
- Missing, empty, malformed, forged, and expired tokens are rejected
- Compact JWTs are rejected unless all three non-empty segments use canonical unpadded Base64URL encoding
- Changing an existing signature character and appending `4` or `5` to a valid signature are rejected
- Extra dots, padding, whitespace, and invalid compact-JWT characters are rejected
- Tokens for nonexistent and blocked accounts are rejected
- User tokens cannot access admin APIs, and admin tokens cannot access user wallet APIs
- A token issued before an account is blocked is rejected after the database status changes
- Login and registration endpoints remain public

The email/JWT verification pass was automated; no new manual browser test was executed during this change, and the persistent database migration was not executed automatically.

## 18. AI Handoff Instructions

Future AI/Codex sessions must:

1. Read this file first, then inspect the actual source before changing anything.
2. Treat source code and executed checks as authoritative if this document becomes stale.
3. Do not redo completed features without evidence of a defect or an explicit request.
4. Do not replace the architecture or authentication direction without approval.
5. Preserve the single shared frontend and existing user/admin behavior.
6. Do not reset, delete, or recreate the database unless explicitly requested.
7. Never run `docker compose down -v` as routine setup.
8. Do not expose credentials, passwords, private tokens, or cloud secrets in code, logs, or reports.
9. Keep core financial logic in Spring Boot; do not move balance mutations to Lambda.
10. Do not hard-delete services or historical transactions; use `is_active` for service availability.
11. Build and test after changes in proportion to risk.
12. Report files changed and commands/tests actually executed; never claim unexecuted tests passed.
13. Ask only when a decision materially affects architecture, authorization, external systems, or data safety.

## 19. Design Decisions

- Keep one frontend for user and admin.
- Keep core wallet operations in Spring Boot.
- Use the implemented Spring Security JWT flow as the authentication foundation.
- Cognito is not currently planned.
- Use S3/CloudFront for the production frontend, EC2 for the backend, and RDS for MySQL.
- Use Lambda only for auxiliary asynchronous notifications, reporting, and alerts.
- Do not move transfer, deposit, payment, or balance mutation logic to Lambda initially.
- Never hard-delete services; deactivate through `is_active`.
- Preserve historical transactions.
- Defer real-money top-up until the security, consistency, audit, webhook, and legal prerequisites are complete.
