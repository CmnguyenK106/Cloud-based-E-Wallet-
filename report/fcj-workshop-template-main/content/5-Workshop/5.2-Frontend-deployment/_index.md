---
title: "Deploy the frontend to S3 and CloudFront"
date: 2024-01-01
weight: 2
chapter: false
pre: " <b> 5.2. </b> "
---

## Objective

Build the React frontend into static files, upload them to S3, and configure S3 as the CloudFront frontend origin.

## Step 1: Configure and build

`frontend/.env.production` contains public build-time `VITE_API_BASE_URL`, not secrets.

```powershell
cd frontend
npm install
npm run build
npm run lint
```

Output is created under `frontend/dist/`. No automated frontend test script exists.

> **Image required:** Successful `npm run build` and `npm run lint`.

<!-- IMAGE_PATH: /images/5-Workshop/5.2-Frontend-deployment/frontend-build-lint.png -->

## Step 2: Upload to S3

Create/select the verified production bucket and upload the contents of `dist/`:

```powershell
aws s3 sync .\dist\ s3://<FRONTEND_BUCKET>/ --delete
```

Verify the bucket before `--delete`. Do not upload source, `.env.production`, or `node_modules`.

> **Image required:** S3 object list after upload.

<!-- IMAGE_PATH: /images/5-Workshop/5.2-Frontend-deployment/s3-objects.png -->

## Step 3: Configure the S3 origin/default behavior

Add the S3 origin and configure default behavior `(*)`. The repository does not confirm current OAC/OAI, bucket policy, or SPA fallback; capture the live configuration before documenting them.

> **Image required:** CloudFront S3 origin and default behavior `(*)`.

<!-- IMAGE_PATH: /images/5-Workshop/5.2-Frontend-deployment/cloudfront-s3-origin.png -->
<!-- IMAGE_PATH: /images/5-Workshop/5.2-Frontend-deployment/cloudfront-default-behavior.png -->

After an update:

```powershell
aws cloudfront create-invalidation --distribution-id <DISTRIBUTION_ID> --paths "/*"
```

## Validation

Open the CloudFront or production domain and test the home page, direct React routes, and responsive layouts. The frontend should load over HTTPS.

> **Image required:** Production frontend on desktop and mobile.

<!-- IMAGE_PATH: /images/5-Workshop/5.2-Frontend-deployment/frontend-production-desktop.png -->
<!-- IMAGE_PATH: /images/5-Workshop/5.2-Frontend-deployment/frontend-production-mobile.png -->
