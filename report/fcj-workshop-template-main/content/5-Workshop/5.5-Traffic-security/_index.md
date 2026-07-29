---
title: "Configure ALB, CloudFront, DNS, and network security"
date: 2024-01-01
weight: 5
chapter: false
pre: " <b> 5.5. </b> "
---

## Objective

Place the backend behind the ALB, route `/api/*` through CloudFront, and restrict networking along ALB → EC2 → RDS.

## Step 1: Create the target group

Create a port-`8080` target group, register EC2, and use `/actuator/health` for health checks.

> **Image required:** Port-8080 target group with a Healthy target.

<!-- IMAGE_PATH: /images/5-Workshop/5.5-Traffic-security/target-group-healthy.png -->

## Step 2: Create the Application Load Balancer

The ALB is internet-facing with an HTTP `80` listener forwarding to the target group. Its security group accepts required public HTTP traffic for the current design.

> **Image required:** HTTP-80 listener and ALB security group.

<!-- IMAGE_PATH: /images/5-Workshop/5.5-Traffic-security/alb-listener.png -->
<!-- IMAGE_PATH: /images/5-Workshop/5.5-Traffic-security/alb-security-group.png -->

## Step 3: Restrict EC2 and RDS

- EC2 `8080` only from the ALB security group.
- SSH `22` only from administrator `/32`.
- EC2 `80`/`443` closed; direct public EC2:8080 blocked.
- RDS `3306` only from the EC2 security group.

> **Image required:** EC2 and RDS security groups showing rule sources.

<!-- IMAGE_PATH: /images/5-Workshop/5.5-Traffic-security/ec2-security-group.png -->
<!-- IMAGE_PATH: /images/5-Workshop/5.5-Traffic-security/rds-security-group.png -->

## Step 4: Configure CloudFront API behavior

Add the ALB origin and `/api/*` behavior over HTTP. Default `(*)` remains on S3. Remove the previous direct CloudFront-to-EC2 origin.

> **Image required:** CloudFront origins and `(*)`/`/api/*` behaviors.

<!-- IMAGE_PATH: /images/5-Workshop/5.5-Traffic-security/cloudfront-origins.png -->
<!-- IMAGE_PATH: /images/5-Workshop/5.5-Traffic-security/cloudfront-behaviors.png -->

## Step 5: Cloudflare DNS

Point `cloud-ewallet.com` to CloudFront according to the live record and retain Resend verification records.

> **Image required:** Redacted Cloudflare DNS records.

<!-- IMAGE_PATH: /images/5-Workshop/5.5-Traffic-security/cloudflare-dns.png -->

## Validation

The domain serves the frontend over HTTPS, `/api/*` reaches the ALB, the target is Healthy, and direct EC2:8080 is blocked. One target does not provide full high availability.
