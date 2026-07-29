---
title: "Proposal"
date: 2024-01-01
weight: 2
chapter: false
pre: " <b> 2. </b> "
---


**Cloud E-Wallet – A simulated e-wallet application deployed on AWS**


## 1. Executive summary

Our team proposes **Cloud E-Wallet**, a web application that simulates essential e-wallet operations and is deployed on AWS. Customers can practice account registration, email verification, profile management, balance viewing, simulated deposits, transfers, service payments, and transaction-history review. Administrators can monitor the application and manage users, transactions, and services.

The project is for learning and technical demonstration. It does not process real money, connect to a real bank or payment gateway, or store card information.

## 2. Problem statement

Even a simulated e-wallet must address secure authentication, customer/administrator authorization, consistent balance updates, transaction history, responsive UI, and cloud deployment.

A local-only application cannot fully demonstrate production routing, domain/HTTPS configuration, separation of frontend/backend/database, network controls, health checks, and email delivery. The project therefore needs an AWS architecture that supports end-to-end deployment while remaining appropriate for an internship scope.

## 3. Proposed solution

- React 19, TypeScript, and Vite frontend.
- Java 17, Spring Boot, Spring Security, and JDBC REST API.
- MySQL for users, tokens, wallets, services, and transactions.
- BCrypt, signed expiring JWTs, and `user`/`admin` roles.
- Database transactions and wallet-row locking for balance safety.
- Amazon S3 and CloudFront for frontend delivery.
- Application Load Balancer and EC2 for the Dockerized backend.
- Amazon RDS MySQL in private subnets.
- Resend SMTP with STARTTLS for verification and password-reset email.
- Cloudflare DNS for the domain and email-verification records.

## 4. Solution architecture

The proposed flow, subsequently applied by the project, is:

```text
Users → Cloudflare DNS → Amazon CloudFront
                              ├─ Default (*) → S3 frontend
                              └─ /api/* → ALB → EC2/Docker/Spring Boot
                                                   ├─ RDS MySQL
                                                   └─ Resend SMTP
```

> **Image required:** Team architecture diagram showing User, Cloudflare, CloudFront, S3, ALB, EC2, RDS, Internet Gateway, Resend, and CloudWatch.

<!-- IMAGE_PATH: /images/2-Proposal/cloud-ewallet-architecture.png -->
<!-- After adding the file, uncomment: ![Cloud E-Wallet architecture](/images/2-Proposal/cloud-ewallet-architecture.png) -->

| Component | Responsibility |
| --- | --- |
| Cloudflare DNS | Manages `cloud-ewallet.com` and sender-domain records |
| CloudFront | Browser HTTPS and routing for frontend and `/api/*` |
| S3 | Stores the React static build |
| ALB | Forwards APIs and checks backend health |
| EC2 | Runs Spring Boot in Docker |
| RDS MySQL | Stores data in private subnets |
| Resend SMTP | Sends verification and reset email |
| CloudWatch | AWS metrics; custom logs/alarms require live configuration evidence |

## 5. Functional scope

### Customer

Registration, verification/resend, login/logout, forgot/reset password, profile and balance, simulated deposit, recipient lookup, transfer, service payment, and transaction history.

### Administrator

Dashboard, user listing and block/unblock, transaction review, and service creation/editing/activation/deactivation.

### Out of scope

Real money, KYC, real OTP/SMS, payment gateways, ECS/Fargate, Auto Scaling, and CI/CD are outside the initial proposal. The ALB has one EC2 target, so the system does not provide full high availability.

## 6. Expected benefits

- An end-to-end full-stack and AWS learning product.
- Clear separation of UI, API, and database.
- Authentication, authorization, and transactional balance processing.
- Responsive UI and UTF-8 Vietnamese content.
- A foundation for future ECS, CI/CD, Auto Scaling, WAF, and stronger monitoring research.

## 7. Implementation plan

| Phase | Work |
| --- | --- |
| Weeks 1–2 | Requirements, architecture, database design, and project setup |
| Weeks 3–5 | Authentication, wallet workflows, customer UI, and administration |
| Week 6 | Testing, defect fixes, and backend containerization |
| Weeks 7–8 | S3, CloudFront, EC2, RDS, Resend, ALB, and production checks |
| Week 9 | Product, documentation, and report completion |
| Weeks 10–11 | ECS and CI/CD research as future work, not production implementation |

## 8. Risks and mitigations

| Risk | Impact | Mitigation |
| --- | --- | --- |
| Secret exposure | High | Separate environment files, placeholders, no committed real values |
| Incorrect balances | High | Transactions, validation, and wallet-row locking |
| Backend outage | High | ALB health check; document one-target limitation and roadmap |
| AWS cost | Medium | Billing/Cost Explorer review and resource cleanup |
| Email failure | Medium | Verify STARTTLS, SMTP variables, and domain status |

## 9. Cost

The repository does not contain a verified AWS Pricing Calculator estimate for this architecture. Our team does not invent a figure; cost should be added from the actual S3, CloudFront, ALB, EC2, RDS, and data-transfer configuration.

> **Image required:** Redacted AWS Pricing Calculator estimate or Billing view.

<!-- IMAGE_PATH: /images/2-Proposal/aws-cost-estimate.png -->
<!-- After adding the file, uncomment: ![AWS cost estimate](/images/2-Proposal/aws-cost-estimate.png) -->

## 10. Expected outcome

The application is available at `https://cloud-ewallet.com`; CloudFront/S3 serves the frontend; CloudFront/ALB routes APIs to Spring Boot; the backend connects to RDS and sends Resend email. Main workflows are validated and architecture limitations are documented accurately.


