# Cloud E-Wallet Project Status

Last documentation review: 2026-07-25

- ✅ Completed — implemented in source or confirmed as the current production deployment.
- 🟡 In progress — active improvement phase, not yet complete.
- ⬜ Planned — not currently implemented.

This is a simulated e-wallet learning project and is not suitable for real money.

## Executive summary

| Area | Status | Current state |
| --- | --- | --- |
| User application | ✅ Completed | Registration/login, verification, profile, deposit, transfer, service payment, and history |
| Administration | ✅ Completed | Dashboard, user status, transactions, and service management |
| Verification policy | ✅ Completed | Unverified users retain account access; backend blocks wallet mutations |
| Production frontend | ✅ Completed | React on S3 through CloudFront at `cloud-ewallet.com` |
| Production backend | ✅ Completed | `chaukhoi/ewallet-backend:ses-v2` on EC2 using manual `docker run` |
| Database/email | ✅ Completed | Amazon RDS MySQL and Amazon SES |
| ALB/security hardening | 🟡 In progress | Active Phase 1 |
| High availability | ⬜ Planned | Phase 2 |
| CI/CD | ⬜ Planned | Phase 3; no current deployment automation |
| ECS/EKS | ⬜ Planned | Phase 4; EKS is optional learning scope |

## Completed

- ✅ Completed — registration, login, JWT authentication, and role-based authorization.
- ✅ Completed — email verification, resend, forgot-password, and reset-password flows.
- ✅ Completed — wallet balance, deposit/top-up, wallet transfer, service payment, and transaction history.
- ✅ Completed — profile viewing/editing for authenticated users, including unverified users.
- ✅ Completed — admin dashboard, user block/unblock, transaction inspection, and service management.
- ✅ Completed — BCrypt, protected APIs, blocked-user enforcement, and backend email-verification restrictions.
- ✅ Completed — current Route 53, CloudFront, S3, EC2/Docker, RDS MySQL, SES, Docker Hub, and Security Group baseline.
- ✅ Completed — manual production backend and frontend deployment workflow.

## In Progress

- 🟡 In Progress — Application Load Balancer implementation.
- 🟡 In Progress — ALB target group, listener, and health-check routing.
- 🟡 In Progress — Security Group hardening and restriction of direct EC2 access.

## Planned

- ⬜ Planned — two EC2 backend instances, load balancing, and failover demonstration.
- ⬜ Planned — GitHub Actions backend/frontend deployment automation.
- ⬜ Planned — ECS migration.
- ⬜ Planned — optional Kubernetes/EKS learning phase.

## Current application functionality

### Wallet

- ✅ Deposit/top-up of simulated funds
- ✅ Transfer between regular-user wallets
- ✅ Payment for active services using the backend-held price
- ✅ Authenticated, user-specific transaction history
- ✅ Transactional mutations, transaction recording, row locking, and relevant insufficient-balance checks

Deposit is an implemented feature, not a roadmap item.

### Authentication and email verification

- ✅ Users can register and log in while unverified.
- ✅ Unverified users can view account/profile information.
- ✅ Unverified users can resend the verification email.
- ✅ Unverified users cannot deposit, transfer, or pay for services.
- ✅ The backend enforces all three restrictions.
- ✅ The frontend shows a persistent verification banner and resend action.
- ✅ Axios handles the verification error globally without logging users out.

Exact backend response:

```text
HTTP 403
code: EMAIL_VERIFICATION_REQUIRED
message: Please verify your email before performing this action.
```

### API inventory

| Status | Method | Endpoint | Purpose |
| --- | --- | --- | --- |
| ✅ | POST | `/api/auth/register` | Create an unverified user, profile, wallet, and token |
| ✅ | POST | `/api/auth/login` | Authenticate and return JWT/account state |
| ✅ | POST | `/api/auth/verify-email` | Verify email |
| ✅ | POST | `/api/auth/resend-verification` | Resend verification |
| ✅ | POST | `/api/auth/forgot-password` | Request recovery |
| ✅ | POST | `/api/auth/reset-password` | Reset password |
| ✅ | GET/PATCH | `/api/account/me` | Read/update profile |
| ✅ | GET | `/api/user/wallet/me` | Wallet/account data |
| ✅ | POST | `/api/user/wallet/deposit` | Deposit/top-up |
| ✅ | POST | `/api/user/wallet/transfer` | Wallet transfer |
| ✅ | GET | `/api/user/wallet/transactions` | Transaction history |
| ✅ | GET | `/api/user/wallet/services` | Active services |
| ✅ | POST | `/api/user/wallet/payments` | Service payment |
| ✅ | GET | `/api/admin/dashboard` | Admin summary |
| ✅ | GET/PATCH | `/api/admin/users`, `/api/admin/users/{id}/status` | User management |
| ✅ | GET | `/api/admin/transactions` | System history |
| ✅ | GET/POST/PATCH | `/api/admin/services/**` | Service management |
| ✅ | GET | `/actuator/health/**` | Health probes |

## Current production architecture

```text
Users
  ↓
Route 53 / cloud-ewallet.com
  ↓
CloudFront
  ↓
S3 React frontend
  ↓ API requests
EC2
  ↓
Docker container: ewallet-backend
  ↓
Spring Boot backend
  ↓
Amazon RDS MySQL

Spring Boot backend → Amazon SES
```

| Component | Status | Current implementation |
| --- | --- | --- |
| Domain/DNS | ✅ Completed | Route 53 / `cloud-ewallet.com` |
| Frontend | ✅ Completed | S3 origin through CloudFront |
| Backend | ✅ Completed | Docker container on EC2 |
| Image | ✅ Completed | Docker Hub `chaukhoi/ewallet-backend:ses-v2` |
| Runtime | ✅ Completed | `ewallet-backend`, manual `docker run`, `8080:8080` |
| Configuration | ✅ Completed | `/home/ec2-user/ewallet-backend.env` |
| Database | ✅ Completed | Amazon RDS MySQL |
| Email | ✅ Completed | Amazon SES |
| Security Groups | ✅ Baseline / 🟡 hardening | Current access control; tighter ALB-to-EC2 rules are Phase 1 |
| Production Compose | ⬜ Not used | Compose is local/legacy deployment guidance |
| CI/CD | ⬜ Planned | Production deployment is manual |

## Current production deployment procedure

### Local

1. Run tests.
2. Build the frontend and run ESLint.
3. Build and push `chaukhoi/ewallet-backend:ses-v2` to Docker Hub.

### EC2

```bash
docker pull chaukhoi/ewallet-backend:ses-v2
docker stop ewallet-backend
docker rm ewallet-backend
docker run -d \
  --name ewallet-backend \
  --env-file /home/ec2-user/ewallet-backend.env \
  -p 8080:8080 \
  --restart unless-stopped \
  chaukhoi/ewallet-backend:ses-v2
```

### Frontend

1. Upload `frontend/dist/` contents to S3.
2. Invalidate CloudFront path `/*`.

## Latest validation

- ✅ Repository inventory: 55 backend test methods in eight test classes.
- ✅ 43 focused authentication, JWT, profile-access, resend, and wallet tests passed.
- ✅ Final wallet safety rerun: 12/12 passed.
- ✅ Frontend production build passed.
- ✅ Frontend ESLint passed.

The inventory count describes tests present in source. The 43 focused tests and the separate 12/12 wallet safety rerun are the latest recorded completed executions; they must not be added together as though they were one suite run.

## Roadmap

### Phase 1 — 🟡 In progress

- Application Load Balancer
- Security Group hardening
- Restrict direct EC2 access
- HTTPS between client and ALB
- Health checks

### Phase 2 — ⬜ Planned

- Two EC2 instances behind ALB
- High availability
- Failover demonstration

### Phase 3 — ⬜ Planned

- GitHub Actions CI/CD
- Automated backend Docker build and push
- Automated EC2 deployment
- Automated frontend build, S3 sync, and CloudFront invalidation

### Phase 4 — ⬜ Planned

- Amazon ECS migration
- Optional Kubernetes/EKS learning phase

## Historical and legacy guidance

The local Docker Compose/MySQL workflow remains useful for development. Older material describing AWS as entirely future work, Docker Compose in production, ECR, App Runner, or an already-present ALB is legacy/target guidance. The current baseline is S3/CloudFront + one EC2 Docker container + RDS + SES, deployed manually.

No Infrastructure as Code or `.github/workflows` deployment automation is present in this repository.

## Environment overview

| Area | Local | Production |
| --- | --- | --- |
| Frontend | Vite at the configured local origin | S3 static assets through CloudFront |
| Backend | Spring `local` profile | Spring `prod` profile in EC2 container |
| Database | MySQL 8 through local Docker Compose | Amazon RDS MySQL |
| Email | Verification/reset links logged by development adapter | Authenticated STARTTLS through Amazon SES SMTP |
| Runtime configuration | `.env.local` based on `.env.example` | `/home/ec2-user/ewallet-backend.env` |

Production variables cover the database connection, JWT secret/lifetime, frontend and CORS origins, account-token lifetimes, and SES SMTP credentials/from-address. Secrets are external to the image and repository.

## Security overview

| Control | Status | Evidence in implementation |
| --- | --- | --- |
| BCrypt passwords | ✅ Completed | Registration/reset hash passwords; login verifies hashes |
| JWT authentication | ✅ Completed | Signed expiring tokens and stateless security filter |
| Role authorization | ✅ Completed | Spring Security separates admin, user-wallet, and account APIs |
| Blocked-user protection | ✅ Completed | Login and protected requests reject blocked accounts |
| Verification restriction | ✅ Completed | Deposit, transfer, and payment return the documented 403 |
| Financial mutation safety | ✅ Completed | Spring transactions, wallet row locks, and balance checks |
| Token safety | ✅ Completed | Verification/reset tokens are hashed, expiring, and single-use |
| Production mail transport | ✅ Completed | SES SMTP authentication with required STARTTLS |
| Network hardening | 🟡 In Progress | Security Group tightening and removal of direct EC2 exposure |

## Production readiness

The application is deployed and functionally usable as a demonstration system. It has baseline application security, health endpoints, a non-root backend image, environment-driven configuration, RDS scripts, and successful focused validation. It is not production-ready for real money. Before stronger availability claims, Phase 1 must put the backend behind an ALB, restrict direct EC2 access, finalize Security Group paths, and verify target health. Phase 2 is required for instance-level redundancy and demonstrated failover.

## Future architecture

### Phase 1

```text
Users → Route 53 / cloud-ewallet.com → CloudFront → S3 React frontend
                                      ↓ API
                            Application Load Balancer
                                      ↓ health-checked target
                              EC2 Docker backend
                               ├→ RDS MySQL
                               └→ Amazon SES
```

### Phase 2

```text
                            Application Load Balancer
                              ├→ EC2 backend A ─┐
Users / API requests ────────┤                 ├→ Amazon RDS MySQL
                              └→ EC2 backend B ─┘

Each Spring Boot backend → Amazon SES
```

These are planned diagrams. The current production diagram earlier in this document remains the deployed baseline.
