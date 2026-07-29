---
title: "Triển khai Amazon RDS MySQL"
date: 2024-01-01
weight: 3
chapter: false
pre: " <b> 5.3. </b> "
---

## Mục tiêu

Tạo MySQL production trên Amazon RDS trong private subnet và chỉ cho backend EC2 kết nối.

## Bước 1: Chuẩn bị mạng và database

Chọn VPC, DB subnet group gồm private subnet và tắt public access. Tạo Security Group RDS chỉ nhận TCP `3306` từ Security Group EC2, không mở `0.0.0.0/0`.

> **Hình cần bổ sung:** RDS subnet/public access và inbound rule port 3306 từ EC2 SG.

<!-- IMAGE_PATH: /images/5-Workshop/5.3-Database-deployment/rds-network.png -->
<!-- IMAGE_PATH: /images/5-Workshop/5.3-Database-deployment/rds-security-group.png -->

## Bước 2: Tạo RDS

Tạo MySQL instance theo cấu hình thực tế của nhóm. Không ghi DB identifier, endpoint, username hoặc password thật trong báo cáo. Lưu endpoint bằng placeholder `<DB_ENDPOINT>` trong tài liệu.

> **Hình cần bổ sung:** RDS ở trạng thái Available, che endpoint và định danh nhạy cảm.

<!-- IMAGE_PATH: /images/5-Workshop/5.3-Database-deployment/rds-available.png -->

## Bước 3: Khởi tạo schema

Với database mới, chạy theo thứ tự:

1. `database/rds/001_schema.sql`.
2. Bản sao `002_admin_template.sql` đã điền an toàn và không commit.
3. `003_services_seed.sql`.

Các bảng chính: `users`, `account_tokens`, `user_profiles`, `admin_profiles`, `wallets`, `services`, `transactions`. Schema sử dụng `utf8mb4`.

> **Hình cần bổ sung:** Danh sách bảng sau khi khởi tạo, không hiển thị dữ liệu cá nhân.

<!-- IMAGE_PATH: /images/5-Workshop/5.3-Database-deployment/rds-tables.png -->

## Kiểm tra

Từ backend EC2 hoặc đường kết nối được phép, kiểm tra kết nối JDBC và truy vấn danh sách bảng. Kết nối từ Internet công cộng phải bị từ chối.
