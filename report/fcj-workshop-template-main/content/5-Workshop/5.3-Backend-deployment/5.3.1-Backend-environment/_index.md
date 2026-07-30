---
title: "Install and configure the backend environment"
date: 2024-01-01
weight: 1
chapter: false
pre: " <b> 5.3.1. </b> "
---

## Objective

Prepare EC2, Docker, and runtime configuration so Spring Boot can connect securely to RDS and Amazon SES.

## Step 1: Prepare EC2

Create the instance in the project VPC with the backend security group and an administrative key pair. The current setup uses a public IPv4 address for manual MobaXterm SSH; port `22` is restricted to the administrator `/32`.

Application traffic reaches port `8080` from the ALB security group, not directly from the Internet. EC2 needs outbound HTTPS for packages/images and outbound SMTP `587` for SES.
![Configured backend EC2 instance](/images/5-Workshop/5.3-Backend-deployment/5.3.1-Backend-environment/ec2-instance-summary.png)

<p style="text-align: center;"><em>Figure 5.5. The backend EC2 instance is running in ewallet-vpc with an IAM role and IMDSv2.</em></p>

![EC2 security group inbound rules](/images/5-Workshop/5.3-Backend-deployment/5.3.1-Backend-environment/ec2-security-group-inbound.png)

<p style="text-align: center;"><em>Figure 5.6. The EC2 security group allows SSH from the administrator IP and backend port 8080 from the ALB security group.</em></p>

## Step 2: Install Docker

Connect through SSH, install Docker for the instance operating system, and verify:

```bash
sudo systemctl enable --now docker
docker --version
```

After adding `ec2-user` to the `docker` group, reconnect before running Docker without `sudo`.
![Docker installed on EC2](/images/5-Workshop/5.3-Backend-deployment/5.3.1-Backend-environment/docker-version.png)

<p style="text-align: center;"><em>Figure 5.7. Docker Client and Docker Engine installed on EC2.</em></p>

## Step 3: Prepare Amazon SES

1. Select SES in Singapore (`ap-southeast-1`).
2. Verify the `cloud-ewallet.com` domain identity and enable Easy DKIM.
3. Add the SES records to Cloudflare DNS without proxying email records.
4. Create application-specific SMTP credentials.
5. Request production access for delivery to recipients outside the SES sandbox.
6. Use `email-smtp.ap-southeast-1.amazonaws.com`, port `587`, and STARTTLS.

The `ses-smtp-user-cloud-ewallet` IAM user is for SMTP only. Never use root or administrator credentials in the application.
![Amazon SES account dashboard](/images/5-Workshop/5.3-Backend-deployment/5.3.1-Backend-environment/ses-account-dashboard.png)

<p style="text-align: center;"><em>Figure 5.8. Amazon SES in the Singapore Region is Healthy with an approved production sending quota.</em></p>

## Step 4: Create the environment file

Create `/home/ec2-user/ewallet-backend.env` and restrict access:

```bash
chmod 600 /home/ec2-user/ewallet-backend.env
```

Use placeholders in documentation:

```text
DB_URL=jdbc:mysql://<DB_ENDPOINT>:3306/<DB_NAME>
DB_USERNAME=<DB_USERNAME>
DB_PASSWORD=<DB_PASSWORD>
JWT_SECRET=<JWT_SECRET>
FRONTEND_BASE_URL=https://cloud-ewallet.com
CORS_ALLOWED_ORIGINS=https://cloud-ewallet.com
EMAIL_PROVIDER=ses
SES_SMTP_HOST=email-smtp.ap-southeast-1.amazonaws.com
SES_SMTP_PORT=587
SES_SMTP_USERNAME=<SES_SMTP_USERNAME>
SES_SMTP_PASSWORD=<SES_SMTP_PASSWORD>
SES_MAIL_FROM_ADDRESS=noreply@cloud-ewallet.com
```

Never commit or capture real values.

## Validation

Docker is running, EC2 can reach RDS on `3306` and SES on `587`, and only the file owner can read the runtime environment file.
