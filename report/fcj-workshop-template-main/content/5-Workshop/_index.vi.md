---
title: "Workshop"
date: 2024-01-01
weight: 5
chapter: false
pre: " <b> 5. </b> "
---

Phần này trình bày quy trình nhóm chúng em triển khai Cloud E-Wallet từ source lên môi trường AWS production. Các bước được sắp xếp theo quan hệ phụ thuộc giữa frontend, database, backend, định tuyến và kiểm thử; nội dung chỉ mô tả những thành phần đã được xác minh trong dự án.

![Kiến trúc triển khai Cloud E-Wallet trên AWS](/images/5-Workshop/5.1-Prerequisites/architecture.png)

*Hình 5.1. Kiến trúc triển khai Cloud E-Wallet trên AWS.*

Trong kiến trúc này, người dùng truy cập domain do Cloudflare quản lý và request được chuyển đến CloudFront. Default behavior phân phối React frontend từ Amazon S3, còn `/api/*` đi qua Application Load Balancer đến Spring Boot container trên EC2. Backend kết nối Amazon RDS MySQL và sử dụng Amazon SES SMTP để gửi email xác minh hoặc đặt lại mật khẩu.

Cloud E-Wallet sử dụng số dư mô phỏng, không xử lý tiền thật, không kết nối cổng thanh toán và không gửi dữ liệu thẻ đến backend.

## Trình tự thực hiện

| Bước | Nội dung chính | Kết quả mong đợi |
| --- | --- | --- |
| [5.1. Chuẩn bị môi trường](5.1-Prerequisites/) | Kiểm tra công cụ và source; tham khảo IAM policy; cấu hình frontend, backend, RDS, JWT, CORS và Amazon SES | Môi trường triển khai sẵn sàng, đúng Region và không để lộ secret |
| [5.2. Triển khai frontend](5.2-Frontend-deployment/) | Build React/Vite, upload static files lên S3, cấu hình S3 origin và default behavior của CloudFront | Frontend được phân phối qua HTTPS bằng CloudFront hoặc domain tùy chỉnh |
| [5.3. Triển khai database](5.3-Database-deployment/) | Tạo RDS MySQL trong private subnet, giới hạn Security Group và khởi tạo schema/seed | Database production hoạt động và chỉ backend được phép kết nối |
| [5.4. Triển khai backend](5.4-Backend-deployment/) | Build/test Spring Boot, tạo Docker image, chạy container trên EC2 và cấu hình Amazon SES SMTP | Backend hoạt động trên port `8080`, kết nối RDS và gửi được email nghiệp vụ |
| [5.5. Cấu hình định tuyến và bảo mật](5.5-Traffic-security/) | Tạo target group và ALB; cấu hình CloudFront behavior `/api/*`, Cloudflare DNS và Security Group | Traffic đi theo chuỗi CloudFront → ALB → EC2; EC2 và RDS không bị mở trực tiếp không cần thiết |
| [5.6. Kiểm tra sau triển khai](5.6-Validation/) | Kiểm tra build, health check, network, workflow người dùng/admin và SES Sending Statistics | Có bằng chứng kỹ thuật và smoke test xác nhận hệ thống hoạt động |
| [5.7. Dọn dẹp tài nguyên](5.7-Cleanup/) | Sao lưu dữ liệu cần thiết, kiểm tra phụ thuộc rồi dừng hoặc xóa tài nguyên không còn sử dụng | Hạn chế chi phí phát sinh sau khi kết thúc demo |

> **Phạm vi triển khai hiện tại:** Frontend và backend được triển khai thủ công. Hệ thống sử dụng một EC2 target sau Application Load Balancer; ECS/Fargate, Auto Scaling và CI/CD được xác định là các hướng mở rộng trong giai đoạn tiếp theo.