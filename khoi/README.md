# Cloud E-Wallet

A Spring Boot and React demonstration e-wallet deployed at `cloud-ewallet.com`. It uses simulated funds and is not suitable for real-money use.

## Status

- ✅ Completed — authentication, email verification, profiles, wallet operations, transaction history, administration, and the existing AWS application deployment.
- Code migration completed locally; production Resend configuration and deployment remain pending.
- 🟡 In progress — Application Load Balancer and production security hardening.
- ⬜ Planned — high availability, CI/CD, and ECS migration.

See [PROJECT_STATUS.md](PROJECT_STATUS.md) for the detailed status and roadmap.

## Technology stack

| Layer | Current implementation |
| --- | --- |
| Frontend | React 19, TypeScript, Vite, React Router, Axios, Zustand, Zod |
| Backend | Java 17, Spring Boot, Spring Security, JDBC, Actuator, Spring Mail |
| Authentication | BCrypt password hashing and signed, expiring JWT access tokens |
| Database | MySQL 8 locally; Amazon RDS MySQL in production |
| Containers | Docker; local MySQL uses Compose, production backend uses `docker run` |
| AWS | Route 53, CloudFront, S3, EC2, RDS MySQL, Security Groups; outbound SMTP to Resend is pending |
| Image registry | Docker Hub |

## Implemented wallet functionality

- ✅ Deposit/top-up of simulated funds (`POST /api/user/wallet/deposit`)
- ✅ Wallet-to-wallet transfer (`POST /api/user/wallet/transfer`)
- ✅ Active-service payment (`POST /api/user/wallet/payments`)
- ✅ Authenticated wallet transaction history (`GET /api/user/wallet/transactions`)

Deposit is implemented now, not a future feature. Deposit, transfer, and payment use Spring transactions, update balances, and record transactions. Transfer and payment enforce sufficient funds; payment uses the service price stored by the backend.

## Email verification flow

- Users may register and log in before email verification.
- Unverified users may view profile/account information and resend the verification email.
- Unverified users may view wallet information/history but cannot deposit, transfer, or pay for services.
- The backend enforces the restriction on all three wallet mutations.
- The React application displays a persistent verification banner with a resend action.
- Axios handles the verification error globally without clearing the session or logging the user out.

Blocked mutations return:

```text
HTTP 403
code: EMAIL_VERIFICATION_REQUIRED
message: Please verify your email before performing this action.
```

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

Spring Boot backend → outbound SMTP → Resend (pending production configuration)
```

Current deployment:

- Frontend: S3, served through CloudFront
- Backend: Docker container on EC2
- Docker Hub image: `chaukhoi/ewallet-backend:ses-v2`
- Container: `ewallet-backend`
- Runtime: manual `docker run`, not Docker Compose or CI/CD
- Environment: `/home/ec2-user/ewallet-backend.env`
- Port mapping: `8080:8080`
- Database: Amazon RDS MySQL
- Email: the deployed `ses-v2` image is the legacy production state; Resend deployment and delivery verification are pending
- Domain: `cloud-ewallet.com`
- Network controls: AWS Security Groups

### AWS services

| Service | Status | Role |
| --- | --- | --- |
| Route 53 | ✅ Completed | DNS for `cloud-ewallet.com` |
| CloudFront | ✅ Completed | Delivers the React application |
| S3 | ✅ Completed | Hosts the frontend static build |
| EC2 | ✅ Completed | Runs the backend Docker container |
| RDS MySQL | ✅ Completed | Production relational database |
| Resend SMTP | ⬜ Pending production configuration | Intended verification and password-reset provider; code migration is complete locally |
| Security Groups | ✅ Completed baseline; 🟡 hardening | Control network access; tighter EC2 exposure is Phase 1 |
| Application Load Balancer | 🟡 In progress | Planned public backend entry point and health routing |
| ECS | ⬜ Planned | Phase 4 container orchestration target |

## Environment overview

| Environment | Frontend | Backend | Database | Email |
| --- | --- | --- | --- | --- |
| Local | Vite development server | Spring `local` profile | MySQL 8 through Docker Compose on host port 3307 | Development service logs links |
| Production | Static `dist/` on S3 through CloudFront | Spring `prod` profile in EC2 Docker container | Amazon RDS MySQL | Resend SMTP intended; configuration/deployment pending |

Important backend environment variables:

| Variable | Purpose |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | Selects `local` or `prod` |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | JDBC connection |
| `JWT_SECRET`, `JWT_EXPIRATION` | JWT signing and lifetime |
| `FRONTEND_BASE_URL` | Verification/reset link origin |
| `CORS_ALLOWED_ORIGINS` | Explicit frontend origins |
| `EMAIL_VERIFICATION_MINUTES`, `PASSWORD_RESET_MINUTES` | Account-token lifetimes |
| `SMTP_HOST`, `SMTP_PORT` | SMTP endpoint; defaults are `smtp.resend.com` and `587` |
| `SMTP_USERNAME` | SMTP username; defaults to `resend` |
| `SMTP_PASSWORD` | Required Resend API key used as the SMTP password |
| `MAIL_FROM_ADDRESS` | Required sender address on the verified Resend domain |

The production values are loaded from `/home/ec2-user/ewallet-backend.env`; populated secret files must not be committed.

## Security overview

- ✅ Passwords are hashed with BCrypt.
- ✅ Protected APIs require signed, expiring JWTs.
- ✅ Spring Security enforces authenticated, `USER`, and `ADMIN` route boundaries.
- ✅ The JWT filter reloads account status and role from MySQL, so a blocked user or changed role is enforced on subsequent requests.
- ✅ Blocked accounts cannot log in or continue using protected APIs.
- ✅ Deposit, transfer, and payment require a verified email in the backend.
- ✅ Wallet mutations use database transactions and row locks.
- ✅ Wallet balances and history are scoped to the authenticated user.
- ✅ Verification/reset tokens are stored as hashes, expire, and are single-use.
- ✅ Production code requires authenticated STARTTLS (enabled and required) through provider-neutral SMTP configuration.
- ✅ Actuator exposes only `health` and `info`; health details are hidden, and SMTP secrets are not exposed.
- 🟡 Security Group hardening and restriction of direct EC2 access are Phase 1.

## Pending Resend production deployment procedure

Current status:

```text
Code migration completed locally
Production Resend configuration pending
Production deployment pending
Production email delivery not yet verified
```

Before deployment, verify `cloud-ewallet.com` in Resend by adding the records Resend supplies to Cloudflare DNS. These email records do not replace or modify the existing website DNS record that sends web traffic to CloudFront/S3.

### Local

1. Run backend tests.
2. Run the frontend production build and ESLint.
3. Build and push the backend:

```powershell
docker build -t chaukhoi/ewallet-backend:resend-v1 backend
docker push chaukhoi/ewallet-backend:resend-v1
```

### EC2

```bash
docker pull chaukhoi/ewallet-backend:resend-v1
docker stop ewallet-backend
docker rm ewallet-backend
docker run -d \
  --name ewallet-backend \
  --env-file /home/ec2-user/ewallet-backend.env \
  -p 8080:8080 \
  --restart unless-stopped \
  chaukhoi/ewallet-backend:resend-v1
```

These commands are documentation for a later task; this migration did not build, push, or deploy the image. EC2 initiates an outbound STARTTLS connection to `smtp.resend.com:587`; no inbound SMTP port needs to be opened. Production continues to use manual `docker run`.

### Frontend

1. Run `npm run build` in `frontend/`.
2. Upload the contents of `frontend/dist/` to S3.
3. Create a CloudFront invalidation for `/*`.

The repository does not contain the production bucket or distribution identifiers, so they are not invented here.

## Local development

Copy `.env.example` to `.env.local`, replace development secrets, then run:

```powershell
.\start-dev.ps1
```

Docker Compose is used for the local MySQL environment only. The frontend defaults to `http://localhost:8080` in Vite development unless `VITE_API_BASE_URL` is set.

The production backend uses the `prod` Spring profile and environment-driven database, JWT, CORS/frontend, token, and provider-neutral SMTP settings. Resend is the intended provider. The Java 17 image runs as a non-root user and exposes port 8080. Health endpoints are `/actuator/health`, `/actuator/health/liveness`, and `/actuator/health/readiness`.

For normal local development, no Resend credentials are needed: `.\start-dev.ps1` selects `local`, and verification/reset links are logged. To perform an optional local SMTP smoke test, start the backend with the `prod` profile and temporary `SMTP_PASSWORD`, `MAIL_FROM_ADDRESS`, database, JWT, frontend, and CORS values in the current shell. Never store the API key in a committed file.

For a fresh RDS database, apply `database/rds/001_schema.sql`, a securely completed copy of `002_admin_template.sql`, and `003_services_seed.sql`, in that order. `database/schema.sql` is local-only; `database/fix_services_utf8.sql` is legacy.

## Latest validation

- ✅ Full backend suite: 60 tests across nine test classes; zero failures, errors, or skips (`mvn test`, 2026-07-25).
- ✅ Backend package build succeeded and produced `target/ewallet-0.0.1-SNAPSHOT.jar` (`mvn package -DskipTests`, 2026-07-25).
- ✅ Frontend production build passed.
- ✅ Frontend ESLint passed.

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

### Future architecture after Phases 1–2

```text
Users
  ↓ HTTPS
Route 53 / cloud-ewallet.com
  ↓
CloudFront ─────────────→ S3 React frontend
  ↓ API requests
Application Load Balancer
  ├── health check → EC2 backend instance A ─┐
  └── health check → EC2 backend instance B ─┼→ Amazon RDS MySQL
                                              └→ outbound SMTP to Resend (pending)
```

The ALB, second EC2 instance, and automated failover in this diagram are targets, not current components.
