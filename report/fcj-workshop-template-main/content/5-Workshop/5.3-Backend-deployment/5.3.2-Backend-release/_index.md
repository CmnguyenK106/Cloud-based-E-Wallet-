---
title: "Build and deploy the backend to EC2"
date: 2024-01-01
weight: 2
chapter: false
pre: " <b> 5.3.2. </b> "
---

## Objective

Validate the Spring Boot source, build a Docker image, and release a backend version to EC2 using the prepared runtime environment.

## Step 1: Validate the source

On the build host:

```powershell
cd backend
.\mvnw.cmd test
.\mvnw.cmd package -DskipTests
```

Continue only after tests and packaging succeed. The JAR is generated under `backend/target/`.

## Step 2: Build the image

```powershell
docker build -t <BACKEND_IMAGE> .
docker image ls
```

Use a version or short commit as `<VERSION>` instead of relying only on `latest`, so releases can be identified and rolled back.

When building on EC2, transfer the source securely and build there. When using a registry, push and pull the exact tag without exposing registry credentials.

## Step 3: Stop the previous version

```bash
docker ps
docker logs --tail 100 ewallet-backend
docker stop ewallet-backend
docker rm ewallet-backend
```

Record the running image tag before removing the old container.

## Step 4: Run the new version

```bash
docker run -d \
  --name ewallet-backend \
  --env-file /home/ec2-user/ewallet-backend.env \
  -p 8080:8080 \
  --restart unless-stopped \
  <BACKEND_IMAGE>
```

The root `docker-compose.yml` uses local MySQL and is not the RDS production deployment.

## Step 5: Post-deployment checks

```bash
docker ps
docker logs --tail 100 ewallet-backend
curl http://localhost:8080/actuator/health
```

Then verify ALB target health, call the API through the production domain, and test a verification or password-reset email workflow. Do not capture credentials or tokens.
