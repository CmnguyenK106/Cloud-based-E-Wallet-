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
| AWS account | Sign in | Appropriate S3, CloudFront, EC2, ALB, RDS access |
| Cloudflare | Review zone | Manage `cloud-ewallet.com` and Resend records |

> **Image required:** Terminal showing tool versions.

<!-- IMAGE_PATH: /images/5-Workshop/5.1-Prerequisites/tool-versions.png -->

> **Image required:** Source tree with real environment files hidden.

<!-- IMAGE_PATH: /images/5-Workshop/5.1-Prerequisites/source-structure.png -->

## Environment preparation

The backend uses `/home/ec2-user/ewallet-backend.env`; the frontend uses `frontend/.env.production` at build time. Prepare variable names for database, JWT, frontend/CORS, SMTP, and `VITE_API_BASE_URL`.

Do not place real values in documentation or Git. Use placeholders such as `<DB_ENDPOINT>`, `<JWT_SECRET>`, and `<SMTP_PASSWORD>`.

## Validation

- Source contains `frontend/`, `backend/`, and `database/`.
- `.env.production.example` contains placeholders only.
- Accounts have minimum required access.
- The team agrees on region, naming, and the planned resource inventory.

## Expected result

Every team member follows one process without sharing secrets through source or reports.
