---
title: "Workshop"
date: 2024-01-01
weight: 5
chapter: false
pre: " <b> 5. </b> "
---

Phần này trình bày quy trình nhóm chúng em triển khai Cloud E-Wallet từ source lên môi trường AWS production. Các bước được sắp xếp theo quan hệ phụ thuộc giữa database, backend, frontend, định tuyến và kiểm thử; nội dung chỉ mô tả những thành phần đã được xác minh trong dự án.

![Kiến trúc triển khai Cloud E-Wallet trên AWS](/images/5-Workshop/5.1-Prerequisites/architecture.png)

<p style="text-align: center;"><em>Hình 5.1. Kiến trúc triển khai Cloud E-Wallet trên AWS.</em></p>

Trong kiến trúc này, người dùng truy cập domain do Cloudflare quản lý và request được chuyển đến CloudFront. Default behavior phân phối React frontend từ Amazon S3, còn `/api/*` đi qua Application Load Balancer đến Spring Boot container trên EC2. Backend kết nối Amazon RDS MySQL và sử dụng Amazon SES SMTP để gửi email xác minh hoặc đặt lại mật khẩu.

Cloud E-Wallet sử dụng số dư mô phỏng, không xử lý tiền thật, không kết nối cổng thanh toán và không gửi dữ liệu thẻ đến backend.

## Trình tự thực hiện

| Bước | Nội dung chính | Kết quả mong đợi |
| --- | --- | --- |
| [5.1. Chuẩn bị môi trường](5.1-Prerequisites/) | Kiểm tra công cụ và source; xác định vai trò hai IAM user; cấu hình frontend, backend, RDS, JWT, CORS và Amazon SES | Môi trường triển khai sẵn sàng, đúng Region và không để lộ secret |
| [5.2. Triển khai database](5.2-Database-deployment/) | Cấu hình RDS private, sau đó khởi tạo schema và dữ liệu nền | Database production sẵn sàng cho backend |
| [5.3. Triển khai backend](5.3-Backend-deployment/) | Chuẩn bị EC2/Docker/SES, build image và phát hành Spring Boot container | Backend kết nối RDS, gửi email và phục vụ qua ALB |
| [5.4. Triển khai frontend](5.4-Frontend-deployment/) | Cấu hình S3/CloudFront, build React và phát hành static files | Frontend được phân phối qua HTTPS bằng CloudFront hoặc custom domain |
| [5.5. Cấu hình định tuyến và bảo mật](5.5-Traffic-security/) | Tạo target group và ALB; cấu hình CloudFront behavior `/api/*`, Cloudflare DNS và Security Group | Traffic đi theo chuỗi CloudFront → ALB → EC2; EC2 và RDS không bị mở trực tiếp không cần thiết |
| [5.6. Kiểm tra sau triển khai](5.6-Validation/) | Xác nhận trạng thái backend, đăng nhập, thông tin ví, chuyển tiền và email khôi phục qua Amazon SES | Các luồng production chính hoạt động end-to-end |
| [5.7. Dọn dẹp tài nguyên](5.7-Cleanup/) | Sao lưu dữ liệu cần thiết, kiểm tra phụ thuộc rồi dừng hoặc xóa tài nguyên không còn sử dụng | Hạn chế chi phí phát sinh sau khi kết thúc demo |

