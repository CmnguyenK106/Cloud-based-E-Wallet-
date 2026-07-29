---
title: "Proposal"
date: 2024-01-01
weight: 2
chapter: false
pre: " <b> 2. </b> "
---


# Cloud E-Wallet – A simulated e-wallet application deployed on AWS


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
- Amazon SES SMTP in Singapore (`ap-southeast-1`) with STARTTLS for verification and password-reset email; Resend is retained as a rollback provider.
- Cloudflare DNS for the domain and email-verification records.

## 4. Solution architecture

The proposed flow, subsequently applied by the project, is:

```text
Users → Cloudflare DNS → Amazon CloudFront
                              ├─ Default (*) → S3 frontend
                              └─ /api/* → ALB → EC2/Docker/Spring Boot
                                                   ├─ RDS MySQL
                                                   └─ Amazon SES SMTP
```

> **Image required:** Team architecture diagram showing User, Cloudflare, CloudFront, S3, ALB, EC2, RDS, Internet Gateway, Amazon SES, and CloudWatch.

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
| Amazon SES SMTP | Sends verification and reset email over authenticated STARTTLS on port `587` |
| CloudWatch | AWS metrics; custom logs/alarms require live configuration evidence |

## 5. Technical implementation

### Implementation stages

Our team delivered the project through five stages:

1. **Research and design:** Analyze requirements, define the simulated-wallet scope, and design the database and frontend–backend–AWS architecture.
2. **Local development:** Build the React frontend, Spring Boot REST API, and MySQL database; implement authentication, authorization, wallet workflows, and administration.
3. **Containerization and cloud preparation:** Validate frontend/backend builds, containerize Spring Boot, create the VPC, subnets, security groups, and prepare RDS.
4. **Deployment and integration:** Deploy the frontend to S3/CloudFront, run the backend container on EC2 behind the ALB, connect RDS, and configure Cloudflare DNS and Amazon SES SMTP.
5. **Validation and completion:** Perform health checks, business smoke tests, email testing, network-security review, cost monitoring, and report completion.

### Technical requirements

- **Frontend:** React 19, TypeScript, and Vite; static build hosted on S3, delivered by CloudFront, with responsive layouts.
- **Backend:** Java 17, Spring Boot, Spring Security, JDBC, and Actuator; Dockerized on EC2 port `8080`, accepting application traffic only from the ALB.
- **Database:** Amazon RDS for MySQL in private subnets; its security group permits port `3306` only from backend EC2; Vietnamese data uses `utf8mb4`.
- **Email:** Amazon SES SMTP in `ap-southeast-1`, port `587`, authentication, and STARTTLS; domain identity/DKIM verified through Cloudflare.
- **Security:** BCrypt, expiring JWTs, `user`/`admin` roles, secrets outside Git, HTTPS from users to CloudFront, and restricted security-group ingress.
- **Operations:** The ALB checks `/actuator/health`; CloudWatch provides AWS metrics; frontend and backend deployments are currently manual.
## 6. Functional scope

### Customer

Registration, verification/resend, login/logout, forgot/reset password, profile and balance, simulated deposit, recipient lookup, transfer, service payment, and transaction history.

### Administrator

Dashboard, user listing and block/unblock, transaction review, and service creation/editing/activation/deactivation.

### Out of scope

Real money, KYC, real OTP/SMS, payment gateways, ECS/Fargate, Auto Scaling, and CI/CD are outside the initial proposal. The ALB has one EC2 target, so the system does not provide full high availability.

## 7. Expected benefits

- An end-to-end full-stack and AWS learning product.
- Clear separation of UI, API, and database.
- Authentication, authorization, and transactional balance processing.
- Responsive UI and UTF-8 Vietnamese content.
- A foundation for future ECS, CI/CD, Auto Scaling, WAF, and stronger monitoring research.

## 8. Implementation plan

| Phase | Work |
| --- | --- |
| Weeks 1–2 | Requirements, architecture, database design, and project setup |
| Weeks 3–5 | Authentication, wallet workflows, customer UI, and administration |
| Week 6 | Testing, defect fixes, and backend containerization |
| Weeks 7–8 | S3, CloudFront, EC2, RDS, Amazon SES, ALB, and production checks |
| Week 9 | Product, documentation, and report completion |
| Weeks 10–11 | ECS and CI/CD research as future work, not production implementation |

## 9. Risks and mitigations

| Risk | Impact | Mitigation |
| --- | --- | --- |
| Secret exposure | High | Separate environment files, placeholders, no committed real values |
| Incorrect balances | High | Transactions, validation, and wallet-row locking |
| Backend outage | High | ALB health check; document one-target limitation and roadmap |
| AWS cost | Medium | Billing/Cost Explorer review and resource cleanup |
| Email failure | Medium | Verify the SES domain, sandbox status, STARTTLS, SMTP credentials, bounces, and complaints |

## 10. Cost

The following figures are **estimates**, not an actual invoice. Our team assumes resources in Singapore (`ap-southeast-1`), On-Demand pricing, 730 operating hours per month, and excludes tax and Free Tier benefits. The maximum is only an upper bound within this report's assumptions; AWS does not cap spending if traffic or resources continue to grow.

### Initial cost

| Cost item | Cost |
| --- | ---: |
| Purchase of `cloud-ewallet.com` through Cloudflare | **USD 10.98** |
| AWS service setup fees | **USD 0.00** |
| **Total one-time initial cost** | **USD 10.98** |

The domain is recorded as an initial purchase at the amount actually paid by the team and is **not amortized into monthly operating costs** below. A future renewal fee is excluded because no actual renewal price is available yet.

### Usage assumptions

| Item | Minimum | Average | Assumed maximum |
| --- | --- | --- | --- |
| EC2 and EBS | One `t3.micro`, 730 hours, 8 GB gp3 | Same as minimum | Same as minimum |
| RDS MySQL | One Single-AZ `db.t3.micro`, 20 GB | Same as minimum | Same as minimum |
| ALB | 730 hours, average 0.1 LCU | 730 hours, average 0.3 LCU | 730 hours, average 1 LCU |
| S3 | 1 GB and few requests | 5 GB, about 30,000 GET and 3,000 PUT requests | 20 GB, about 100,000 GET and 10,000 PUT requests |
| CloudFront | 5 GB and about 50,000 requests | 30 GB and about 250,000 requests | 100 GB and about 1,000,000 requests |
| Amazon SES | 1,000 text emails/month | 3,000 text emails/month | 10,000 text emails/month |
| CloudWatch | Basic metrics only | 1 GB of log ingestion | 5 GB of log ingestion |

### Monthly operating cost

| Service | Minimum (USD) | Average (USD) | Assumed maximum (USD) |
| --- | ---: | ---: | ---: |
| EC2 `t3.micro` | 9.64 | 9.64 | 9.64 |
| 8 GB gp3 EBS | 0.80 | 0.80 | 0.80 |
| RDS MySQL `db.t3.micro` + 20 GB | 21.74 | 21.74 | 21.74 |
| Application Load Balancer + LCU | 18.98 | 20.15 | 24.24 |
| S3 storage and requests | 0.03 | 0.15 | 0.70 |
| CloudFront transfer and requests | 0.61 | 3.65 | 12.10 |
| Amazon SES | 0.16 | 0.48 | 1.60 |
| CloudWatch | 0.00 | 0.50 | 2.50 |
| **Estimated monthly operating total** | **51.96** | **57.11** | **73.32** |
| **First-month total including domain purchase** | **62.94** | **68.09** | **84.30** |

### Conditions for each cost level

- **Minimum – USD 51.96/month:** The demo runs continuously with one EC2 `t3.micro`, one Single-AZ RDS `db.t3.micro`, and one ALB; up to about 1 GB on S3, 5 GB through CloudFront, 1,000 SES emails, and basic CloudWatch metrics only. Free Tier, tax, snapshots, and resources outside the table are excluded.
- **Average – USD 57.11/month:** Compute and database sizing remains unchanged, with more regular usage: about 5 GB on S3, 30 GB through CloudFront, 3,000 SES emails, 1 GB of CloudWatch Logs, and an average 0.3 LCU. This represents periodic team testing and demonstrations.
- **Assumed maximum – USD 73.32/month:** The system still uses one small EC2 and RDS instance, but usage rises to 20 GB on S3, 100 GB through CloudFront, 10,000 SES emails, 5 GB of logs, and an average 1 LCU. A larger EC2/RDS class, additional targets, Multi-AZ, NAT Gateway, WAF, or usage beyond these limits can exceed this figure.

AWS prices vary by date, Region, account, and actual consumption. Before long-term operation, the team should enter the deployed configuration into AWS Pricing Calculator and compare it with Billing/Cost Explorer. References: [AWS EC2 Pricing](https://aws.amazon.com/ec2/pricing/on-demand/), [Amazon RDS for MySQL Pricing](https://aws.amazon.com/rds/mysql/pricing/), [Elastic Load Balancing Pricing](https://aws.amazon.com/elasticloadbalancing/pricing/), [Amazon CloudFront Pricing](https://aws.amazon.com/cloudfront/pricing/), [Amazon S3 Pricing](https://aws.amazon.com/s3/pricing/), and [Amazon SES Pricing](https://aws.amazon.com/ses/pricing/).
> **Image required:** Redacted AWS Pricing Calculator estimate or Billing view.

<!-- IMAGE_PATH: /images/2-Proposal/aws-cost-estimate.png -->
<!-- After adding the file, uncomment: ![AWS cost estimate](/images/2-Proposal/aws-cost-estimate.png) -->

## 11. Expected outcome

The application is available at `https://cloud-ewallet.com`; CloudFront/S3 serves the frontend; CloudFront/ALB routes APIs to Spring Boot; the backend connects to RDS and sends email through Amazon SES SMTP. Main workflows are validated and architecture limitations are documented accurately.


