---
title: "Deploy Amazon RDS MySQL"
date: 2024-01-01
weight: 3
chapter: false
pre: " <b> 5.3. </b> "
---

## Objective

Create production MySQL on Amazon RDS in private subnets and allow only backend EC2 access.

## Step 1: Prepare network and database access

Choose the VPC, a DB subnet group containing private subnets, and disable public access. The RDS security group accepts TCP `3306` only from the EC2 security group, never `0.0.0.0/0`.

> **Image required:** RDS subnet/public access and inbound 3306 from EC2 SG.

<!-- IMAGE_PATH: /images/5-Workshop/5.3-Database-deployment/rds-network.png -->
<!-- IMAGE_PATH: /images/5-Workshop/5.3-Database-deployment/rds-security-group.png -->

## Step 2: Create RDS

Create MySQL using the team's verified configuration. Do not document real identifiers, endpoint, username, or password. Use `<DB_ENDPOINT>` in documentation.

> **Image required:** RDS in Available state with sensitive identifiers hidden.

<!-- IMAGE_PATH: /images/5-Workshop/5.3-Database-deployment/rds-available.png -->

## Step 3: Initialize schema

For a fresh database, run:

1. `database/rds/001_schema.sql`.
2. A securely completed, uncommitted `002_admin_template.sql` copy.
3. `003_services_seed.sql`.

Main tables are `users`, `account_tokens`, `user_profiles`, `admin_profiles`, `wallets`, `services`, and `transactions`, using `utf8mb4`.

> **Image required:** Initialized table list without personal data.

<!-- IMAGE_PATH: /images/5-Workshop/5.3-Database-deployment/rds-tables.png -->

## Validation

From backend EC2 or another approved path, verify JDBC connectivity and list tables. Public Internet connections should be rejected.
