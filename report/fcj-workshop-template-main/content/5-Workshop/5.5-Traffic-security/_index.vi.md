---
title: "Cấu hình ALB, CloudFront, DNS và bảo mật mạng"
date: 2024-01-01
weight: 5
chapter: false
pre: " <b> 5.5. </b> "
---

## Mục tiêu

Đưa backend sau ALB, định tuyến `/api/*` qua CloudFront và giới hạn mạng theo chuỗi ALB → EC2 → RDS.

## Bước 1: Tạo target group

Tạo target group port `8080`, đăng ký EC2 và đặt health check path `/actuator/health`.

> **Hình cần bổ sung:** Target group port 8080 và target Healthy.

<!-- IMAGE_PATH: /images/5-Workshop/5.5-Traffic-security/target-group-healthy.png -->

## Bước 2: Tạo Application Load Balancer

ALB là internet-facing, listener HTTP port `80`, forward đến target group. ALB Security Group nhận public HTTP cần thiết cho cấu hình hiện tại.

> **Hình cần bổ sung:** ALB listener HTTP 80 và ALB Security Group.

<!-- IMAGE_PATH: /images/5-Workshop/5.5-Traffic-security/alb-listener.png -->
<!-- IMAGE_PATH: /images/5-Workshop/5.5-Traffic-security/alb-security-group.png -->

## Bước 3: Giới hạn EC2 và RDS

- EC2 inbound `8080` chỉ từ ALB Security Group.
- SSH `22` chỉ từ IP quản trị `/32`.
- EC2 đóng `80` và `443`; direct public EC2:8080 bị chặn.
- RDS `3306` chỉ từ EC2 Security Group.

> **Hình cần bổ sung:** EC2 và RDS Security Group thể hiện đúng source rule.

<!-- IMAGE_PATH: /images/5-Workshop/5.5-Traffic-security/ec2-security-group.png -->
<!-- IMAGE_PATH: /images/5-Workshop/5.5-Traffic-security/rds-security-group.png -->

## Bước 4: Cấu hình CloudFront API behavior

Thêm ALB origin và behavior `/api/*` trỏ đến ALB qua HTTP. Default `(*)` vẫn trỏ S3. Xóa origin CloudFront-to-EC2 trực tiếp cũ.

> **Hình cần bổ sung:** CloudFront origins và hai behaviors `(*)`, `/api/*`.

<!-- IMAGE_PATH: /images/5-Workshop/5.5-Traffic-security/cloudfront-origins.png -->
<!-- IMAGE_PATH: /images/5-Workshop/5.5-Traffic-security/cloudfront-behaviors.png -->

## Bước 5: Cloudflare DNS

Trỏ `cloud-ewallet.com` đến CloudFront theo record thực tế và giữ các record xác minh Resend.

> **Hình cần bổ sung:** Cloudflare DNS records, che dữ liệu không cần thiết.

<!-- IMAGE_PATH: /images/5-Workshop/5.5-Traffic-security/cloudflare-dns.png -->

## Kiểm tra

Domain tải frontend qua HTTPS; `/api/*` đến ALB; target Healthy; direct EC2:8080 bị chặn. ALB chỉ có một target nên chưa đạt high availability đầy đủ.
