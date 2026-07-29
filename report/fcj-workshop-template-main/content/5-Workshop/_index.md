---
title: "Workshop"
date: 2024-01-01
weight: 5
chapter: false
pre: " <b> 5. </b> "
---

This section documents how our team deployed Cloud E-Wallet from source to the AWS production environment. The steps follow the dependencies among the frontend, database, backend, routing, and validation, and describe only components verified in the project.

![Cloud E-Wallet deployment architecture on AWS](/images/5-Workshop/5.1-Prerequisites/architecture.png)

*Figure 5.1. Cloud E-Wallet deployment architecture on AWS.*

In this architecture, users access a Cloudflare-managed domain and requests reach CloudFront. The default behavior serves the React frontend from Amazon S3, while `/api/*` travels through the Application Load Balancer to the Spring Boot container on EC2. The backend connects to Amazon RDS for MySQL and uses Amazon SES SMTP for verification and password-reset email.

Cloud E-Wallet uses simulated balances, does not process real money, does not connect to a payment gateway, and does not send card data to the backend.

## Procedure

| Step | Main work | Expected outcome |
| --- | --- | --- |
| [5.1. Prepare the environment](5.1-Prerequisites/) | Verify tools and source; review the reference IAM policy and frontend, backend, RDS, JWT, CORS, and Amazon SES configuration | Deployment environment is ready in the correct Region with no exposed secrets |
| [5.2. Deploy the frontend](5.2-Frontend-deployment/) | Build React/Vite, upload static files to S3, and configure the S3 origin and default CloudFront behavior | Frontend is delivered over HTTPS through CloudFront or the custom domain |
| [5.3. Deploy the database](5.3-Database-deployment/) | Create RDS for MySQL in private subnets, restrict its security group, and initialize schema/seed data | Production database is available only to the permitted backend |
| [5.4. Deploy the backend](5.4-Backend-deployment/) | Build/test Spring Boot, create the Docker image, run the EC2 container, and configure Amazon SES SMTP | Backend runs on port `8080`, connects to RDS, and sends workflow email |
| [5.5. Configure routing and security](5.5-Traffic-security/) | Create the target group and ALB; configure CloudFront `/api/*`, Cloudflare DNS, and security groups | Traffic follows CloudFront → ALB → EC2 without unnecessary direct exposure of EC2 or RDS |
| [5.6. Validate the deployment](5.6-Validation/) | Check builds, health, network controls, customer/admin workflows, and SES Sending Statistics | Technical evidence and smoke tests confirm system operation |
| [5.7. Clean up resources](5.7-Cleanup/) | Back up required data, verify dependencies, and stop or delete unused resources | Post-demo charges are reduced |

> **Current deployment scope:** Frontend and backend are deployed manually. The system uses one EC2 target behind the Application Load Balancer; ECS/Fargate, Auto Scaling, and CI/CD are identified as future improvements.