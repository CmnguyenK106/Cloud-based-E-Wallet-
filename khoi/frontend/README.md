# Cloud E-Wallet Frontend

React, TypeScript, Vite, Axios, Zustand, and Zod frontend for Cloud E-Wallet.

## Implemented behavior

- ✅ Registration and login before email verification
- ✅ Account/profile access and verification-email resend for unverified users
- ✅ Persistent verification banner
- ✅ Deposit/top-up, wallet transfer, service payment, and transaction history
- ✅ Mutation controls disabled until verification
- ✅ Global Axios handling for HTTP 403 `EMAIL_VERIFICATION_REQUIRED` without logout

## Commands

```powershell
npm install
npm run dev
npm run build
npm run lint
```

`VITE_API_BASE_URL` selects a separate API origin. Development defaults to `http://localhost:8080`; an absent production value uses same-origin `/api`.

## Production deployment

The current production frontend is hosted in Amazon S3 and served through CloudFront at `cloud-ewallet.com`.

1. Run `npm run build`.
2. Upload the contents of `dist/` to the production S3 bucket.
3. Create a CloudFront invalidation for `/*`.

Deployment is manual today. GitHub Actions automation is planned, not implemented. The latest recorded production build and ESLint checks passed.
