---
title: "Deployment prerequisites"
date: 2024-01-01
weight: 1
chapter: false
pre: " <b> 5.1. </b> "
---

## Objective

Prepare tools, source, accounts, and configuration before creating or updating AWS resources.

## Tools

| Component | Check | Purpose |
| --- | --- | --- |
| Java 17 | `java -version` | Build Spring Boot |
| Node.js/npm | `node --version`, `npm --version` | Build React/Vite |
| Docker | `docker --version` | Build and run backend image |
| Git | `git --version` | Source management |
| AWS CLI | `aws --version` | Optional when using Console |
| AWS account | Sign in | Appropriate S3, CloudFront, EC2, ALB, RDS, and SES access |
| Cloudflare | Review zone | Manage `cloud-ewallet.com` and Amazon SES verification records |

## IAM permissions

The project uses two IAM users with separate responsibilities, avoiding the root account for routine deployment work:

| IAM user | Access method | Project responsibility |
| --- | --- | --- |
| `khoi_admin` | AWS Management Console sign-in | Manages S3, CloudFront, EC2, the Application Load Balancer, RDS, SES, and CloudWatch. In the internship environment, the user receives `AdministratorAccess` through `admin-group`. |
| `ses-smtp-user-cloud-ewallet` | Programmatic access through SES SMTP credentials; no Console sign-in | Allows the Spring Boot backend to authenticate with Amazon SES SMTP and send account-verification or password-reset email. This user is not used for builds or infrastructure administration. |

`khoi_admin` has broad permissions to support hands-on deployment across several AWS services. Credentials for `ses-smtp-user-cloud-ewallet` are stored only in the EC2 environment file and never included in source code, screenshots, or the report.

## Prepare the environment configuration

Before building and deploying, our team separates the frontend and backend configuration.

### Frontend

The frontend reads `frontend/.env.production` during the build. This file defines `VITE_API_BASE_URL`, which is the API address called by the React application after deployment to S3 and CloudFront. Vite variables are embedded in the compiled JavaScript, so this file must not contain passwords or secrets.

The frontend can be configured in either of the following ways:

```dotenv
# Option 1: Use the default CloudFront domain
# Replace <CLOUDFRONT_DISTRIBUTION_DOMAIN> with the distribution-specific domain
VITE_API_BASE_URL=https://<CLOUDFRONT_DISTRIBUTION_DOMAIN>

# Example format: https://dxxxxxxxxxxxxx.cloudfront.net
```

```dotenv
# Option 2: Use a custom domain that points to CloudFront
VITE_API_BASE_URL=https://cloud-ewallet.com
```

Each CloudFront distribution has its own domain, which must be copied from the AWS Console; it is a public identifier rather than a password or secret. Our project uses `https://cloud-ewallet.com`. Keep only the applicable `VITE_API_BASE_URL` value in the real file. After changing it, rebuild the frontend and upload it to S3; create a `/*` invalidation if CloudFront still caches the previous build.

### Backend

The backend on EC2 reads `/home/ec2-user/ewallet-backend.env` when the Docker container starts. The following block uses the project's real variable names while replacing secret values with placeholders:

```dotenv
SPRING_PROFILES_ACTIVE=prod

DB_URL=jdbc:mysql://<RDS_ENDPOINT>:3306/<DB_NAME>?useUnicode=true&characterEncoding=UTF-8&useSSL=true&requireSSL=true&serverTimezone=UTC
DB_USERNAME=<DB_USERNAME>
DB_PASSWORD=<DB_PASSWORD>

JWT_SECRET=<STRONG_RANDOM_SECRET_AT_LEAST_32_BYTES>
JWT_EXPIRATION=3600

FRONTEND_BASE_URL=https://cloud-ewallet.com
CORS_ALLOWED_ORIGINS=https://cloud-ewallet.com

MAIL_DEVELOPMENT_LOG_ENABLED=false
EMAIL_VERIFICATION_MINUTES=1440
PASSWORD_RESET_MINUTES=30
EMAIL_PROVIDER=ses

SES_SMTP_HOST=email-smtp.ap-southeast-1.amazonaws.com
SES_SMTP_PORT=587
SES_SMTP_USERNAME=<SES_SMTP_USERNAME>
SES_SMTP_PASSWORD=<SES_SMTP_PASSWORD>
SES_MAIL_FROM_ADDRESS=noreply@cloud-ewallet.com
```

The variables are grouped by purpose:

| Configuration group | Variables | Purpose |
| --- | --- | --- |
| Database | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Connect the backend to Amazon RDS for MySQL |
| Authentication | `JWT_SECRET` | Sign and verify access tokens |
| Domain and CORS | `FRONTEND_BASE_URL`, `CORS_ALLOWED_ORIGINS` | Generate user-facing links and allow only the intended frontend to call the API |
| Email service | `EMAIL_PROVIDER=ses`, the `SES_SMTP_*` variables, and `SES_MAIL_FROM_ADDRESS` | Connect to Amazon SES SMTP for verification and password-reset email |

The populated environment file is stored only on the deployment host and is never committed to Git. Source code and report examples use `.env.production.example` with placeholders such as `<DB_ENDPOINT>`, `<JWT_SECRET>`, and `<SES_SMTP_PASSWORD>`.

## Pre-deployment validation

- The source contains the `frontend/`, `backend/`, and `database/` directories.
- `frontend/.env.production` points to the production API and contains no secret.
- `.env.production.example` contains placeholders rather than real credentials.
- `/home/ec2-user/ewallet-backend.env` is created directly on EC2 with restricted file permissions.
- The AWS account has the required least-privilege permissions.
- The team has agreed on Singapore (`ap-southeast-1`), the naming convention, and the resources to deploy.

## Configure the VPC

The project uses `ewallet-vpc` with CIDR `10.0.0.0/16`, DNS resolution, and DNS hostnames enabled. Four subnets are distributed across `ap-southeast-1a` and `ap-southeast-1b`: two public subnets for Internet-facing resources and two private subnets for Amazon RDS. The public route table reaches the Internet Gateway, while the private subnets do not expose the database directly to the Internet.

![Cloud E-Wallet VPC resource map](/images/5-Workshop/5.1-Prerequisites/vpc-resource-map.png)

<p style="text-align: center;"><em>Figure 5.2. Cloud E-Wallet VPC resource map across two Availability Zones.</em></p>

## Result

Every team member follows one deployment process without sharing secrets through source code or reports.
