---
title: "Chuẩn bị môi trường triển khai"
date: 2024-01-01
weight: 1
chapter: false
pre: " <b> 5.1. </b> "
---

## Mục tiêu

Chuẩn bị đầy đủ công cụ, source, tài khoản và cấu hình trước khi tạo hoặc cập nhật tài nguyên AWS.

## Công cụ

| Thành phần | Kiểm tra | Yêu cầu |
| --- | --- | --- |
| Java 17 | `java -version` | Build Spring Boot |
| Node.js/npm | `node --version`, `npm --version` | Build React/Vite |
| Docker | `docker --version` | Build và chạy backend image |
| Git | `git --version` | Quản lý source |
| AWS CLI | `aws --version` | Tùy chọn nếu dùng Console |
| AWS account | Đăng nhập Console | Quyền phù hợp cho S3, CloudFront, EC2, ALB, RDS và SES |
| Cloudflare | Kiểm tra zone | Quản lý `cloud-ewallet.com` và các record xác minh Amazon SES |

> **Hình cần bổ sung:** Terminal kiểm tra phiên bản công cụ.

<!-- IMAGE_PATH: /images/5-Workshop/5.1-Prerequisites/tool-versions.png -->

> **Hình cần bổ sung:** Cây thư mục source, không hiển thị file môi trường thật.

<!-- IMAGE_PATH: /images/5-Workshop/5.1-Prerequisites/source-structure.png -->

## Chuẩn bị biến môi trường

Backend dùng `/home/ec2-user/ewallet-backend.env`; frontend dùng `frontend/.env.production` tại thời điểm build. Chuẩn bị `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `FRONTEND_BASE_URL`, `CORS_ALLOWED_ORIGINS`, `VITE_API_BASE_URL` và các biến mail `EMAIL_PROVIDER=ses`, `SES_SMTP_HOST`, `SES_SMTP_PORT`, `SES_SMTP_USERNAME`, `SES_SMTP_PASSWORD`, `SES_MAIL_FROM_ADDRESS`.

Không đưa giá trị thật vào tài liệu hoặc Git. Dùng placeholder như `<DB_ENDPOINT>`, `<JWT_SECRET>` và `<SES_SMTP_PASSWORD>`.

## Kiểm tra

- Source có đủ `frontend/`, `backend/`, `database/`.
- `.env.production.example` chỉ chứa placeholder.
- Tài khoản có quyền cần thiết theo nguyên tắc tối thiểu.
- Region, naming convention và danh sách tài nguyên dự kiến được thống nhất trong nhóm.

## Kết quả mong đợi

Mọi thành viên hiểu cùng quy trình và không cần chia sẻ secret qua source hoặc báo cáo.
