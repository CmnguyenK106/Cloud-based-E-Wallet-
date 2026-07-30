---
title: "Install and configure S3 and CloudFront"
date: 2024-01-01
weight: 1
chapter: false
pre: " <b> 5.4.1. </b> "
---

## Objective

Create private Amazon S3 storage for static files and configure CloudFront to deliver the React frontend over HTTPS. Document the important creation steps and final settings rather than every console click.

## Step 1: Create the S3 bucket

Open **Amazon S3 → Create bucket**:

1. Select the project Region and a globally unique bucket name.
2. Keep **Block all public access** enabled.
3. Enable versioning when object recovery is required.
4. Create the bucket without uploading source, `.env.production`, or `node_modules`.

The bucket stores only the contents of `frontend/dist/`.

## Step 2: Create the CloudFront distribution

Open **CloudFront → Create distribution**:

1. Select the S3 regional endpoint as the origin.
2. Use Origin Access Control (OAC) so CloudFront can read the private bucket.
3. Apply the bucket policy generated or supplied by CloudFront.
4. Set **Viewer protocol policy** to **Redirect HTTP to HTTPS**.
5. Point default behavior `(*)` to the S3 origin.
6. Allow only `GET`, `HEAD`, and `OPTIONS` for the frontend.
7. Set **Default root object** to `index.html`.

Do not use the S3 website endpoint with an OAC origin.

## Step 3: Support React Router

If a direct React route returns `403` or `404`, configure custom error responses for both codes with `/index.html` and HTTP response `200`. Test the home page and direct routes afterwards.

## Step 4: Verify S3 access

The bucket policy allows object reads only from the intended distribution. Do not make the bucket public to solve `AccessDenied`. Replace `<AWS_ACCOUNT_ID>`, `<DISTRIBUTION_ID>`, and `<FRONTEND_BUCKET>` during configuration without publishing them unnecessarily.

## Step 5: Verify the distribution

Wait for **Deployed**, then open:

```text
https://<CLOUDFRONT_DISTRIBUTION_DOMAIN>
```

Section 5.5 adds the custom domain and `/api/*` behavior after the backend ALB is ready.
![S3 Block Public Access settings](/images/5-Workshop/5.4-Frontend-deployment/5.4.1-S3-CloudFront-configuration/s3-block-public-access.png)

<p style="text-align: center;"><em>Figure 5.9. Block Public Access enabled for the frontend S3 bucket.</em></p>

![CloudFront general configuration](/images/5-Workshop/5.4-Frontend-deployment/5.4.1-S3-CloudFront-configuration/cloudfront-general.png)

<p style="text-align: center;"><em>Figure 5.10. CloudFront domain, TLS certificate, and default root object configuration.</em></p>

![CloudFront origins](/images/5-Workshop/5.4-Frontend-deployment/5.4.1-S3-CloudFront-configuration/cloudfront-origins.png)

<p style="text-align: center;"><em>Figure 5.11. S3 frontend origin and Application Load Balancer origin in CloudFront.</em></p>

![CloudFront behaviors](/images/5-Workshop/5.4-Frontend-deployment/5.4.1-S3-CloudFront-configuration/cloudfront-behaviors.png)

<p style="text-align: center;"><em>Figure 5.12. The `/api/*` behavior routes to the ALB, while the default behavior routes to S3.</em></p>