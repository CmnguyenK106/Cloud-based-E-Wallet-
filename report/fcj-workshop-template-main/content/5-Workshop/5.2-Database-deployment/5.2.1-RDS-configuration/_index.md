---
title: "Install and configure Amazon RDS for MySQL"
date: 2024-01-01
weight: 1
chapter: false
pre: " <b> 5.2.1. </b> "
---

## Objective

Create the production Amazon RDS for MySQL database in private subnets and allow connections only from the backend security group.

## Step 1: Prepare the network

Select at least two private subnets in different Availability Zones. Create a database security group with inbound MySQL/Aurora TCP `3306` sourced from the backend EC2 security group. Never expose this port to `0.0.0.0/0`.
![RDS security group inbound rule](/images/5-Workshop/5.2-Database-deployment/5.2.1-RDS-configuration/rds-security-group-inbound.png)

<p style="text-align: center;"><em>Figure 5.3. The inbound rule allows only the backend security group to connect to RDS on port 3306.</em></p>

## Step 2: Create the DB subnet group

Open **Amazon RDS → Subnet groups → Create DB subnet group**, select the project VPC, and add the prepared private subnets.
![RDS DB subnet group](/images/5-Workshop/5.2-Database-deployment/5.2.1-RDS-configuration/rds-subnet-group.png)

<p style="text-align: center;"><em>Figure 5.4. The DB subnet group uses two private subnets in two Availability Zones.</em></p>

## Step 3: Create RDS for MySQL

Open **Databases → Create database**, then:

1. Select **Standard create** and **MySQL**.
2. Select a MySQL version compatible with the backend.
3. Enter the DB identifier according to the team naming convention.
4. Create a strong master username and password; never commit or publish them.
5. Select an instance class and storage suitable for the demonstration workload.
6. Select the VPC, DB subnet group, and database security group.
7. Set **Public access = No**.
8. Configure automated backups and retention; enable deletion protection while appropriate.
9. Create the database and wait for **Available**.

## Step 4: Record connection parameters

Obtain the endpoint and port from **Connectivity & security**. Documentation uses placeholders only:

```text
DB_URL=jdbc:mysql://<DB_ENDPOINT>:3306/<DB_NAME>
DB_USERNAME=<DB_USERNAME>
DB_PASSWORD=<DB_PASSWORD>
```

Real values are stored only in the backend environment file on EC2.

## Validation

- RDS is **Available** and not publicly accessible.
- Port `3306` accepts only the backend security group.
- Screenshots and source files disclose no endpoint credentials.
