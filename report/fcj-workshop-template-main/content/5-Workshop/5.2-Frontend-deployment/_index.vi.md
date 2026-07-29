---
title: "Triển khai frontend lên S3 và CloudFront"
date: 2024-01-01
weight: 2
chapter: false
pre: " <b> 5.2. </b> "
---

## Mục tiêu

Build React frontend thành static files, upload lên S3 và cấu hình S3 làm origin frontend của CloudFront.

## Bước 1: Cấu hình và build

`frontend/.env.production` chứa `VITE_API_BASE_URL` public tại build-time, không chứa secret.

```powershell
cd frontend
npm install
npm run build
npm run lint
```

Kết quả build nằm trong `frontend/dist/`. Repository không có automated frontend test script.

> **Hình cần bổ sung:** `npm run build` và `npm run lint` thành công.

<!-- IMAGE_PATH: /images/5-Workshop/5.2-Frontend-deployment/frontend-build-lint.png -->

## Bước 2: Upload lên S3

Tạo/chọn đúng bucket production và upload nội dung bên trong `dist/`. Ví dụ:

```powershell
aws s3 sync .\dist\ s3://<FRONTEND_BUCKET>/ --delete
```

Kiểm tra kỹ bucket trước `--delete`. Không upload source, `.env.production` hoặc `node_modules`.

> **Hình cần bổ sung:** Danh sách object của bucket sau khi upload.

<!-- IMAGE_PATH: /images/5-Workshop/5.2-Frontend-deployment/s3-objects.png -->

## Bước 3: Tạo S3 origin/default behavior

Thêm S3 origin vào CloudFront và đặt default behavior `(*)` phục vụ frontend. Repository chưa xác nhận OAC/OAI, bucket policy và SPA fallback; cần chụp cấu hình thật trước khi bổ sung.

> **Hình cần bổ sung:** CloudFront S3 origin và default behavior `(*)`.

<!-- IMAGE_PATH: /images/5-Workshop/5.2-Frontend-deployment/cloudfront-s3-origin.png -->
<!-- IMAGE_PATH: /images/5-Workshop/5.2-Frontend-deployment/cloudfront-default-behavior.png -->

Sau mỗi bản frontend mới, có thể tạo invalidation:

```powershell
aws cloudfront create-invalidation --distribution-id <DISTRIBUTION_ID> --paths "/*"
```

## Kiểm tra

Mở CloudFront domain hoặc production domain, kiểm tra trang chủ, route React trực tiếp và responsive. Frontend phải tải qua HTTPS.

> **Hình cần bổ sung:** Giao diện frontend trên desktop và mobile.

<!-- IMAGE_PATH: /images/5-Workshop/5.2-Frontend-deployment/frontend-production-desktop.png -->
<!-- IMAGE_PATH: /images/5-Workshop/5.2-Frontend-deployment/frontend-production-mobile.png -->
