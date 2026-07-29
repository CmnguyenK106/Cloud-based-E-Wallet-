---
title: "Deploy the backend on Amazon EC2"
date: 2024-01-01
weight: 4
chapter: false
pre: " <b> 5.4. </b> "
---

## Objective

Build the Spring Boot Docker image, run it on EC2, and connect RDS/Resend through safe runtime configuration.

## Step 1: Build and validate the backend

```powershell
cd backend
.\mvnw.cmd test
.\mvnw.cmd package -DskipTests
docker build -t <BACKEND_IMAGE> .
```

The image is pushed manually to the actual registry. The repository does not authoritatively identify the deployed registry/tag, so no value is invented.

> **Image required:** Successful 68 tests, package build, and Docker build.

<!-- IMAGE_PATH: /images/5-Workshop/5.4-Backend-deployment/backend-test-build.png -->
<!-- IMAGE_PATH: /images/5-Workshop/5.4-Backend-deployment/docker-build.png -->

## Step 2: Prepare EC2

EC2 currently resides in a public subnet with public IPv4 for manual SSH and outbound access. Install Docker; no Nginx is used. SSH `22` accepts only the administrator `/32`.

> **Image required:** EC2 instance, subnet, and security group, with public IP hidden when appropriate.

<!-- IMAGE_PATH: /images/5-Workshop/5.4-Backend-deployment/ec2-instance.png -->

## Step 3: Create the runtime environment file

On EC2, create `/home/ec2-user/ewallet-backend.env` with production values. Do not capture its contents. It contains profile, RDS, JWT, frontend/CORS, and Resend SMTP configuration.

## Step 4: Run the container

```bash
docker run -d \
  --name ewallet-backend \
  --env-file /home/ec2-user/ewallet-backend.env \
  -p 8080:8080 \
  --restart unless-stopped \
  <BACKEND_IMAGE>
```

Root `docker-compose.yml` is local MySQL only, not production.

> **Image required:** `docker ps` and redacted startup log.

<!-- IMAGE_PATH: /images/5-Workshop/5.4-Backend-deployment/docker-ps.png -->
<!-- IMAGE_PATH: /images/5-Workshop/5.4-Backend-deployment/backend-startup-log.png -->

## Step 5: Configure Resend

Production uses authenticated SMTP `587` with STARTTLS. Cloudflare stores sender-domain verification records. Amazon SES is not used, and no inbound SMTP port is opened on EC2.

> **Image required:** Verified Resend domain and received verification/reset email.

<!-- IMAGE_PATH: /images/5-Workshop/5.4-Backend-deployment/resend-verified.png -->
<!-- IMAGE_PATH: /images/5-Workshop/5.4-Backend-deployment/resend-email.png -->

## Validation

Run `docker ps`, review logs, and call `/actuator/health` through an allowed path. The container should run on `8080`, connect to RDS, and send valid workflow email.
