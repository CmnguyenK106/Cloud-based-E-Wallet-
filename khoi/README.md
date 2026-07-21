# Cloud E-Wallet

Spring Boot and React demonstration e-wallet. It is not suitable for real money.

## Local development

Copy `.env.example` to `.env.local`, replace `JWT_SECRET`, then run:

```powershell
.\start-dev.ps1
```

The local Spring profile provides localhost database, frontend, CORS, and development-email defaults. The frontend uses `http://localhost:8080` during Vite development unless `VITE_API_BASE_URL` is set.

## AWS deployment preparation

No AWS resources are created by this repository. The intended first deployment is a static frontend on S3/CloudFront, this backend on EC2, and MySQL on RDS.

### Backend image

```powershell
docker build -t ewallet-backend:local backend
```

The image uses a Maven build stage and a Java 17 runtime stage, runs as a non-root user, and exposes port 8080.

### Production backend environment

Set `SPRING_PROFILES_ACTIVE=prod` and provide `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `FRONTEND_BASE_URL`, and `CORS_ALLOWED_ORIGINS`. Use the JDBC RDS endpoint with TLS options appropriate to the RDS certificate configuration. Set `MAIL_DEVELOPMENT_LOG_ENABLED=false`; the production profile also forces URL logging off.

Example EC2 container invocation:

```powershell
docker run --rm -p 8080:8080 --env-file .env.production ewallet-backend:local
```

Never commit `.env.production`. Store production secrets in an appropriate AWS secret/configuration service or protected EC2 environment mechanism.

Health monitoring URL: `GET /actuator/health`. Liveness and readiness probes are available at `/actuator/health/liveness` and `/actuator/health/readiness`. Health details are not publicly exposed.

### Frontend production build

```powershell
$env:VITE_API_BASE_URL='https://api.example.com'
cd frontend
npm run build
```

There is no committed production API hostname. When the variable is absent in a production build, requests use same-origin `/api`; set the variable for a separate API origin.

### CORS

`CORS_ALLOWED_ORIGINS` is a comma-separated list of explicit origins, for example `https://wallet.example.com`. Wildcards and empty lists are rejected, and credentialed CORS is disabled.

## RDS initialization

1. Create a private MySQL 8 RDS instance, database credentials, security groups, backups, and an EC2-to-RDS network path. Do not expose port 3306 publicly.
2. Connect using an administrative database account and create the application database and least-privilege user:

```sql
CREATE DATABASE ewallet_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'ewallet_app'@'%' IDENTIFIED BY '<STRONG_GENERATED_PASSWORD>';
GRANT SELECT, INSERT, UPDATE, DELETE ON ewallet_db.* TO 'ewallet_app'@'%';
```

3. Apply the production schema from a trusted machine that can reach RDS:

```powershell
mysql --host=<RDS_ENDPOINT> --port=3306 --user=<MIGRATION_ADMIN> --password --ssl-mode=VERIFY_IDENTITY --ssl-ca=<RDS_CA_BUNDLE> ewallet_db < database/rds/001_schema.sql
```

4. Generate a BCrypt password hash offline, copy `database/rds/002_admin_template.sql` outside the repository, replace all placeholders, and execute that copy once. Do not use demo credentials or `database/schema.sql` in production.
5. Apply the controlled production service catalog from `database/rds/003_services_seed.sql`.
6. Configure the runtime account with only application DML permissions. Schema changes should continue through reviewed, numbered, non-destructive scripts; take an RDS snapshot before applying changes.

For a fresh RDS database, run `001_schema.sql`, the placeholder-completed one-time `002_admin_template.sql`, and then `003_services_seed.sql`, in that order. Existing files under `database/migrations/` are not required after applying the complete fresh schema. `database/schema.sql` is local-development only, and `database/fix_services_utf8.sql` is a legacy repair script rather than part of fresh RDS initialization.

The RDS scripts contain no destructive reset statements, demo users, wallets, transactions, or local test-data dependency. The only catalog data is the controlled production service seed.
