# Cloud E-Wallet Project Status

Last repository audit: 2026-07-27

Cloud E-Wallet is a simulated-funds learning application, not a real-money wallet.

## Executive summary

| Area | Status | Current state |
| --- | --- | --- |
| Customer application | Completed | Authentication, email/account recovery, profile, wallet operations, recipient preview, services, history |
| Administration | Completed | Dashboard, users, block/unblock, transactions, and service lifecycle |
| Production frontend | Completed | Responsive `frontend/` build on S3 through CloudFront |
| Production backend | Completed | One EC2 instance, container `ewallet-backend`, manual Docker |
| Data and mail | Completed | Amazon RDS MySQL and verified Resend SMTP flows |
| ALB and hardening | Planned next | Not deployed |
| HA, CI/CD, orchestration | Future | Not deployed |

Repository evidence identifies the deployed image as `chaukhoi/ewallet-backend:ses-v2`. Although the tag contains legacy provider wording, the active runtime configuration is provider-neutral SMTP and production mail uses Resend. There is no repository evidence for a newer deployed tag.

## Completed

- Local MVP and MySQL Docker Compose workflow.
- BCrypt registration/login and migration to signed, expiring JWT sessions.
- Logout and current-account/profile read/update.
- Registration verification, resend verification, forgot-password, and reset-password.
- Hashed, expiring, single-use account tokens.
- Backend enforcement of verified-email requirements for deposit, transfer, and payment.
- Wallet balance, simulated deposit, transfer, service payment, and user transaction history.
- Authenticated recipient-name lookup by phone with eligibility and self-recipient protection.
- Admin dashboard, user listing and block/unblock, transaction management, and service add/edit/activate/deactivate.
- Responsive frontend redesign in the active `frontend/` directory.
- Customer/admin deposit sender label `Bank Card`.
- Read-only transfer-recipient display that clears on changed, invalid, unavailable, or self-owned phone numbers.
- Service-card banners using actual service names and removal of visible demo labels.
- Production frontend on S3/CloudFront, Cloudflare DNS, backend Docker on one EC2, RDS MySQL, and Resend SMTP.
- Manual backend and frontend deployment procedures.

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
Docker: ewallet-backend
  ↓
Spring Boot
  ├── Amazon RDS MySQL
  └── Resend SMTP (outbound 587/STARTTLS)
```

| Component | Current implementation |
| --- | --- |
| Domain | `https://cloud-ewallet.com` |
| DNS | Cloudflare |
| Frontend | Amazon S3 origin through CloudFront |
| Backend | One EC2 instance, manual `docker run`, port mapping `8080:8080` |
| Container | `ewallet-backend` |
| Image | `chaukhoi/ewallet-backend:ses-v2` (legacy name, only deployed tag evidenced) |
| Runtime env file | `/home/ec2-user/ewallet-backend.env`, outside the repository |
| Database | Amazon RDS MySQL |
| Email | Resend SMTP with authenticated STARTTLS |

## Current architecture limitations

- One EC2 backend instance and no application-tier redundancy.
- No Application Load Balancer, target group, or ALB health-check routing yet.
- Direct backend exposure cannot be claimed removed until the ALB/security-group phase is completed.
- No multi-instance high availability or Auto Scaling.
- The repository proves Amazon RDS MySQL is active but does not prove Multi-AZ; no Multi-AZ claim is made.
- Backend deployment is manual Docker; frontend deployment is manual S3 upload plus CloudFront invalidation.
- No CI/CD, Infrastructure as Code, centralized observability, Secrets Manager, or Parameter Store integration.

## Environment decision

No consolidation was performed. Local backend/runtime, frontend build-time, and production EC2 runtime configuration use different loading mechanisms. Keeping them separate reduces the risk of leaking secrets or breaking deployment.

- `.env.example`: canonical safe local template.
- `.env.local`: ignored local runtime file; preserved.
- `.env.production.example`: canonical safe production template.
- `frontend/.env.production`: ignored Vite build-time file; preserved.
- `/home/ec2-user/ewallet-backend.env`: external production runtime file; unchanged.

Active application variables are `SPRING_PROFILES_ACTIVE`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_EXPIRATION`, `FRONTEND_BASE_URL`, `CORS_ALLOWED_ORIGINS`, `MAIL_DEVELOPMENT_LOG_ENABLED`, `EMAIL_VERIFICATION_MINUTES`, `PASSWORD_RESET_MINUTES`, `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `MAIL_FROM_ADDRESS`, and `VITE_API_BASE_URL`.

No active `SES_SMTP_*` variables remain. SMTP defaults are Resend-specific only where appropriate: host `smtp.resend.com`, port 587, and username `resend`.

## Security controls

| Control | Implementation |
| --- | --- |
| Password protection | BCrypt hashing on registration/reset and verification on login |
| Authentication | Signed, expiring JWT; stateless Spring Security |
| Authorization | Role rules for admin and user-wallet routes |
| Blocked users | Login and protected requests enforce current database status |
| Email restrictions | Backend blocks wallet mutations for unverified users |
| Account tokens | Hashed, expiring, single-use |
| Wallet consistency | Spring transactions, row locks, sufficient-balance checks |
| RDS network | Private access/security-group relationship with EC2 |
| Mail transport | Resend authentication and required STARTTLS |
| Repository secrets | Populated env files ignored; templates use placeholders |
| Card form | Presentation-only; sensitive card fields never reach the backend |

## Database

Tables: `users`, `account_tokens`, `user_profiles`, `admin_profiles`, `wallets`, `services`, and `transactions`.

Fresh RDS seed order:

1. `database/rds/001_schema.sql`
2. a completed but uncommitted copy of `database/rds/002_admin_template.sql`
3. `database/rds/003_services_seed.sql`

`database/schema.sql` resets and seeds local development. `database/migrations/V001__email_verification_and_password_reset.sql` upgrades the earlier schema. `database/fix_services_utf8.sql` repairs legacy service text only.

## Validation from this audit

Commands run on 2026-07-27 during the final audit:

- Backend `mvn test`: passed — 68 tests, 0 failures, 0 errors, 0 skipped.
- Backend `mvn package -DskipTests`: passed — produced `backend/target/ewallet-0.0.1-SNAPSHOT.jar`.
- Frontend `npm install`: passed — dependencies already up to date.
- Frontend `npm run build`: passed — TypeScript and Vite production build completed.
- Frontend `npm run lint`: passed — ESLint reported no errors.
- Frontend automated tests: no test script exists in `frontend/package.json`.

## Next immediate phase

Planned, not completed:

1. Create an Application Load Balancer.
2. Create a target group and configure `/actuator/health` health checks.
3. Register the current EC2 backend and verify target health.
4. Route backend traffic through the ALB.
5. Harden security groups so the backend accepts application traffic only from the ALB.
6. Capture architecture evidence and write the deployment report.

Do not claim the ALB is complete until listener routing, target health, client/API behavior, and security-group restrictions are verified.

## Future improvements

- Second EC2 backend instance.
- Multi-AZ application tier and Auto Scaling.
- CI/CD.
- ECS or Fargate.
- EKS/Kubernetes.
- Centralized logging, metrics, tracing, and alerting.
- Secrets Manager or Systems Manager Parameter Store.

## Audit boundary

No infrastructure or application deployment action occurred during this repository audit.
