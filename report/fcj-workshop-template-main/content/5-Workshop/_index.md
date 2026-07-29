---
title: "Workshop"
date: 2024-01-01
weight: 5
chapter: false
pre: " <b> 5. </b> "
---

This section guides the process our team followed to deploy Cloud E-Wallet source to AWS production. Steps follow actual dependencies and only describe components supported by repository evidence.

```text
User → Cloudflare DNS → CloudFront
                           ├─ (*) → S3 frontend
                           └─ /api/* → ALB → EC2/Docker/Spring Boot
                                                ├─ RDS MySQL
                                                └─ Resend SMTP
```

Cloud E-Wallet uses simulated balances, does not process real money, and does not send card data to the backend.

## Procedure

| Step | Content | Outcome |
| --- | --- | --- |
| [5.1. Prerequisites](5.1-Prerequisites/) | Tools, accounts, source, environment variables | Ready environment with no exposed secrets |
| [5.2. Frontend](5.2-Frontend-deployment/) | Build React, upload S3, prepare CloudFront origin | Static frontend deployed |
| [5.3. Database](5.3-Database-deployment/) | Create RDS, security group, and schema | Production MySQL ready |
| [5.4. Backend](5.4-Backend-deployment/) | Build image, run EC2 container, configure Resend | Spring Boot on port 8080 |
| [5.5. Routing and security](5.5-Traffic-security/) | ALB, health, CloudFront behaviors, Cloudflare, SGs | Correct traffic path without direct EC2 exposure |
| [5.6. Validation](5.6-Validation/) | Builds/tests, health, production smoke tests | Evidence-backed operation |
| [5.7. Cleanup](5.7-Cleanup/) | Back up and remove resources by dependency | Reduced post-demo cost |

> **Image required:** Architecture diagram used throughout the workshop.

<!-- IMAGE_PATH: /images/5-Workshop/cloud-ewallet-deployment-architecture.png -->
<!-- After adding the file, uncomment: ![Cloud E-Wallet deployment architecture](/images/5-Workshop/cloud-ewallet-deployment-architecture.png) -->

> **Note:** Frontend/backend deployment is manual. The system does not use ECS/Fargate, Auto Scaling, or CI/CD, and the ALB has one EC2 target.

