---
title: "Configure ALB, CloudFront, DNS, and network security"
date: 2024-01-01
weight: 5
chapter: false
pre: " <b> 5.5. </b> "
---

## Objective

Complete the request path from users to the frontend and backend while restricting network access along CloudFront → Application Load Balancer → EC2 → Amazon RDS.

## Configure the target group

Our team created the backend target group with these settings:

- Target type: EC2 instance.
- Protocol: HTTP.
- Port: `8080`.
- Health check path: `/actuator/health`.
- The backend EC2 instance is registered as the target.

The ALB forwards traffic to EC2 only after the health check succeeds and the target becomes **Healthy**.

![Healthy backend Target Group](/images/5-Workshop/5.5-Traffic-security/target-group-healthy.png)

<p style="text-align: center;"><em>Figure 5.15. The target group uses HTTP port 8080 and the backend EC2 target is Healthy.</em></p>

## Configure the Application Load Balancer

The internet-facing Application Load Balancer runs in the public subnets of `ewallet-vpc`. Its HTTP listener on port `80` forwards requests to backend target-group port `8080`.

The browser connects to CloudFront over HTTPS. CloudFront connects to the ALB origin, and the ALB forwards the request to the Spring Boot container on EC2.

![Application Load Balancer details and listener](/images/5-Workshop/5.5-Traffic-security/alb-details-listener.png)

<p style="text-align: center;"><em>Figure 5.16. Internet-facing ALB with an HTTP port 80 listener forwarding to the backend target group.</em></p>

## Restrict access with security groups

Security groups use source relationships instead of exposing application ports directly to the Internet:

| Resource | Required inbound rule | Purpose |
| --- | --- | --- |
| ALB | HTTP port `80` for the current public listener | Receive API requests from CloudFront |
| EC2 backend | TCP `8080` with the ALB security group as source | Allow only the ALB to call Spring Boot |
| EC2 backend | SSH `22` from the administrator IP `/32` | Support manual administration through MobaXterm |
| Amazon RDS | MySQL `3306` with the EC2 security group as source | Allow only the backend to connect to the database |

EC2 does not expose port `8080` to `0.0.0.0/0`. Ports `80` and `443` are also unnecessary on EC2 because the instance does not run Nginx. Its public IPv4 address is used only for SSH administration within the workshop, not as an application endpoint.

![ALB security group inbound rules](/images/5-Workshop/5.5-Traffic-security/alb-security-group-inbound.png)

<p style="text-align: center;"><em>Figure 5.17. The ALB security group allows HTTP port 80 and HTTPS port 443 from the Internet.</em></p>

The EC2 inbound rules are shown in [Section 5.3.1](../5.3-Backend-deployment/5.3.1-Backend-environment/), and the RDS inbound rule is shown in [Section 5.2.1](../5.2-Database-deployment/5.2.1-RDS-configuration/), so the screenshots are not duplicated here.

## Route the API through CloudFront

The distribution has two origins and two behaviors:

- Default `(*)` uses the S3 frontend origin with `Managed-CachingOptimized`.
- `/api/*` uses `ewallet-alb-origin` with `Managed-CachingDisabled`, preventing unintended API-response caching.

CloudFront origins and behaviors are documented in [Section 5.4.1](../5.4-Frontend-deployment/5.4.1-S3-CloudFront-configuration/). This section focuses on their role in the end-to-end routing flow.

## Configure the domain in Cloudflare

Cloudflare manages the DNS zone for `cloud-ewallet.com`. Records originate from several services:

| Record group | Source | Purpose |
| --- | --- | --- |
| CNAME `cloud-ewallet.com` | CloudFront distribution domain | Route the apex domain to CloudFront |
| CNAME `www` | `cloud-ewallet.com` | Support `www.cloud-ewallet.com` |
| Certificate-validation CNAME | AWS Certificate Manager | Validate the domain and support automatic TLS certificate renewal |
| DKIM CNAME records | Amazon SES domain identity | Validate DKIM for project email |
| MX and TXT for `send.cloud-ewallet.com` | Amazon SES Custom MAIL FROM | Configure MAIL FROM and SPF |
| TXT `_dmarc` | Team-defined policy | Publish the domain DMARC policy |

AWS and email verification records are set to **DNS only**, without Cloudflare proxying, so ACM and SES can read the DNS values correctly. SES domain identity, Easy DKIM, and SMTP credentials are covered in [Section 5.3.1](../5.3-Backend-deployment/5.3.1-Backend-environment/).

![Cloud E-Wallet DNS records in Cloudflare](/images/5-Workshop/5.5-Traffic-security/cloudflare-dns-records.png)

<p style="text-align: center;"><em>Figure 5.18. CloudFront, ACM, and Amazon SES records in Cloudflare DNS.</em></p>

## Validation

After configuration, our team confirmed that:

- `https://cloud-ewallet.com` loads the S3 frontend through CloudFront.
- CloudFront routes `/api/*` requests to the ALB and then EC2 port `8080`.
- The EC2 target is **Healthy** in the target group.
- The EC2 public address cannot be used to access port `8080` directly.
- EC2 connects to RDS on port `3306` according to the security-group source rule.
- The domain, certificate, and SES verification records operate through Cloudflare DNS.