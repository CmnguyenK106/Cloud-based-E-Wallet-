---
title: "Workshop"
date: 2024-01-01
weight: 5
chapter: false
pre: " <b> 5. </b> "
---

Phần này hướng dẫn quy trình nhóm chúng em cùng triển khai project Cloud E-Wallet từ source lên môi trường AWS production. Nội dung đi theo thứ tự phụ thuộc thực tế và chỉ mô tả những thành phần đã được xác minh trong repository.

```text
User → Cloudflare DNS → CloudFront
                           ├─ (*) → S3 frontend
                           └─ /api/* → ALB → EC2/Docker/Spring Boot
                                                ├─ RDS MySQL
                                                └─ Amazon SES SMTP
```

Cloud E-Wallet dùng số dư mô phỏng, không xử lý tiền thật và không gửi dữ liệu thẻ đến backend.

## Trình tự thực hiện

| Bước | Nội dung | Kết quả |
| --- | --- | --- |
| [5.1. Chuẩn bị](5.1-Prerequisites/) | Công cụ, tài khoản, source và biến môi trường | Môi trường sẵn sàng, không lộ secret |
| [5.2. Frontend](5.2-Frontend-deployment/) | Build React, upload S3, chuẩn bị CloudFront origin | Static frontend đã được triển khai |
| [5.3. Database](5.3-Database-deployment/) | Tạo RDS, Security Group và schema | MySQL production sẵn sàng |
| [5.4. Backend](5.4-Backend-deployment/) | Build image, chạy container EC2, cấu hình Amazon SES SMTP | Spring Boot hoạt động trên port 8080 |
| [5.5. Routing và bảo mật](5.5-Traffic-security/) | ALB, health check, CloudFront behaviors, Cloudflare và SG | Traffic đi đúng chuỗi và EC2 không lộ trực tiếp |
| [5.6. Kiểm tra](5.6-Validation/) | Build/test, health và smoke test production | Có bằng chứng hệ thống hoạt động |
| [5.7. Dọn dẹp](5.7-Cleanup/) | Sao lưu và xóa tài nguyên theo phụ thuộc | Hạn chế chi phí sau demo |

> **Hình cần bổ sung:** Sơ đồ kiến trúc dùng xuyên suốt workshop.

<!-- IMAGE_PATH: /images/5-Workshop/cloud-ewallet-deployment-architecture.png -->
<!-- Sau khi thêm file, bỏ comment: ![Kiến trúc triển khai Cloud E-Wallet](/images/5-Workshop/cloud-ewallet-deployment-architecture.png) -->

> **Lưu ý:** Backend/frontend hiện deploy thủ công. Hệ thống chưa dùng ECS/Fargate, Auto Scaling hoặc CI/CD và ALB chỉ có một EC2 target.

