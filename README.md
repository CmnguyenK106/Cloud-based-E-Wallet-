# Cloud E-Wallet

Cloud E-Wallet is a deployed demonstration e-wallet using simulated balances. It is not a real-money wallet or payment-card system.

Production URL: `https://cloud-ewallet.com`

See [PROJECT_STATUS.md](PROJECT_STATUS.md) for deployment verification, current limitations, and the roadmap.

## Technology

| Layer | Implementation |
| --- | --- |
| Frontend | React 19, TypeScript, Vite, React Router, Axios, Zustand, Zod |
| Backend | Java 17, Spring Boot, Spring Security, JDBC, Actuator, Spring Mail |
| Authentication | BCrypt passwords and signed, expiring JWT access tokens |
| Data | MySQL 8 locally; Amazon RDS MySQL in production |
| Runtime | Docker Compose for local MySQL; Dockerized Spring Boot on EC2 |
| Email | Development link logging locally; Amazon SES SMTP in production; Resend fallback |

## Implemented functionality

Customers can register, verify or resend verification email, log in and out, recover/reset a password, view and update a profile, view a wallet balance, look up an eligible recipient by phone number, deposit simulated funds, transfer funds, pay active services, and view transaction history.

Administrators can view the dashboard, list and block/unblock users, inspect transactions, and add, edit, activate, or deactivate services. The JWT filter reloads account role and status from MySQL, so a blocked account is rejected on subsequent protected requests; the frontend clears the session on `401` or `ACCOUNT_BLOCKED`.

The responsive frontend and database use UTF-8/`utf8mb4` for Vietnamese content.

### Deposit boundary

Deposit is a simulation. Card number, cardholder name, expiry, CVV, and funding source exist only in temporary React state. The frontend sends only `amount` and an optional `description`; no card data is sent to the backend or stored. There is no bank, card processor, or real payment gateway integration.

## Production architecture

```text
Users
  |
  v
Cloudflare DNS
  |
  v
Amazon CloudFront
  |-- Default behavior (*) --> Amazon S3 React frontend
  |
  `-- /api/* behavior --> Application Load Balancer
                             |
                             v
                        Amazon EC2
                        Dockerized Spring Boot backend
                             |
                             +--> Amazon RDS MySQL
                             |
                             `--> Amazon SES SMTP
```

- Browser-to-CloudFront traffic uses HTTPS.
- CloudFront sends `/api/*` to the internet-facing ALB over HTTP port 80.
- The ALB forwards requests to the backend target group on port 8080.
- Target group health checks use `/actuator/health`; the registered EC2 target was verified healthy.
- The previous direct CloudFront-to-EC2 API origin has been removed.
- The backend runs in Docker on one EC2 instance. The ALB is ready for additional targets, but one target does not provide full backend high availability.
- Amazon RDS MySQL remains in private subnets.
- Spring Mail uses provider-neutral SMTP settings. `EMAIL_PROVIDER` selects Amazon
  SES (the production default) or Resend (fallback) without changing business
  logic. Both use authenticated STARTTLS on port 587.

## EC2 and network security

The current EC2 instance is in a public subnet and has a public IPv4 address for manual administration and outbound Internet access. It is not directly exposed for application traffic:

- EC2 port `8080` accepts inbound traffic only from the ALB security group.
- SSH port `22` accepts inbound traffic only from the administrator's specific `/32` public IP.
- EC2 ports `80` and `443` are closed.
- No Nginx service runs on EC2; Docker publishes Spring Boot on `8080`.
- Direct access to the EC2 public IPv4 address on port `8080` is blocked.
- The ALB security group permits the required public HTTP listener traffic.
- The RDS security group permits MySQL only from the backend EC2 security group.

Moving the backend to private subnets is a future improvement. It would require private-instance administration, preferably Systems Manager Session Manager, plus controlled outbound access through a NAT Gateway or suitable VPC endpoints and a revised image deployment process.

## Project structure

```text
khoi/
|-- backend/
|-- database/
|-- frontend/
|-- .env.example
|-- .env.production.example
|-- description.md
|-- docker-compose.yml
|-- PROJECT_STATUS.md
`-- README.md
```

## Environment files

| File | Purpose | Git policy |
| --- | --- | --- |
| `.env.example` | Safe local-development template | Tracked |
| `.env.local` | Real local runtime values | Ignored; never commit |
| `.env.production.example` | Safe production template | Tracked |
| `ewallet-backend.env` | Real production backend values prepared locally | Must be ignored; never commit |
| `/home/ec2-user/ewallet-backend.env` | EC2 copy loaded with Docker `--env-file` | Outside Git |
| `frontend/.env.production` | Vite production build-time configuration | Ignored; public after compilation |

Keep local, frontend build-time, and production runtime configuration separate.
Never place database, JWT, SMTP, or AWS secrets in documentation or Vite
configuration. See [DEPLOYMENT.md](DEPLOYMENT.md) for the SES migration and Resend
rollback procedure.

Local `MAIL_DEVELOPMENT_LOG_ENABLED=true` logs account links and avoids real SMTP.
SMTP mode requires valid credentials for the selected provider. Production must
use `MAIL_DEVELOPMENT_LOG_ENABLED=false`.

Example production backend launch:

```bash
docker run -d \
  --name ewallet-backend \
  --env-file /home/ec2-user/ewallet-backend.env \
  -p 8080:8080 \
  --restart unless-stopped \
  <BACKEND_IMAGE>
```

The repository does not provide authoritative evidence for the exact currently deployed image tag, so deployment documentation intentionally uses a placeholder.

## Database

The main tables are:

- `users`
- `account_tokens`
- `user_profiles`
- `admin_profiles`
- `wallets`
- `services`
- `transactions`

`account_tokens` stores SHA-256 token hashes for `EMAIL_VERIFICATION` and `PASSWORD_RESET`. `expires_at` controls expiry and `used_at` records one-time use. Scheduled cleanup, including AWS Lambda cleanup, is not implemented.

Local reset/seed uses `database/schema.sql`. Fresh RDS setup uses `database/rds/001_schema.sql`, a securely completed uncommitted copy of `002_admin_template.sql`, then `003_services_seed.sql`.

## Build and validation

```powershell
cd backend
.\mvnw.cmd test
.\mvnw.cmd package -DskipTests
```

```powershell
cd frontend
npm install
npm run build
npm run lint
```

There is no frontend automated test script. Verified repository and production validation results are recorded in [PROJECT_STATUS.md](PROJECT_STATUS.md).

## Future improvements

1. Move backend EC2 instances to private subnets.
2. Use Systems Manager Session Manager instead of public SSH.
3. Add an Auto Scaling Group with multiple EC2 targets across Availability Zones.
4. Add CI/CD for automated build, testing, image publishing, and deployment.
5. Move Docker images from Docker Hub to Amazon ECR.
6. Add controlled outbound access using a NAT Gateway or appropriate VPC endpoints.
7. Add HTTPS directly between CloudFront and the ALB if required.
8. Add AWS WAF and stronger monitoring and alerting.
9. Add scheduled expired-token cleanup with Spring scheduling or AWS Lambda and EventBridge.
10. Integrate a real payment gateway for actual card or bank deposits.

These are planned improvements, not current features.
