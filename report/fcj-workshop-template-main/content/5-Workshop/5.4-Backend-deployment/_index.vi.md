---
title: "Triển khai backend trên Amazon EC2"
date: 2024-01-01
weight: 4
chapter: false
pre: " <b> 5.4. </b> "
---

## Mục tiêu

Build Docker image cho Spring Boot, chạy container trên EC2 và kết nối RDS/Amazon SES bằng cấu hình runtime an toàn.

## Bước 1: Build và kiểm tra backend

```powershell
cd backend
.\mvnw.cmd test
.\mvnw.cmd package -DskipTests
docker build -t <BACKEND_IMAGE> .
```

Image được push thủ công theo registry thực tế. Repository không xác nhận exact registry/tag đang deploy nên không điền giá trị giả.

> **Hình cần bổ sung:** 68 backend tests, package build và Docker build thành công.

<!-- IMAGE_PATH: /images/5-Workshop/5.4-Backend-deployment/backend-test-build.png -->
<!-- IMAGE_PATH: /images/5-Workshop/5.4-Backend-deployment/docker-build.png -->

## Bước 2: Chuẩn bị EC2

EC2 hiện nằm trong public subnet, có public IPv4 để SSH thủ công và outbound Internet. Cài Docker; không cài Nginx. SSH `22` chỉ nhận IP quản trị `/32`.

> **Hình cần bổ sung:** EC2 instance, subnet và Security Group; che public IP nếu cần.

<!-- IMAGE_PATH: /images/5-Workshop/5.4-Backend-deployment/ec2-instance.png -->

## Bước 3: Tạo runtime environment file

Trên EC2, tạo `/home/ec2-user/ewallet-backend.env` với giá trị production. Không chụp nội dung file. File gồm cấu hình profile, RDS, JWT, frontend/CORS và Amazon SES SMTP. Resend chỉ được giữ bên ngoài Git để rollback khi cần.

## Bước 4: Chạy container

```bash
docker run -d \
  --name ewallet-backend \
  --env-file /home/ec2-user/ewallet-backend.env \
  -p 8080:8080 \
  --restart unless-stopped \
  <BACKEND_IMAGE>
```

`docker-compose.yml` chỉ chạy MySQL local, không dùng ở production.

> **Hình cần bổ sung:** `docker ps` và log startup đã che thông tin nhạy cảm.

<!-- IMAGE_PATH: /images/5-Workshop/5.4-Backend-deployment/docker-ps.png -->
<!-- IMAGE_PATH: /images/5-Workshop/5.4-Backend-deployment/backend-startup-log.png -->

## Bước 5: Cấu hình Amazon SES

1. Chọn Amazon SES tại Region Singapore (`ap-southeast-1`) để đồng bộ với SMTP endpoint trong code.
2. Tạo domain identity cho `cloud-ewallet.com`, bật Easy DKIM và thêm các CNAME verification/DKIM do SES cung cấp vào Cloudflare DNS. Không proxy các record email.
3. Xác nhận identity chuyển sang trạng thái **Verified**. Nếu SES còn ở sandbox, xác minh cả địa chỉ nhận hoặc gửi yêu cầu production access trước khi kiểm thử với người dùng bên ngoài.
4. Tạo SES SMTP credentials dành riêng cho ứng dụng; đây không phải AWS access key thông thường.
5. Trong `/home/ec2-user/ewallet-backend.env`, đặt `EMAIL_PROVIDER=ses`, endpoint `email-smtp.ap-southeast-1.amazonaws.com`, port `587`, username/password SMTP và địa chỉ gửi `noreply@cloud-ewallet.com`. Không đưa credential thật vào Git hoặc ảnh báo cáo.
6. Khởi tạo lại container để nạp cấu hình. Spring Mail dùng SMTP authentication và STARTTLS; EC2 chỉ tạo kết nối outbound nên không cần mở inbound SMTP port.

Code vẫn hỗ trợ `EMAIL_PROVIDER=resend` như phương án rollback, nhưng Amazon SES là provider production hiện tại.

> **Hình cần bổ sung:** SES domain identity Verified, DKIM records trên Cloudflare đã che dữ liệu không cần thiết, SMTP settings không lộ credential và email verification/reset đã nhận.

<!-- IMAGE_PATH: /images/5-Workshop/5.4-Backend-deployment/ses-identity-verified.png -->
<!-- IMAGE_PATH: /images/5-Workshop/5.4-Backend-deployment/ses-cloudflare-dkim.png -->
<!-- IMAGE_PATH: /images/5-Workshop/5.4-Backend-deployment/ses-email.png -->

## Kiểm tra

Chạy `docker ps`, xem log và gọi `/actuator/health` qua đường truy cập được phép. Container phải chạy port `8080`, kết nối RDS và gửi được email trong workflow hợp lệ.
