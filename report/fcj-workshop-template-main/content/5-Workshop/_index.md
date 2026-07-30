---
title: "Workshop"
date: 2024-01-01
weight: 5
chapter: false
pre: " <b> 5. </b> "
---

This section documents how our team deployed Cloud E-Wallet from source to the AWS production environment. The steps follow the dependencies among the database, backend, frontend, routing, and validation, and describe only components verified in the project.

![Cloud E-Wallet deployment architecture on AWS](/images/5-Workshop/5.1-Prerequisites/architecture.png)

<p style="text-align: center;"><em>Figure 5.1. Cloud E-Wallet deployment architecture on AWS.</em></p>

In this architecture, users access a Cloudflare-managed domain and requests reach CloudFront. The default behavior serves the React frontend from Amazon S3, while `/api/*` travels through the Application Load Balancer to the Spring Boot container on EC2. The backend connects to Amazon RDS for MySQL and uses Amazon SES SMTP for verification and password-reset email.

Cloud E-Wallet uses simulated balances, does not process real money, does not connect to a payment gateway, and does not send card data to the backend.

## Procedure

| Step | Main work | Expected outcome |
| --- | --- | --- |
| [5.1. Prepare the environment](5.1-Prerequisites/) | Verify tools and source; identify the two IAM users and configure the frontend, backend, RDS, JWT, CORS, and Amazon SES configuration | Deployment environment is ready in the correct Region with no exposed secrets |
| [5.2. Deploy the database](5.2-Database-deployment/) | Configure private RDS, then initialize the schema and baseline data | The production database is ready for the backend |
| [5.3. Deploy the backend](5.3-Backend-deployment/) | Prepare EC2/Docker/SES, build the image, and release the Spring Boot container | The backend connects to RDS, sends email, and serves traffic through the ALB |
| [5.4. Deploy the frontend](5.4-Frontend-deployment/) | Configure S3/CloudFront, build React, and release the static files | The frontend is delivered over HTTPS through CloudFront or the custom domain |
| [5.5. Configure routing and security](5.5-Traffic-security/) | Create the target group and ALB; configure CloudFront `/api/*`, Cloudflare DNS, and security groups | Traffic follows CloudFront → ALB → EC2 without unnecessary direct exposure of EC2 or RDS |
| [5.6. Validate the deployment](5.6-Validation/) | Confirm backend health, sign-in, wallet information, money transfer, and password-reset email through Amazon SES | Primary production workflows operate end to end |
| [5.7. Clean up resources](5.7-Cleanup/) | Back up required data, verify dependencies, and stop or delete unused resources | Post-demo charges are reduced |
