# Cloud E-Wallet Project Status

Last source review: 2026-07-24

Status legend:

- ✅ Completed — implemented and verified in the repository, or previously completed and retained.
- 🟡 In Progress — partially implemented, locally prepared but not cloud-verified, or currently blocked on verification.
- ⬜ Planned — not implemented or not evidenced by the repository.

This document is derived from the current source tree. Source code and executed checks take precedence over older notes. The application is a simulated e-wallet learning project and is not suitable for real money.

## 1. Executive Summary

| Area | Status | Current state |
| --- | --- | --- |
| Local MVP | ✅ Completed | User wallet, account, service-payment, transaction-history, and admin workflows are implemented. |
| Authentication | ✅ Completed | BCrypt passwords, signed expiring JWT access tokens, backend role/status reload, and email verification/password reset are implemented. |
| Production email code | 🟡 In Progress | SMTP implementation for Amazon SES, production profile wiring, examples, and tests exist locally; no live SES sender/account verification is evidenced. |
| Production builds | ✅ Completed | Backend Dockerfile and frontend static build configuration exist; frontend build/lint pass in the 2026-07-24 review. |
| AWS deployment | ⬜ Planned | No IaC, AWS identifiers, deployed URLs, or other repository evidence proves that S3, CloudFront, EC2, RDS, ALB, ACM, Route 53, SES, or CloudWatch resources exist. |
| ALB/security phase | ⬜ Planned | The application exposes health probes and handles forwarded headers, but the ALB, HTTPS certificate, network rules, WAF/rate limiting, and secret store are not implemented here. |
| High availability | ⬜ Planned | The target remains one backend EC2 initially; a two-instance ALB target group is a future migration. |
| CI/CD | ⬜ Planned | No `.github/workflows` deployment pipeline exists. |
| Container orchestration | ⬜ Planned | A backend container exists, but no ECS/EKS task, service, cluster, or deployment configuration exists. |
| Production readiness | 🟡 In Progress | Application packaging and baseline security are prepared; cloud infrastructure, operational controls, complete tests, and financial-safety controls remain. |

## 2. Technology Stack

| Area | Status | Current technology |
| --- | --- | --- |
| Frontend | ✅ Completed | React 19, TypeScript 6, Vite 8, React Router 7, Axios, Zustand, and Zod |
| Backend | ✅ Completed | Java 17, Spring Boot 4.1, Spring Web MVC, Spring Security, Actuator, Mail, and `JdbcTemplate` |
| Authentication | ✅ Completed | BCrypt and JJWT HS256 access tokens |
| Database | ✅ Completed | MySQL 8 locally; RDS-compatible numbered SQL scripts prepared |
| Local runtime | ✅ Completed | Docker Compose MySQL plus PowerShell startup helper |
| Backend packaging | ✅ Completed | Multi-stage Docker image with Java 17 and non-root UID 10001 |
| Frontend packaging | ✅ Completed | Static Vite output suitable for S3/CloudFront |
| Infrastructure as code | ⬜ Planned | No CloudFormation, CDK, Terraform, or Pulumi configuration |

## 3. Implemented Application Architecture

```text
Browser
  ├─ React/Vite SPA
  │    ├─ public, user, and admin route guards
  │    ├─ Zustand state + JWT in localStorage
  │    └─ Axios bearer-token client + 10-second account-status polling
  │
  └─ /api requests
       └─ Spring Boot REST API
            ├─ stateless Spring Security + JWT filter
            ├─ role/status reloaded from MySQL on protected requests
            ├─ controllers containing SQL and business logic
            ├─ transactional wallet mutations with row locking
            ├─ local link-logging email service (local profile)
            └─ SMTP email service (prod profile)
                 └─ MySQL 8
```

Current implementation notes:

- ✅ Completed — one frontend serves regular-user and administrator experiences.
- ✅ Completed — public, authenticated, user-only, and admin-only routes are enforced in the frontend and backend.
- ✅ Completed — deposit, transfer, and payment mutations use Spring transactions; wallet balance reads use `SELECT ... FOR UPDATE`.
- ✅ Completed — the protected-request JWT filter reloads account role and status from MySQL.
- ✅ Completed — `/actuator/health`, `/actuator/health/liveness`, and `/actuator/health/readiness` are public with details hidden.
- ✅ Completed — development-only `/api/test/**` handlers are restricted to `local` and `test` profiles.
- 🟡 In Progress — SMTP/SES production delivery is code-complete locally but not verified with AWS SES.
- ⬜ Planned — service/repository layering; SQL and much business logic currently remain in controllers.

## 4. Completed Milestones

Previous completed milestones are retained below.

### User Features

- ✅ Completed — registration with normalized email, phone, profile, wallet, and BCrypt password.
- ✅ Completed — login, logout, role-aware navigation, and page-refresh session persistence.
- ✅ Completed — email verification/resend with hashed, expiring, single-use tokens.
- ✅ Completed — forgot/reset password with generic account-discovery-safe responses.
- ✅ Completed — profile viewing and editing.
- ✅ Completed — wallet information and current balance.
- ✅ Completed — simulated deposit/top-up.
- ✅ Completed — transfer between regular-user wallets.
- ✅ Completed — active service catalog and payment using the server-side database price.
- ✅ Completed — inactive-service payment rejection.
- ✅ Completed — user-specific transaction history and correct authenticated-wallet before/after balances.
- ✅ Completed — unverified users may use read-only account pages but cannot mutate wallet state.
- ✅ Completed — successful operations refresh wallet and transaction state.

### Admin Features

- ✅ Completed — admin authentication and admin-only frontend/backend access.
- ✅ Completed — dashboard totals.
- ✅ Completed — user search/filter and ban/unban.
- ✅ Completed — system transaction search, filters, stable sorting, pagination, responsive views, and detail modal.
- ✅ Completed — list active/inactive services.
- ✅ Completed — create, edit, activate, and deactivate services without hard deletion.
- ✅ Completed — frontend Zod and backend decimal/service validation.
- ✅ Completed — trimmed, case-insensitive duplicate service-name rejection at application level.

### Shared and Quality Milestones

- ✅ Completed — shared toast notifications and confirmation modals with Escape handling.
- ✅ Completed — responsive desktop/mobile layouts and null-safe rendering.
- ✅ Completed — automatic logout for expired/invalid sessions and blocked accounts.
- ✅ Completed — UTF-8 source/API mapping checks and Vietnamese response coverage.
- ✅ Completed — Dockerized non-root backend and environment-specific Spring profiles.
- ✅ Completed — environment-driven database, JWT, frontend URL, CORS, token lifetime, and mail settings.
- ✅ Completed — production-safe numbered RDS schema/admin/catalog scripts.
- ✅ Completed — public health/liveness/readiness endpoints and production exclusion of test controllers.
- ✅ Completed — isolated wallet safety, rollback-boundary, concurrency-model, JWT, token, email, profile-selection, and production-endpoint tests.
- ✅ Completed — production SMTP email adapter and Amazon SES SMTP configuration are implemented and unit/profile tested locally.

## 5. API and Route Inventory

| Status | Method | Endpoint | Purpose |
| --- | --- | --- | --- |
| ✅ Completed | POST | `/api/auth/register` | Create regular user, profile, wallet, and verification token |
| ✅ Completed | POST | `/api/auth/login` | Authenticate user/admin and issue JWT |
| ✅ Completed | POST | `/api/auth/verify-email` | Consume email verification token |
| ✅ Completed | POST | `/api/auth/resend-verification` | Replace verification token |
| ✅ Completed | POST | `/api/auth/forgot-password` | Request recovery without revealing account existence |
| ✅ Completed | POST | `/api/auth/reset-password` | Consume reset token and replace password |
| ✅ Completed | GET/PATCH | `/api/account/me` | Read/update current profile |
| ✅ Completed | GET | `/api/user/wallet/me` | Current user and wallet |
| ✅ Completed | POST | `/api/user/wallet/deposit` | Simulated deposit |
| ✅ Completed | POST | `/api/user/wallet/transfer` | Wallet transfer |
| ✅ Completed | GET | `/api/user/wallet/transactions` | Authenticated wallet history |
| ✅ Completed | GET | `/api/user/wallet/services` | Active service list |
| ✅ Completed | POST | `/api/user/wallet/payments` | Service payment |
| ✅ Completed | GET | `/api/admin/dashboard` | Admin summary |
| ✅ Completed | GET/PATCH | `/api/admin/users`, `/api/admin/users/{id}/status` | User management |
| ✅ Completed | GET | `/api/admin/transactions` | Filtered/paginated system history |
| ✅ Completed | GET/POST/PATCH | `/api/admin/services/**` | Service management |
| ✅ Completed | GET | `/actuator/health/**` | ALB/container health probes |
| ✅ Completed | GET | `/api/test/**` | Local/test-profile diagnostics only |

Frontend routes implemented: `/`, `/login`, `/register`, `/verify-email`, `/forgot-password`, `/reset-password`, `/profile`, `/dashboard`, `/admin`, `/admin/users`, `/admin/transactions`, and `/admin/services`.

## 6. Database Status

| Object | Status | Notes |
| --- | --- | --- |
| `users` | ✅ Completed | Unique phone/email, verification state, BCrypt password, role/status |
| `account_tokens` | ✅ Completed | SHA-256 token hash, token type, expiry, single-use timestamp |
| `user_profiles` / `admin_profiles` | ✅ Completed | Role-specific profile data |
| `wallets` | ✅ Completed | One per regular user, decimal balance, non-negative check |
| `services` | ✅ Completed | Price, description, active flag, timestamps |
| `transactions` | ✅ Completed | Deposit/transfer/payment history with nullable preserved relationships |
| Local schema initialization | ✅ Completed | `database/schema.sql` mounted by Docker Compose on first volume creation |
| Fresh RDS scripts | ✅ Completed | `001_schema.sql`, placeholder-only `002_admin_template.sql`, `003_services_seed.sql` |
| Migration framework | ⬜ Planned | No Flyway/Liquibase automation; scripts are manually applied |
| DB-enforced normalized service uniqueness | ⬜ Planned | Application check exists, but concurrent creates can race |
| Production RDS execution | ⬜ Planned | No repository evidence that scripts have been applied to RDS |

## 7. Deployment Status

### Evidence-Based Current State

- ✅ Completed — backend Dockerfile builds a non-root Java 17 runtime image.
- ✅ Completed — frontend produces static assets and supports `VITE_API_BASE_URL` or same-origin `/api`.
- ✅ Completed — production profile reads database, JWT, CORS, frontend URL, token, and SES SMTP settings from the environment.
- ✅ Completed — RDS initialization scripts and a controlled admin template exist.
- ✅ Completed — forwarded-header support is enabled in the production profile for operation behind an ALB/reverse proxy.
- 🟡 In Progress — SES SMTP application integration exists locally; AWS sender identity, sandbox/production access, credentials, and live delivery are unverified.
- ⬜ Planned — AWS account/region/domain decisions and deployed resource identifiers.
- ⬜ Planned — S3 frontend bucket and CloudFront distribution.
- ⬜ Planned — Route 53 DNS and ACM certificates.
- ⬜ Planned — VPC/subnets/security groups, private RDS, EC2 instance, and runtime service.
- ⬜ Planned — ALB, HTTPS listener, target group, health check, and HTTP-to-HTTPS redirect.
- ⬜ Planned — CloudWatch log shipping, dashboards, alarms, retention, AWS Budgets, and backup restore test.
- ⬜ Planned — deployment verification and rollback evidence.

Repository state is not proof of live AWS state. Until external deployment evidence is recorded, AWS deployment remains planned.

## 8. Target AWS Architecture

### Phase 1 Target — One EC2 Behind an ALB

```text
Users
  │ HTTPS
  ▼
Route 53
  ├──────────────► CloudFront ─► private S3 origin (React build)
  │
  └─ api hostname ─► ACM TLS certificate
                       │
                       ▼
                 Application Load Balancer
                 public subnets, 80→443
                       │ target group /actuator/health
                       ▼
                 EC2 backend instance
                 private or tightly restricted subnet
                       │ TCP 3306, SG-to-SG only
                       ▼
                 RDS MySQL, private subnet

Supporting services:
Secrets Manager or SSM Parameter Store ─► EC2 runtime configuration
EC2/Application/ALB/RDS logs and metrics ─► CloudWatch alarms
Spring Boot SMTP ─► Amazon SES
AWS Budgets ─► cost alerts
```

### Phase 2 Target — High Availability

```text
                         ┌─► EC2 backend A (AZ-a) ─┐
Users ─► HTTPS ALB ──────┤                         ├─► RDS MySQL
                         └─► EC2 backend B (AZ-b) ─┘
                              stateless JWT API

CloudFront ─► private S3 frontend
Auto Scaling Group: desired=2, minimum=2, health replacement enabled
```

The backend is already stateless at the HTTP session layer, so both instances can share RDS and the same externally managed JWT secret. Deployment must keep image/config/schema versions compatible across both targets. Core balance mutations remain in Spring Boot; they do not move to Lambda.

### Phase 4 Target — ECS Preferred

```text
CloudFront ─► S3 frontend
Users ─► ALB ─► ECS service on Fargate (2+ tasks across AZs) ─► RDS MySQL
                  ├─ ECR image
                  ├─ Secrets Manager/SSM
                  ├─ CloudWatch Logs
                  └─ autoscaling and rolling deployment
```

EKS/Kubernetes is a later learning phase, not the preferred first orchestration platform, because ECS/Fargate has lower operational complexity for this application.

## 9. Security Implementation

### Implemented

- ✅ Completed — BCrypt password hashing.
- ✅ Completed — signed HS256 JWT access tokens with configurable expiry and a startup-enforced minimum 32-byte secret.
- ✅ Completed — strict canonical compact-JWT validation; malformed, forged, padded, expired, and noncanonical tokens are rejected.
- ✅ Completed — stateless Spring Security with explicit admin/user/account authorization.
- ✅ Completed — account existence, status, and role are reloaded from MySQL on each protected request.
- ✅ Completed — blocked accounts are rejected and frontend sessions are cleared.
- ✅ Completed — explicit production CORS origins; wildcards/empty origin lists are rejected and credentialed CORS is disabled.
- ✅ Completed — hashed, expiring, single-use verification/reset tokens; replacement invalidates unused prior tokens.
- ✅ Completed — generic forgot-password response limits account enumeration.
- ✅ Completed — unverified accounts cannot mutate wallets.
- ✅ Completed — production test endpoints are absent.
- ✅ Completed — container runs as a non-root user.
- ✅ Completed — Actuator exposes only health/info and hides health details.
- ✅ Completed — SES SMTP uses authentication and required STARTTLS in the production profile.
- ✅ Completed — secrets are represented as environment placeholders; populated production environment files are ignored.

### Remaining Security Work

- 🟡 In Progress — repository secret handling is improved, but the previously tracked local JWT secret must be considered exposed and never reused.
- ⬜ Planned — store runtime secrets in AWS Secrets Manager or encrypted SSM Parameter Store; use an EC2 instance role and least privilege.
- ⬜ Planned — ACM certificate, ALB HTTPS listener, port 80 redirect, modern TLS policy, and HSTS validation.
- ⬜ Planned — allow inbound backend traffic only from the ALB security group; allow RDS 3306 only from backend security groups.
- ⬜ Planned — WAF or application/gateway rate limiting for login, registration, verification, reset, and wallet mutation endpoints.
- ⬜ Planned — refresh-token rotation or short-lived access-token strategy and password-reset token/session revocation.
- ⬜ Planned — content-security policy and stronger XSS controls; JWT currently resides in `localStorage`.
- ⬜ Planned — request idempotency keys for all wallet mutations.
- ⬜ Planned — admin audit log and immutable financial/audit events.
- ⬜ Planned — dependency, secret, container-image, and static analysis in CI.
- ⬜ Planned — RDS TLS `VERIFY_IDENTITY`, encryption at rest, backup retention, deletion protection, and restore test.
- ⬜ Planned — CloudTrail, GuardDuty/security monitoring, CloudWatch alarms, and incident/credential-rotation procedures.

## 10. Production Readiness

Overall: 🟡 In Progress.

| Capability | Status | Production requirement |
| --- | --- | --- |
| Repeatable frontend build | ✅ Completed | Vite production build passes |
| Repeatable backend package | ✅ Completed | Maven/Docker packaging exists |
| Environment-specific config | ✅ Completed | Profiles and environment placeholders exist |
| Health/readiness endpoints | ✅ Completed | Suitable for ALB target checks |
| Production SMTP adapter | ✅ Completed | Code and automated tests exist |
| Live SES delivery | ⬜ Planned | Verify identity, access mode, credentials, bounce/complaint handling |
| HTTPS and DNS | ⬜ Planned | Route 53/ACM/ALB/CloudFront |
| Secret management | ⬜ Planned | Secrets Manager/SSM with least privilege |
| Network isolation | ⬜ Planned | ALB-only backend ingress and private RDS |
| Observability | ⬜ Planned | Structured logs, metrics, dashboards, alarms, retention |
| Backups/disaster recovery | ⬜ Planned | Automated snapshots and tested restore runbook |
| Rate limiting | ⬜ Planned | Auth/recovery and high-risk mutation endpoints |
| Idempotency | ⬜ Planned | Deposit, transfer, and payment |
| Real-DB concurrency proof | ⬜ Planned | Parallel MySQL integration/stress tests |
| Browser E2E suite | ⬜ Planned | Critical user/admin workflows |
| CI/CD and rollback | ⬜ Planned | Automated validated deployments and previous-version rollback |
| Real-money suitability | ⬜ Planned | Explicitly out of scope until financial, legal, provider, and audit prerequisites are complete |

## 11. Testing Status

### Automated Results — 2026-07-24

- ✅ Completed — `npm run build`: passed.
- ✅ Completed — `npm run lint`: passed.
- ✅ Completed — 54 focused backend tests passed across token, email profile, SMTP, auth/email, wallet safety, JWT/security, and production-profile endpoint suites.
- 🟡 In Progress — `mvn clean test`: 54 passed and 1 errored. `EwalletApplicationTests.contextLoads` requires the local MySQL endpoint at `localhost:3307`; MySQL was not running during this review. This is an environment-dependent test error, not a claim of application success.
- ✅ Completed — production-profile tests confirm health/auth exposure and absence of `/api/test/**`.
- ✅ Completed — JWT tests cover valid, missing, malformed, forged, expired, noncanonical, nonexistent-account, blocked-account, and cross-role cases.
- ✅ Completed — wallet safety tests cover successful transfer, insufficient balance, inactive service, blocked user, rollback boundary, modeled concurrent outgoing transfers, history balance privacy, and Vietnamese response mapping.
- ✅ Completed — email/token tests cover normalization, duplicates, hashing, expiry, single use, replacement, unverified restrictions, generic recovery response, BCrypt reset, SMTP content, and profile selection.
- ⬜ Planned — frontend component/unit tests; none are configured.
- ⬜ Planned — browser E2E tests.
- ⬜ Planned — Testcontainers or dedicated MySQL integration test environment.
- ⬜ Planned — real-MySQL concurrency/load tests.
- ⬜ Planned — cloud smoke, failover, backup/restore, security, and performance tests.

### Testing Checklist

- 🟡 In Progress — start Docker MySQL and rerun the entire Maven suite until all 55 tests pass.
- ✅ Completed — build and lint the frontend.
- ✅ Completed — verify production profile hides test endpoints.
- ✅ Completed — verify JWT and role/status enforcement.
- ✅ Completed — verify wallet mutation safety at isolated controller/transaction boundaries.
- ⬜ Planned — automate registration, verification, login, reset, profile, deposit, transfer, payment, and admin browser flows.
- ⬜ Planned — test concurrent transfers/payments against real MySQL and prove no negative balances.
- ⬜ Planned — test idempotent retries after idempotency is implemented.
- ⬜ Planned — run dependency, secret, SAST, and container scans in CI.
- ⬜ Planned — run ALB health, HTTPS redirect, CORS, DNS, and CloudFront smoke tests.
- ⬜ Planned — terminate one Phase 2 EC2 target and verify uninterrupted service.
- ⬜ Planned — restore an RDS snapshot into an isolated environment and verify data.
- ⬜ Planned — test SES verification/reset delivery, bounce, complaint, and sandbox restrictions.

## 12. Known Issues and Limitations

| Severity | Status | Issue and required action |
| --- | --- | --- |
| Critical for real money | ⬜ Planned | The system is a simulation and lacks provider integration, signed webhooks, reconciliation, refunds, compliance, and legal review. |
| High | ⬜ Planned | Wallet mutations have no idempotency keys; repeated requests may duplicate operations. |
| High | ⬜ Planned | No real-MySQL concurrency stress proof; isolated tests model locking behavior only. |
| High | ⬜ Planned | No live AWS network/TLS/secret-management deployment is evidenced. |
| High | ⬜ Planned | No auth/recovery/mutation rate limiting. |
| Medium | 🟡 In Progress | JWT in `localStorage` is exposed if an XSS defect occurs; add CSP/XSS hardening and evaluate safer token architecture. |
| Medium | ⬜ Planned | Password reset does not revoke already issued JWTs; no refresh-token or server-side revocation system exists. |
| Medium | ⬜ Planned | SQL and business logic are concentrated in controllers, limiting testability and maintainability. |
| Medium | ⬜ Planned | Duplicate service-name protection is application-only and can race under concurrent creates. |
| Medium | ⬜ Planned | No admin audit log or immutable operational audit trail. |
| Medium | 🟡 In Progress | Full backend suite depends on a running local MySQL instance; introduce reproducible test DB provisioning. |
| Low | ⬜ Planned | `start-dev.ps1` uses fixed waits instead of readiness-driven startup. |
| Low | ⬜ Planned | No frontend automated unit/component coverage. |

## 13. Environment Variables

Never commit populated `.env.local` or `.env.production` files. Use `.env.example` and `.env.production.example` only as templates.

| Variable | Status | Required where | Purpose |
| --- | --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | ✅ Completed | Backend | `local` or `prod` |
| `DB_URL` | ✅ Completed | Backend | JDBC URL; production should use RDS TLS verification |
| `DB_USERNAME` | ✅ Completed | Backend | Least-privilege runtime DB user |
| `DB_PASSWORD` | ✅ Completed | Backend | Runtime DB password |
| `JWT_SECRET` | ✅ Completed | Backend | Required, minimum 32 bytes; shared consistently across HA tasks |
| `JWT_EXPIRATION` | ✅ Completed | Backend | Access-token lifetime in seconds; minimum 60, default 3600 |
| `FRONTEND_BASE_URL` | ✅ Completed | Backend | Base URL for verification/reset links |
| `CORS_ALLOWED_ORIGINS` | ✅ Completed | Backend | Comma-separated explicit frontend origins |
| `EMAIL_VERIFICATION_MINUTES` | ✅ Completed | Backend | Verification token lifetime; default 1440 |
| `PASSWORD_RESET_MINUTES` | ✅ Completed | Backend | Reset token lifetime; default 30 |
| `MAIL_DEVELOPMENT_LOG_ENABLED` | ✅ Completed | Local backend | Logs local verification/reset links; production forces false |
| `SES_SMTP_HOST` | ✅ Completed | Production backend | Regional SES SMTP hostname |
| `SES_SMTP_PORT` | ✅ Completed | Production backend | SMTP STARTTLS port; default 587 |
| `SES_SMTP_USERNAME` | ✅ Completed | Production backend | SES SMTP credential |
| `SES_SMTP_PASSWORD` | ✅ Completed | Production backend | SES SMTP credential |
| `MAIL_FROM_ADDRESS` | ✅ Completed | Production backend | SES-verified sender |
| `VITE_API_BASE_URL` | ✅ Completed | Frontend build | API origin; omit for same-origin `/api` |
| `JAVA_OPTS` | ✅ Completed | Backend container | Optional JVM runtime flags |
| AWS region/account/resource identifiers | ⬜ Planned | CI/runtime | Define through deployment configuration, not application source |

Production secrets should move to Secrets Manager or encrypted SSM Parameter Store. Non-secret configuration may use SSM, EC2 service environment files with restricted permissions, or ECS task configuration in Phase 4.

## 14. Production Checklist

### Application and Data

- ✅ Completed — frontend production build configuration.
- ✅ Completed — backend non-root container image.
- ✅ Completed — production Spring profile and health probes.
- ✅ Completed — fresh RDS schema/admin/service scripts.
- ✅ Completed — production SMTP adapter and STARTTLS configuration.
- 🟡 In Progress — rerun full backend suite with MySQL available.
- ⬜ Planned — add migration automation/version tracking.
- ⬜ Planned — add idempotency, audit logging, limits, and complete concurrency proof.

### AWS, Network, and Security

- ⬜ Planned — select account, region, domain, tags, owners, and cost limits.
- ⬜ Planned — create AWS Budget alerts before long-running resources.
- ⬜ Planned — provision VPC/subnets/routes/security groups.
- ⬜ Planned — create private encrypted RDS with backups and deletion protection.
- ⬜ Planned — store credentials/secrets in Secrets Manager or SSM and attach least-privilege IAM role.
- ⬜ Planned — create EC2 runtime and managed service with restart-on-failure.
- ⬜ Planned — create ALB, target group, health check, ACM certificate, and 80-to-443 redirect.
- ⬜ Planned — expose backend only to ALB and RDS only to backend security groups.
- ⬜ Planned — create private S3 origin, CloudFront distribution, and DNS records.
- ⬜ Planned — configure WAF/rate limiting and security headers.
- ⬜ Planned — configure SES and validate live delivery.

### Operations

- ⬜ Planned — centralize application/system/ALB logs with retention.
- ⬜ Planned — alarms for target health, 5xx, latency, CPU, disk, RDS capacity/connections, and backup failures.
- ⬜ Planned — document deployment, rollback, secret rotation, incident handling, and ownership.
- ⬜ Planned — verify RDS restore and EC2 replacement.
- ⬜ Planned — run post-deployment regression and capture resource IDs, URLs, versions, and evidence here.

## 15. Deployment Guide

Every cloud step below is planned until external evidence is recorded.

### A. Preflight

1. ✅ Completed — use Java 17, Maven, Node/npm, Docker, MySQL client, and AWS CLI-compatible tooling.
2. ✅ Completed — validate locally with frontend build/lint and backend tests.
3. 🟡 In Progress — resolve the environment-dependent context test by starting MySQL and obtain a fully green backend suite.
4. ⬜ Planned — choose AWS region, domain names, resource names/tags, owner, backup policy, and monthly budget.
5. ⬜ Planned — create a strong new production JWT secret; never reuse a secret from Git history.

### B. Database

1. ⬜ Planned — create an encrypted private MySQL 8 RDS instance with backups, deletion protection, and no public access.
2. ⬜ Planned — allow port 3306 only from the backend security group.
3. ⬜ Planned — create `ewallet_db` with `utf8mb4` and a least-privilege runtime user.
4. ⬜ Planned — apply `database/rds/001_schema.sql`.
5. ⬜ Planned — copy `002_admin_template.sql` outside the repository, replace every placeholder with protected values, and apply it once.
6. ⬜ Planned — apply `database/rds/003_services_seed.sql`.
7. ⬜ Planned — verify TLS with the RDS CA and `VERIFY_IDENTITY`; snapshot before later migrations.

### C. Backend, Secrets, and ALB

1. ✅ Completed — build the image with `docker build -t ewallet-backend:<version> backend`.
2. ⬜ Planned — publish the versioned image to ECR or securely transfer it to EC2 for Phase 1.
3. ⬜ Planned — create the EC2 instance, instance role, Systems Manager access, CloudWatch agent/logging, and restart-managed container/service.
4. ⬜ Planned — store DB, JWT, and SES secrets in Secrets Manager/SSM; inject them without committing files.
5. ⬜ Planned — set the production environment variables listed above and `SPRING_PROFILES_ACTIVE=prod`.
6. ⬜ Planned — create an ALB target group using `/actuator/health` and register the EC2 target.
7. ⬜ Planned — create ACM certificate and HTTPS listener; redirect HTTP 80 to HTTPS 443.
8. ⬜ Planned — allow EC2 application ingress only from the ALB security group.
9. ⬜ Planned — verify health, authentication, role enforcement, CORS, forwarded HTTPS links, and wallet/admin smoke tests.

### D. Frontend

1. ✅ Completed — build with `VITE_API_BASE_URL=https://<api-hostname>` when the API uses a separate origin.
2. ⬜ Planned — create a private S3 bucket with block-public-access enabled.
3. ⬜ Planned — create CloudFront with origin access control, SPA fallback behavior, compression, and HTTPS.
4. ⬜ Planned — deploy `frontend/dist`, invalidate changed CloudFront paths, and configure Route 53/ACM.
5. ⬜ Planned — set exact `FRONTEND_BASE_URL` and `CORS_ALLOWED_ORIGINS`, then restart/redeploy the backend safely.

### E. SES, Observability, and Acceptance

1. ⬜ Planned — verify SES sender/domain and DKIM, create SMTP credentials, and request production access if required.
2. ⬜ Planned — verify registration and reset delivery plus bounce/complaint behavior.
3. ⬜ Planned — enable CloudWatch logs, ALB access logs, alarms, retention, and AWS Budget notifications.
4. ⬜ Planned — execute the testing checklist and backup/restore drill.
5. ⬜ Planned — record deployed URLs/resource IDs, version, schema level, test evidence, owner, and rollback target in this document.

Rollback: keep immutable frontend artifacts and backend image tags. Deregister an unhealthy backend version and restore the previous target/image. Database changes must be backward-compatible; take an RDS snapshot before migrations and never use destructive reset scripts in production.

## 16. Future Roadmap

## Phase 1 - ALB + Security

- ✅ Completed — expose public health, liveness, and readiness probes.
- ✅ Completed — support forwarded headers behind a reverse proxy.
- ✅ Completed — restrict CORS to explicit origins and isolate development endpoints.
- ✅ Completed — prepare a non-root backend image, production profile, environment templates, and RDS scripts.
- ✅ Completed — implement production SMTP delivery compatible with Amazon SES.
- 🟡 In Progress — achieve a completely green local verification run; one MySQL-dependent context test was blocked by the stopped local database on 2026-07-24.
- ⬜ Planned — provision VPC, public ALB subnets, backend subnet placement, route tables, and least-privilege security groups.
- ⬜ Planned — create private encrypted RDS, apply scripts, enforce TLS, backups, deletion protection, and restore testing.
- ⬜ Planned — create one EC2 backend runtime with instance role, SSM access, managed restart, and CloudWatch logs.
- ⬜ Planned — store JWT, DB, and SES secrets in Secrets Manager or encrypted SSM.
- ⬜ Planned — create ALB target group, `/actuator/health` check, HTTPS listener, ACM certificate, and HTTP redirect.
- ⬜ Planned — configure Route 53 API DNS and ensure EC2 accepts application traffic only from the ALB.
- ⬜ Planned — deploy the private S3/CloudFront frontend and configure exact frontend/API origins.
- ⬜ Planned — finish SES identity/DKIM/access setup and live verification/reset delivery testing.
- ⬜ Planned — add WAF or application/gateway rate limits for authentication, recovery, and wallet mutations.
- ⬜ Planned — add CSP/security headers, dependency/secret/container scanning, and credential-rotation procedures.
- ⬜ Planned — add idempotency, admin audit logs, transaction limits, and real-MySQL concurrency tests before any real-money consideration.
- ⬜ Planned — configure CloudWatch alarms, ALB access logs, retention, CloudTrail/security monitoring, backups, budgets, and operational runbooks.
- ⬜ Planned — complete cloud smoke/security tests and record deployment evidence.

## Phase 2 - High Availability

- ⬜ Planned — create a launch template from the validated Phase 1 backend image and bootstrap configuration.
- ⬜ Planned — place two EC2 instances in separate Availability Zones behind the existing ALB.
- ⬜ Planned — use an Auto Scaling Group with desired/minimum capacity of two and target-group health replacement.
- ⬜ Planned — move all instance-specific configuration/secrets to shared Secrets Manager/SSM sources.
- ⬜ Planned — ensure both instances use the same JWT secret, production profile, compatible image version, and RDS schema.
- ⬜ Planned — use rolling or instance-refresh deployment with ALB deregistration delay and readiness checks.
- ⬜ Planned — verify stateless behavior; do not add sticky sessions unless a demonstrated requirement appears.
- ⬜ Planned — load test the two-target system and tune DB connections, timeouts, JVM memory, health thresholds, and scaling policies.
- ⬜ Planned — terminate one instance and verify ALB continuity and automatic replacement.
- ⬜ Planned — evaluate Multi-AZ RDS after the application tier is stable and cost/availability requirements justify it.

## Phase 3 - CI/CD

- ⬜ Planned — add a pull-request workflow that installs pinned dependencies, runs frontend lint/build, runs all backend tests with a disposable MySQL service/Testcontainers, and uploads reports.
- ⬜ Planned — add dependency review, secret scanning, SAST, and container-image scanning.
- ⬜ Planned — add a protected production deployment environment with required review and GitHub OIDC; do not store long-lived AWS keys in GitHub.
- ⬜ Planned — frontend job: build once with the production API URL, upload the versioned artifact to private S3, invalidate CloudFront, and smoke test the distribution.
- ⬜ Planned — backend job: build/test, create a versioned container image, scan it, push it to ECR, and deploy using an EC2 rolling/instance-refresh strategy.
- ⬜ Planned — run database migrations as a separately approved, backward-compatible step before application rollout.
- ⬜ Planned — wait for ALB target health, run API/auth/CORS/health smoke tests, and automatically stop or roll back on failure.
- ⬜ Planned — retain build artifacts, image digests, commit SHA, migration version, deployment logs, and previous rollback version.
- ⬜ Planned — use concurrency controls so only one production deployment runs at a time.
- ⬜ Planned — separate frontend and backend path filters while retaining an explicit full-deployment option.

## Phase 4 - Container Orchestration

- ⬜ Planned — prefer Amazon ECS on Fargate after Phase 3 is stable.
- ⬜ Planned — push immutable backend images to ECR and define an ECS task with CPU/memory limits, health checks, CloudWatch logs, and Secrets Manager/SSM injection.
- ⬜ Planned — run an ECS service with at least two tasks across Availability Zones behind the existing ALB.
- ⬜ Planned — configure rolling deployment/circuit breaker, autoscaling, graceful shutdown, and deployment alarms.
- ⬜ Planned — retain S3/CloudFront for the frontend and RDS for MySQL; do not containerize the database.
- ⬜ Planned — update GitHub Actions to register a task-definition revision and deploy the ECS service through OIDC.
- ⬜ Planned — evaluate ECS capacity, cost, observability, and failure recovery before considering Kubernetes.
- ⬜ Planned — use Kubernetes/EKS only as a later learning phase: deployments, services, ingress/ALB controller, secrets integration, autoscaling, observability, network policy, upgrades, and cost management.
- ⬜ Planned — do not migrate to EKS merely for production readiness; choose it only when learning goals or workload/platform requirements justify the added operational burden.

## 17. Local Commands

Start local development:

```powershell
.\start-dev.ps1
```

Start or stop MySQL while preserving data:

```powershell
docker compose up -d
docker compose stop
```

Validate:

```powershell
cd frontend
npm run build
npm run lint

cd ..\backend
mvn clean test
```

Build backend image:

```powershell
docker build -t ewallet-backend:local backend
```

`docker compose down -v` deletes the local database volume and is not a routine command. Do not run it unless data deletion is explicitly intended.

For an existing local database created before email verification/reset fields, apply `database/migrations/V001__email_verification_and_password_reset.sql` once. Fresh RDS databases use the three numbered `database/rds` scripts instead.

## 18. Handoff Rules

- ✅ Completed — preserve the completed milestones above unless source evidence proves a regression.
- ✅ Completed — treat implementation and executed checks as authoritative over stale documentation.
- ✅ Completed — keep one shared frontend and keep core balance mutation logic in Spring Boot.
- ✅ Completed — preserve historical transactions and deactivate services instead of hard-deleting them.
- ✅ Completed — never commit credentials or reuse the JWT secret previously present in Git history.
- ✅ Completed — never destroy/recreate the local or production database as routine setup.
- ⬜ Planned — update this file with real AWS resource identifiers and verification evidence only after deployment actually occurs.
- ⬜ Planned — do not introduce real-money behavior until HTTPS, idempotency, audit, concurrency, webhook, reconciliation, limit, provider, legal, and operational prerequisites are complete.
