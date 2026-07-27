# Cloud E-Wallet

Cloud E-Wallet is a deployed demonstration e-wallet. It uses simulated balances and a presentation-only card form; it is not a real-money or payment-card system.

Production URL: `https://cloud-ewallet.com`

## Current status

- The responsive React frontend in `frontend/` is deployed to Amazon S3 and delivered by CloudFront.
- Cloudflare manages DNS for `cloud-ewallet.com`.
- A Java 17 Spring Boot backend runs in the Docker container `ewallet-backend` on one Amazon EC2 instance.
- Amazon RDS MySQL is the production database.
- Resend SMTP is active for registration verification, resend verification, forgot-password, and reset-password mail.
- Production deployment is manual. There is no ALB, second backend instance, Auto Scaling, ECS, EKS, or CI/CD pipeline.

The current selected production backend image is `chaukhoi/ewallet-backend:v3`.

See [PROJECT_STATUS.md](PROJECT_STATUS.md) for the completed scope, limitations, validation results, and roadmap.

## Stack

| Layer | Implementation |
| --- | --- |
| Frontend | React 19, TypeScript, Vite, React Router, Axios, Zustand, Zod |
| Backend | Java 17, Spring Boot, Spring Security, JDBC, Actuator, Spring Mail |
| Authentication | BCrypt and signed, expiring JWT access tokens |
| Data | MySQL 8 locally; Amazon RDS MySQL in production |
| Runtime | Docker Compose for local MySQL; manual Docker on EC2 in production |
| Mail | Local link logging; Resend SMTP in production |

## Implemented features

Customers can register, log in and out, verify or resend verification email, recover/reset a password, view/update a profile, view a wallet balance, deposit simulated funds, transfer funds, preview an eligible recipient name by phone, pay active services, and view transaction history.

Administrators can view the dashboard, list and block/unblock users, inspect transactions, and add, edit, activate, or deactivate services.

Deposit senders display as `Bank Card` in customer and admin transaction views. Transfer recipient preview uses authenticated `GET /api/user/wallet/recipient`; the read-only result clears when the number changes or is invalid, missing, blocked, wallet-less, an admin, or the current user. Service banners render the actual service name. Visible demo labels were removed from the deployed UI.

The card number, cardholder name, expiry, CVV, and funding-source fields exist only in temporary React state. The deposit request sends only `amount` and optional `description`; card data is not sent to the backend, persisted, or added to transaction history.

## Current architecture

```text
Users
  ↓
Cloudflare DNS
  ↓
CloudFront
  ↓
Amazon S3 React frontend
  ↓ API requests
Amazon EC2
  ↓
Docker container: ewallet-backend
  ↓
Spring Boot backend
  ├── Amazon RDS MySQL
  └── Resend SMTP (outbound STARTTLS on port 587)
```

No inbound SMTP port is required. The EC2 backend initiates the outbound SMTP connection.

## Project structure

```text
khoi/
├── .codex/
├── .vscode/
├── backend/
├── database/
├── frontend/
├── .env.example
├── .env.local
├── .env.production.example
├── .gitignore
├── description.md
├── docker-compose.yml
├── ewallet-backend.env
├── PROJECT_STATUS.md
├── README.md
├── start-dev.ps1
└── tempCodeRunnerFile.ps1
```

`backend/`, `database/`, and `frontend/` are directories. The four backend/local environment files are in the project root, beside those directories.

## Environment file structure

Environment files remain separate because local PowerShell/Spring runtime configuration, production Spring runtime configuration, and the Vite build have different consumers.

| File | Exact location | Purpose | Real values | Git policy |
| --- | --- | --- | ---: | --- |
| `.env.example` | Project root | Safe local-development template | No | Tracked |
| `.env.local` | Project root | Real local backend/runtime configuration loaded by `start-dev.ps1` | Yes | Ignored |
| `.env.production.example` | Project root | Safe production template containing required variable names and placeholders | No | Tracked |
| `ewallet-backend.env` | Project root | Real production backend configuration prepared locally for EC2 deployment | Yes | Ignored; never commit |
| `frontend/.env.production` | Inside `frontend/` | Vite production build-time API origin | Public build-time values only | Follow current Git policy |
| `/home/ec2-user/ewallet-backend.env` | EC2 filesystem | Secure runtime copy used by the Docker backend container | Yes | Outside Git |

### `.env.example`

`.env.example` is the safe local template in the project root. It contains development defaults or placeholders, is safe to commit, and must not contain real production credentials. Copy it to `.env.local` and fill in the local values:

```text
.env.example
      ↓ copy and fill
.env.local
      ↓ loaded by
start-dev.ps1
```

### `.env.local`

`.env.local` is in the project root and contains real local values. `start-dev.ps1` loads it for the local backend/runtime. It may contain a local JWT secret and local database configuration. It must be ignored by Git and must never be committed, uploaded, or included in screenshots. Do not print its values.

For local development, ensure Docker Desktop is running and then run:

```powershell
.\start-dev.ps1
```

The script starts MySQL 8 from `docker-compose.yml` on host port 3307, starts the backend with the `local` profile, and starts the React frontend. Local email verification and reset links are logged; SMTP credentials are not required.

### `.env.production.example`

`.env.production.example` is the safe production template in the project root. It documents required environment-variable names using placeholders rather than real credentials, so it is safe to commit. The deployed container does not use this template directly unless it has first been copied and filled:

```text
.env.production.example
      ↓ use as template
ewallet-backend.env
```

### `ewallet-backend.env`

`ewallet-backend.env` is located in the project root at the same directory level as `backend/`, `database/`, `frontend/`, `README.md`, and `docker-compose.yml`. It follows `.env.production.example`, but contains the real production backend values and is prepared locally before deployment.

This file must be ignored by Git and never committed, printed to logs, or shown in screenshots. It must not contain frontend-only Vite variables. Upload it securely to EC2:

```text
.env.production.example
      ↓ copy and fill locally
ewallet-backend.env
      ↓ securely upload to EC2
/home/ec2-user/ewallet-backend.env
      ↓ loaded through Docker --env-file
ewallet-backend container
```

### EC2 runtime copy

`/home/ec2-user/ewallet-backend.env` is not a different configuration format. It is the deployed EC2 copy of the root-level `ewallet-backend.env`. The filenames are identical, but the files are stored in different locations:

```text
Local project:
C:\Users\DELL\Desktop\intern\khoi\ewallet-backend.env

EC2:
/home/ec2-user/ewallet-backend.env
```

No secret values should appear in documentation, logs, or screenshots.

### Production Docker command

The current selected backend image is `chaukhoi/ewallet-backend:v3`:

```bash
docker run -d \
  --name ewallet-backend \
  --env-file /home/ec2-user/ewallet-backend.env \
  -p 8080:8080 \
  --restart unless-stopped \
  chaukhoi/ewallet-backend:v3
```

Production continues to use one EC2 Docker backend and manual `docker run`.

### Frontend environment configuration

`frontend/.env.production` belongs only to the Vite frontend build process. It is inside `frontend/`, commonly defines `VITE_API_BASE_URL`, and is read during `npm run build`. Vite embeds its values into public frontend assets. Spring Boot does not read it, and Docker does not load it through `--env-file`.

It must never contain `DB_PASSWORD`, `JWT_SECRET`, `SMTP_PASSWORD`, Resend API keys, or any database credentials.

```text
frontend/.env.production
      ↓ read during
npm run build
      ↓ embedded into
frontend/dist
      ↓ uploaded to
Amazon S3
```

### Root environment files compared

```text
.env.example
→ safe local template

.env.local
→ real local runtime configuration

.env.production.example
→ safe production template

ewallet-backend.env
→ real production backend configuration
```

These four root files must not be merged because they serve different purposes.

### Git safety checks

```powershell
git check-ignore -v .env.local ewallet-backend.env frontend/.env.production
git status --short
```

`.env.local` and `ewallet-backend.env` should be ignored. Neither real configuration file should appear as an untracked file in `git status`.

If `ewallet-backend.env` is already tracked, remove it from Git tracking while keeping the local file:

```powershell
git rm --cached ewallet-backend.env
```

## Environment variables

| Variable | Local | Production | Secret | Source default / requirement |
| --- | ---: | ---: | ---: | --- |
| `SPRING_PROFILES_ACTIVE` | Yes | Yes | No | Select `local` or `prod` |
| `DB_URL` | Optional | Yes | No | Local profile supplies MySQL port 3307 URL |
| `DB_USERNAME` | Optional | Yes | No | Local default `ewallet_user` |
| `DB_PASSWORD` | Optional | Yes | Yes | Local development default only; no production default |
| `JWT_SECRET` | Yes | Yes | Yes | Required; at least 32 bytes |
| `JWT_EXPIRATION` | Optional | Optional | No | 3600 seconds; minimum 60 |
| `FRONTEND_BASE_URL` | Optional | Yes | No | Local default `http://localhost:5173` |
| `CORS_ALLOWED_ORIGINS` | Optional | Yes | No | Local default; production must use explicit origins |
| `MAIL_DEVELOPMENT_LOG_ENABLED` | Optional | Fixed false by prod profile | No | Local default true |
| `EMAIL_VERIFICATION_MINUTES` | Optional | Optional | No | 1440 |
| `PASSWORD_RESET_MINUTES` | Optional | Optional | No | 30 |
| `SMTP_HOST` | No | Optional | No | `smtp.resend.com` |
| `SMTP_PORT` | No | Optional | No | 587 |
| `SMTP_USERNAME` | No | Optional | No | `resend` |
| `SMTP_PASSWORD` | No | Yes | Yes | Required by prod startup validation |
| `MAIL_FROM_ADDRESS` | No | Yes | No | Required by prod startup validation |
| `VITE_API_BASE_URL` | Optional | Conditional | No | Development and production fallbacks described above |

The Maven wrapper additionally recognizes its standard bootstrap variables (`JAVA_HOME`, `MAVEN_USER_HOME`, `MVNW_REPOURL`, `MVNW_USERNAME`, `MVNW_PASSWORD`, and `MVNW_VERBOSE`); they are tooling variables, not application runtime configuration.

## Security

- BCrypt hashes registration/reset passwords.
- JWT authentication is stateless and role authorization separates user, admin, and account routes.
- The JWT filter reloads role and status from MySQL, enforcing blocked users on later requests.
- The backend blocks deposit, transfer, and payment until email verification.
- Verification/reset tokens are SHA-256 hashes, expire, and are single-use.
- Wallet mutations use Spring transactions and wallet row locking; transfer/payment enforce sufficient funds.
- RDS access is controlled by its private network/security-group relationship with EC2.
- Production SMTP requires authentication and STARTTLS.
- Secret-bearing environment files are ignored; safe templates contain placeholders only.
- Card number, CVV, expiry, cardholder, and funding-source values are never transmitted to the backend.

## Database

Current tables are `users`, `account_tokens`, `user_profiles`, `admin_profiles`, `wallets`, `services`, and `transactions`.

- Local reset/seed: `database/schema.sql`.
- Fresh RDS order: `database/rds/001_schema.sql`, a securely completed uncommitted copy of `002_admin_template.sql`, then `003_services_seed.sql`.
- Existing pre-token databases may use `database/migrations/V001__email_verification_and_password_reset.sql`.
- `database/fix_services_utf8.sql` is a targeted repair script for legacy mojibake service rows, not a normal fresh-install step.

## Build and validation

Backend:

```powershell
cd backend
mvn test
mvn package -DskipTests
```

Frontend:

```powershell
cd frontend
npm install
npm run build
npm run lint
```

There is no frontend automated test script. Validation results from this audit are recorded in `PROJECT_STATUS.md`.

## Deployment boundary

This repository audit did not build/push a production image, restart EC2, alter the EC2 environment file, upload to S3, invalidate CloudFront, change Cloudflare, RDS, Resend, or security groups, or create ALB/ECS/CI/CD resources.
