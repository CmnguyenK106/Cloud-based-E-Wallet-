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

> **Image required:** Terminal showing tool versions.

<!-- IMAGE_PATH: /images/5-Workshop/5.1-Prerequisites/tool-versions.png -->

> **Image required:** Source tree with real environment files hidden.

<!-- IMAGE_PATH: /images/5-Workshop/5.1-Prerequisites/source-structure.png -->

## IAM permissions

If separate deployment permissions are required, an IAM user or role can be used instead of the root account. The following reference policy covers routine project work: updating the S3 frontend, creating CloudFront invalidations, inspecting EC2/ALB/RDS, reviewing CloudWatch, and sending or checking email through Amazon SES.

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "UpdateFrontendBucket",
      "Effect": "Allow",
      "Action": [
        "s3:ListBucket",
        "s3:GetBucketLocation"
      ],
      "Resource": "arn:aws:s3:::<FRONTEND_BUCKET_NAME>"
    },
    {
      "Sid": "ManageFrontendObjects",
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject",
        "s3:DeleteObject"
      ],
      "Resource": "arn:aws:s3:::<FRONTEND_BUCKET_NAME>/*"
    },
    {
      "Sid": "RefreshCloudFront",
      "Effect": "Allow",
      "Action": [
        "cloudfront:GetDistribution",
        "cloudfront:GetDistributionConfig",
        "cloudfront:CreateInvalidation"
      ],
      "Resource": "arn:aws:cloudfront::<AWS_ACCOUNT_ID>:distribution/<DISTRIBUTION_ID>"
    },
    {
      "Sid": "InspectRuntimeResources",
      "Effect": "Allow",
      "Action": [
        "ec2:DescribeInstances",
        "ec2:DescribeSecurityGroups",
        "elasticloadbalancing:DescribeLoadBalancers",
        "elasticloadbalancing:DescribeListeners",
        "elasticloadbalancing:DescribeTargetGroups",
        "elasticloadbalancing:DescribeTargetHealth",
        "rds:DescribeDBInstances",
        "rds:DescribeDBSubnetGroups",
        "cloudwatch:ListMetrics",
        "cloudwatch:GetMetricData",
        "cloudwatch:GetMetricStatistics"
      ],
      "Resource": "*"
    },
    {
      "Sid": "SendFromProjectIdentity",
      "Effect": "Allow",
      "Action": [
        "ses:SendEmail",
        "ses:SendRawEmail",
        "sesv2:GetEmailIdentity"
      ],
      "Resource": "arn:aws:ses:ap-southeast-1:<AWS_ACCOUNT_ID>:identity/cloud-ewallet.com"
    },
    {
      "Sid": "InspectSESAccount",
      "Effect": "Allow",
      "Action": [
        "ses:GetSendQuota",
        "ses:GetSendStatistics",
        "sesv2:GetAccount"
      ],
      "Resource": "*"
    }
  ]
}
```

Replace `<FRONTEND_BUCKET_NAME>`, `<AWS_ACCOUNT_ID>`, and `<DISTRIBUTION_ID>` with real identifiers before creating the policy. Describe operations, CloudWatch, and SES quota APIs do not support resource-level ARN restrictions and therefore require `Resource: "*"`. This routine policy excludes VPC, EC2, ALB, and RDS creation/deletion; full provisioning or cleanup should use a separate time-limited deployment role with only the additional permissions required for that step.


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

## Expected result

Every team member follows one process without sharing secrets through source or reports.
