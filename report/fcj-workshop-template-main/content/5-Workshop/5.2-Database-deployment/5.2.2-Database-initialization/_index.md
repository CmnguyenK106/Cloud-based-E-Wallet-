---
title: "Initialize the database"
date: 2024-01-01
weight: 2
chapter: false
pre: " <b> 5.2.2. </b> "
---

## Objective

Initialize the Cloud E-Wallet schema and seed data after RDS is ready, without exposing the database to the Internet.

## Step 1: Select the execution host

Run the MySQL client from the backend EC2 instance or another approved host with private network access. Do not make RDS public merely to import data.

## Step 2: Prepare the scripts

Run the files under `database/rds/` in this order:

1. `001_schema.sql` — tables, keys, and relationships.
2. A private completed copy of `002_admin_template.sql` — initial administrator; never commit the completed copy.
3. `003_services_seed.sql` — initial service records.

The `utf8mb4` schema includes `users`, `account_tokens`, `user_profiles`, `admin_profiles`, `wallets`, `services`, and `transactions`.

## Step 3: Connect and initialize

```bash
mysql -h <DB_ENDPOINT> -P 3306 -u <DB_USERNAME> -p <DB_NAME>
```

After confirming the target database:

```sql
SOURCE database/rds/001_schema.sql;
SOURCE <SAFE_ADMIN_SEED_FILE>;
SOURCE database/rds/003_services_seed.sql;
```

Do not place the password directly in the command because shell history may retain it.

## Step 4: Verify

```sql
SHOW TABLES;
SELECT COUNT(*) FROM services;
```

Verify the schema and seed records without capturing personal data, password hashes, or tokens.

## Result

The required schema and seed data exist. Backend EC2 can connect with runtime credentials, while direct Internet connections are rejected.
