---
title: "Bản đề xuất"
date: 2024-01-01
weight: 2
chapter: false
pre: " <b> 2. </b> "
---


**Cloud E-Wallet – Ứng dụng ví điện tử mô phỏng triển khai trên AWS**


## 1. Tóm tắt đề xuất

Nhóm chúng em đề xuất xây dựng **Cloud E-Wallet**, một ứng dụng Web mô phỏng các nghiệp vụ cơ bản của ví điện tử và triển khai trên AWS. Hệ thống giúp người dùng thực hành đăng ký, xác minh email, quản lý tài khoản, theo dõi số dư, nạp tiền mô phỏng, chuyển tiền, thanh toán dịch vụ và xem lịch sử giao dịch. Quản trị viên có thể theo dõi tổng quan, quản lý người dùng, giao dịch và danh mục dịch vụ.

Dự án phục vụ học tập và trình diễn kỹ thuật, không xử lý tiền thật, không kết nối ngân hàng hoặc cổng thanh toán thật và không lưu dữ liệu thẻ.

## 2. Vấn đề

Một ứng dụng ví điện tử dù ở mức mô phỏng vẫn cần giải quyết đồng thời nhiều yêu cầu: xác thực an toàn, phân quyền người dùng/quản trị viên, cập nhật số dư nhất quán, lưu lịch sử giao dịch, cung cấp giao diện responsive và triển khai các thành phần Web trên Cloud.

Nếu chỉ chạy local, nhóm khó đánh giá đầy đủ luồng truy cập production, cấu hình domain/HTTPS, tách frontend-backend-database, bảo mật mạng, health check và dịch vụ email. Vì vậy, dự án cần một kiến trúc AWS đủ rõ ràng để triển khai end-to-end nhưng vẫn phù hợp phạm vi thực tập.

## 3. Giải pháp đề xuất

Giải pháp gồm:

- React 19, TypeScript và Vite cho frontend.
- Java 17, Spring Boot, Spring Security và JDBC cho REST API.
- MySQL cho dữ liệu người dùng, token, ví, dịch vụ và giao dịch.
- BCrypt và JWT cho xác thực; role `user`/`admin` cho phân quyền.
- Database transaction và khóa hàng ví để bảo vệ cập nhật số dư.
- Amazon S3 và CloudFront để phân phối frontend.
- Application Load Balancer và EC2 chạy Dockerized Spring Boot cho backend.
- Amazon RDS MySQL trong private subnet.
- Resend SMTP qua STARTTLS cho xác minh email và đặt lại mật khẩu.
- Cloudflare DNS quản lý domain và các record xác minh email.

## 4. Kiến trúc giải pháp

Luồng production đề xuất và đã được áp dụng trong dự án:

```text
Người dùng → Cloudflare DNS → Amazon CloudFront
                                  ├─ Default (*) → S3 frontend
                                  └─ /api/* → ALB → EC2/Docker/Spring Boot
                                                       ├─ RDS MySQL
                                                       └─ Resend SMTP
```

> **Hình cần bổ sung:** Sơ đồ kiến trúc Cloud E-Wallet do nhóm xây dựng, thể hiện User, Cloudflare, CloudFront, S3, ALB, EC2, RDS, Internet Gateway, Resend và CloudWatch.

<!-- IMAGE_PATH: /images/2-Proposal/cloud-ewallet-architecture.png -->
<!-- Sau khi thêm file, bỏ comment dòng Markdown sau: ![Kiến trúc Cloud E-Wallet](/images/2-Proposal/cloud-ewallet-architecture.png) -->

| Thành phần | Vai trò |
| --- | --- |
| Cloudflare DNS | Quản lý `cloud-ewallet.com` và record xác minh sender domain |
| CloudFront | Nhận HTTPS từ trình duyệt; định tuyến frontend và `/api/*` |
| S3 | Lưu static build React |
| ALB | Chuyển tiếp API, thực hiện health check backend |
| EC2 | Chạy Spring Boot trong Docker |
| RDS MySQL | Lưu dữ liệu trong private subnet |
| Resend SMTP | Gửi email xác minh và đặt lại mật khẩu |
| CloudWatch | Theo dõi metrics AWS; log/alarm tùy chỉnh chỉ ghi nhận khi có cấu hình thực tế |

## 5. Phạm vi chức năng

### Người dùng

- Đăng ký, xác minh/gửi lại email xác minh, đăng nhập và đăng xuất.
- Quên và đặt lại mật khẩu.
- Xem/cập nhật hồ sơ và số dư.
- Nạp tiền mô phỏng, tra cứu người nhận, chuyển tiền và thanh toán dịch vụ.
- Xem lịch sử giao dịch.

### Quản trị viên

- Dashboard tổng quan.
- Xem và khóa/mở khóa người dùng.
- Xem giao dịch.
- Thêm, sửa, kích hoạt hoặc vô hiệu hóa dịch vụ.

### Ngoài phạm vi

Tiền thật, KYC, OTP/SMS thật, payment gateway, ECS/Fargate, Auto Scaling và CI/CD không thuộc phiên bản đề xuất ban đầu. ALB chỉ có một EC2 target nên hệ thống chưa đạt high availability đầy đủ.

## 6. Lợi ích dự kiến

- Tạo sản phẩm thực hành full-stack và AWS có thể demo end-to-end.
- Tách rõ giao diện, API và cơ sở dữ liệu.
- Áp dụng xác thực, phân quyền và transaction vào bài toán có số dư.
- Hỗ trợ giao diện responsive và nội dung tiếng Việt UTF-8.
- Tạo nền tảng để nghiên cứu thêm ECS, CI/CD, Auto Scaling, WAF và giám sát nâng cao.

## 7. Kế hoạch thực hiện

| Giai đoạn | Nội dung |
| --- | --- |
| Tuần 1–2 | Phân tích yêu cầu, thiết kế kiến trúc, database và khởi tạo source |
| Tuần 3–5 | Xây dựng xác thực, nghiệp vụ ví, giao diện người dùng và admin |
| Tuần 6 | Kiểm thử, sửa lỗi và Docker hóa backend |
| Tuần 7–8 | Triển khai S3, CloudFront, EC2, RDS, Resend và ALB; kiểm tra production |
| Tuần 9 | Hoàn thiện sản phẩm, tài liệu và báo cáo |
| Tuần 10–11 | Tìm hiểu ECS và CI/CD như hướng phát triển, chưa triển khai production |

## 8. Rủi ro và biện pháp giảm thiểu

| Rủi ro | Ảnh hưởng | Biện pháp |
| --- | --- | --- |
| Lộ secret | Cao | Tách file môi trường, dùng placeholder, không commit giá trị thật |
| Sai lệch số dư | Cao | Transaction, validation và khóa hàng ví |
| Backend gián đoạn | Cao | Health check ALB; ghi nhận giới hạn một target và đề xuất mở rộng |
| Chi phí AWS | Trung bình | Theo dõi Billing/Cost Explorer và cleanup tài nguyên |
| Email không gửi được | Trung bình | Kiểm tra STARTTLS, biến SMTP và domain verification |

## 9. Chi phí

Repository không chứa một AWS Pricing Calculator estimate đã xác minh cho kiến trúc này. Nhóm chúng em không tự đưa ra con số giả; chi phí cần được bổ sung từ cấu hình production thực tế của S3, CloudFront, ALB, EC2, RDS và data transfer.

> **Hình cần bổ sung:** Kết quả AWS Pricing Calculator hoặc Billing đã che thông tin nhạy cảm.

<!-- IMAGE_PATH: /images/2-Proposal/aws-cost-estimate.png -->
<!-- Sau khi thêm file, bỏ comment dòng Markdown sau: ![Ước tính chi phí AWS](/images/2-Proposal/aws-cost-estimate.png) -->

## 10. Kết quả mong đợi

Sản phẩm có thể truy cập qua `https://cloud-ewallet.com`; frontend được phân phối bởi CloudFront/S3; API đi qua CloudFront/ALB đến Spring Boot container; backend kết nối RDS và gửi email Resend. Các workflow chính được kiểm thử và giới hạn kiến trúc được trình bày trung thực.


