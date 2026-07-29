# Cloud E-Wallet Project Status

Last documentation audit: 2026-07-29

Cloud E-Wallet is a deployed simulated-funds learning application, not a real-money wallet.

## Final implemented state

| Area | Status | Current state |
| --- | --- | --- |
| Customer application | Implemented | Registration, authentication, email/account recovery, profile, wallet operations, recipient lookup, services, history |
| Administration | Implemented | Dashboard, user block/unblock, transaction review, service lifecycle |
| Frontend | Deployed | Responsive React/TypeScript/Vite static build on S3 through CloudFront |
| Backend | Deployed | Dockerized Spring Boot on one EC2 target behind an ALB |
| Data and email | Deployed | Amazon RDS MySQL and Amazon SES SMTP; Resend fallback |
| Backend high availability | Partial | ALB routing exists, but only one EC2 target is registered |
| Automation | Future | Deployment remains manual; no CI/CD or Auto Scaling Group |

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
  `-- /api/* behavior --> Application Load Balancer (HTTP :80)
                             |
                             v
                        Target group (:8080)
                        health: /actuator/health
                             |
                             v
                        One public-subnet EC2 instance
                        Dockerized Spring Boot
                             |
                             +--> Private-subnet Amazon RDS MySQL
                             `--> Amazon SES SMTP (STARTTLS)
```

CloudFront is the browser-facing HTTPS endpoint. It routes `/api/*` to the internet-facing ALB over HTTP. The old direct CloudFront-to-EC2 origin is removed. The ALB target is healthy and forwards to the containerized backend on port `8080`.

## Network security and limitations

- EC2 is currently in a public subnet with a public IPv4 address for manual administration and outbound access; it is not a private-subnet instance.
- Application traffic reaches EC2 only from the ALB security group on port `8080`.
- Direct public access to EC2 port `8080` is blocked.
- SSH is restricted to the administrator's `/32` public IP.
- EC2 ports `80` and `443` are closed, and Nginx is not installed or required.
- RDS accepts MySQL only from the backend EC2 security group.
- The ALB has one healthy EC2 target. Routing and health checks are implemented and direct exposure is reduced, but full fault tolerance and Multi-AZ application availability are not achieved.

A stronger production design would place multiple backend instances in private subnets across Availability Zones, managed by an Auto Scaling Group. That requires a revised deployment/administration model, Systems Manager Session Manager, and controlled outbound connectivity such as a NAT Gateway or suitable VPC endpoints.

## Application evidence

Source review confirms:

- BCrypt registration/login, signed expiring JWTs, user/admin authorization, and logout.
- Database-backed blocked-user enforcement at login and on protected requests; the frontend clears rejected sessions.
- Email verification, resend verification, forgot-password, and reset-password.
- Profile retrieval/update, wallet balance, recipient lookup, simulated deposit, transfer, active-service listing/payment, and transaction history.
- Admin dashboard, user management, transaction management, and service add/edit/activate/deactivate.
- UTF-8 backend responses, `utf8mb4` database configuration, and responsive frontend layouts.
- Deposit card fields remain client-side only; the API receives `amount` and optional `description`.

## Email and account tokens

The backend uses one Spring Mail/`JavaMailSender` service with provider-neutral
business logic. `EMAIL_PROVIDER=ses` selects Amazon SES SMTP in production;
`EMAIL_PROVIDER=resend` selects the retained rollback provider. Selection is
case-insensitive, only the active provider is validated, and both use
authentication and required STARTTLS. `MAIL_DEVELOPMENT_LOG_ENABLED` is fixed to
`false` in the production profile.

The `account_tokens` table stores hashed tokens for `EMAIL_VERIFICATION` and `PASSWORD_RESET`. Validity and one-time use are controlled by `expires_at` and `used_at`. No Spring scheduler, EventBridge job, or AWS Lambda token cleanup exists.

## Database

Final main tables:

1. `users`
2. `account_tokens`
3. `user_profiles`
4. `admin_profiles`
5. `wallets`
6. `services`
7. `transactions`

The schemas use `utf8mb4`/`utf8mb4_unicode_ci` for Vietnamese text.

## Validation evidence

Repository test output from the documented final audit supports:

- Backend: 72 tests passed, 0 failures, 0 errors, 0 skipped.
- Backend package build passed.
- Frontend TypeScript/Vite production build passed.
- Frontend lint passed.
- No frontend automated test script exists.

Documented production smoke coverage includes registration, email verification, login, profile retrieval/update, recipient lookup, simulated deposit, transfer, service payment, transaction history, admin workflows, and verification/reset email flows. Infrastructure checks cover a healthy ALB target, CloudFront `/api/*` routing, and blocked direct EC2 backend access.

An unauthenticated protected API request returning `401 Unauthorized` confirms that the request reached Spring Security. By itself, it does not prove that every business workflow succeeds.

## Environment separation

- `.env.example`: safe local-development template.
- `.env.local`: real local values; ignored by Git.
- `.env.production.example`: safe production template.
- Root `ewallet-backend.env`: real production backend environment; must be ignored by Git.
- `/home/ec2-user/ewallet-backend.env`: production copy used by Docker on EC2.
- `frontend/.env.production`: Vite build-time production configuration; ignored by Git.

No secret value belongs in documentation. SES and Resend credentials remain
external to Git. See [DEPLOYMENT.md](DEPLOYMENT.md) for migration, verification,
rollback, and future CI/CD secret-injection guidance.

The correctly and incorrectly spelled production environment filenames
(`ewallet-backend.env` and `ewallet-bakend.env`) are ignored and untracked. Their
contents were not inspected.

## Future improvements

1. Move backend EC2 instances to private subnets.
2. Use Systems Manager Session Manager instead of public SSH.
3. Add an Auto Scaling Group with multiple EC2 targets across Availability Zones.
4. Add CI/CD for automated build, testing, image publishing, and deployment.
5. Move Docker images from Docker Hub to Amazon ECR.
6. Add controlled outbound access using a NAT Gateway or appropriate VPC endpoints.
7. Add HTTPS directly between CloudFront and the ALB if required.
8. Add AWS WAF and stronger monitoring or alerting.
9. Add scheduled cleanup for expired account tokens using Spring scheduling or AWS Lambda with EventBridge.
10. Integrate a real payment gateway for actual card or bank deposits.

These items are not implemented in the current version.
