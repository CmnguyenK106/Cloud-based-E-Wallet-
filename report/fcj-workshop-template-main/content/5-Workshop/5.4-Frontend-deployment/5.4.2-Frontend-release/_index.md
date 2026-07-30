---
title: "Build and release the frontend through CloudFront"
date: 2024-01-01
weight: 2
chapter: false
pre: " <b> 5.4.2. </b> "
---

## Objective

Define the repeatable React release process: configure the API URL, validate source, build, synchronize `dist/` to S3, invalidate CloudFront, and run smoke tests.

## Step 1: Prepare the build variable

`frontend/.env.production` contains the public build-time API URL, not a secret.

For initial CloudFront-domain testing:

```text
VITE_API_BASE_URL=https://<CLOUDFRONT_DISTRIBUTION_DOMAIN>
```

After the custom domain and `/api/*` are ready:

```text
VITE_API_BASE_URL=https://cloud-ewallet.com
```

## Step 2: Install dependencies and validate

```powershell
cd frontend
npm install
npm run lint
npm run build
```

The build script runs TypeScript and Vite and writes output to `frontend/dist/`. 

## Step 3: Inspect the output

Confirm that `dist/index.html` and the assets exist. Never edit `dist/` directly because the next build replaces it.

## Step 4: Synchronize to S3

When AWS CLI is installed and configured for the deployment IAM identity:

```powershell
aws s3 sync .\dist\ s3://<FRONTEND_BUCKET>/ --delete
```

Verify the bucket before using `--delete`. With the AWS Console, upload all contents inside `dist/` and remove obsolete assets that are no longer referenced.
![Frontend build files uploaded to S3](/images/5-Workshop/5.4-Frontend-deployment/5.4.2-Frontend-release/s3-deployed-objects.png)

<p style="text-align: center;"><em>Figure 5.13. The frontend build output uploaded to the production S3 bucket.</em></p>

## Step 5: Create a CloudFront invalidation

In the Console, open the distribution and select **Invalidations → Create invalidation**, then enter `/*`; or run:

```powershell
aws cloudfront create-invalidation `
  --distribution-id <DISTRIBUTION_ID> `
  --paths "/*"
```

Wait for completion so users do not receive stale `index.html` references.
![Create a CloudFront invalidation](/images/5-Workshop/5.4-Frontend-deployment/5.4.2-Frontend-release/cloudfront-create-invalidation.png)

<p style="text-align: center;"><em>Figure 5.14. A `/*` invalidation created after the frontend update.</em></p>

## Step 6: Smoke test

1. Open the home page over HTTPS.
2. Hard-refresh and verify that assets return no `403/404`.
3. Open React routes directly.
4. Call an API through the production domain and verify CORS.
5. Check desktop/mobile layouts and the sign-in workflow.

If validation fails, compare the local build, S3 objects, CloudFront origin/behaviors, and invalidation before releasing again.
