---
title: "Bản đề xuất"
date: 2024-01-01
weight: 2
chapter: false
pre: " <b> 2. </b> "
---


# Cloud E-Wallet – Ứng dụng ví điện tử mô phỏng triển khai trên AWS


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
- Amazon SES SMTP tại Region Singapore (`ap-southeast-1`) qua STARTTLS cho xác minh email và đặt lại mật khẩu; Resend được giữ làm phương án rollback.
- Cloudflare DNS quản lý domain và các record xác minh email.

## 4. Kiến trúc giải pháp

Luồng production đề xuất và đã được áp dụng trong dự án:

```text
Người dùng → Cloudflare DNS → Amazon CloudFront
                                  ├─ Default (*) → S3 frontend
                                  └─ /api/* → ALB → EC2/Docker/Spring Boot
                                                       ├─ RDS MySQL
                                                       └─ Amazon SES SMTP
```

> **Hình cần bổ sung:** Sơ đồ kiến trúc Cloud E-Wallet do nhóm xây dựng, thể hiện User, Cloudflare, CloudFront, S3, ALB, EC2, RDS, Internet Gateway, Amazon SES và CloudWatch.

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
| Amazon SES SMTP | Gửi email xác minh và đặt lại mật khẩu; dùng SMTP `587`, xác thực và STARTTLS |
| CloudWatch | Theo dõi metrics AWS; log/alarm tùy chỉnh chỉ ghi nhận khi có cấu hình thực tế |

## 5. Triển khai kỹ thuật

### Các giai đoạn triển khai

Dự án được nhóm chúng em thực hiện qua năm giai đoạn:

1. **Nghiên cứu và thiết kế:** Phân tích yêu cầu, xác định phạm vi ví điện tử mô phỏng, thiết kế database và kiến trúc frontend – backend – AWS.
2. **Phát triển trên môi trường local:** Xây dựng React frontend, Spring Boot REST API và MySQL; hoàn thiện xác thực, phân quyền, nghiệp vụ ví và trang quản trị.
3. **Đóng gói và chuẩn bị Cloud:** Kiểm thử backend/frontend, Docker hóa Spring Boot, tạo VPC, subnet, Security Group và chuẩn bị RDS.
4. **Triển khai và tích hợp:** Đưa frontend lên S3/CloudFront, chạy backend container trên EC2 sau ALB, kết nối RDS, cấu hình Cloudflare DNS và Amazon SES SMTP.
5. **Kiểm thử và hoàn thiện:** Thực hiện health check, smoke test nghiệp vụ, kiểm tra email, rà soát bảo mật mạng, theo dõi chi phí và hoàn thiện tài liệu.

### Yêu cầu kỹ thuật

- **Frontend:** React 19, TypeScript và Vite; build thành static files trên S3, phân phối qua CloudFront và hỗ trợ responsive.
- **Backend:** Java 17, Spring Boot, Spring Security, JDBC và Actuator; đóng gói Docker, chạy trên EC2 port `8080` và chỉ nhận traffic ứng dụng từ ALB.
- **Database:** Amazon RDS for MySQL trong private subnet; Security Group chỉ cho phép backend EC2 kết nối port `3306`; dữ liệu tiếng Việt dùng `utf8mb4`.
- **Email:** Amazon SES SMTP tại `ap-southeast-1`, port `587`, authentication và STARTTLS; domain identity/DKIM được xác minh qua Cloudflare.
- **Bảo mật:** BCrypt, JWT có thời hạn, role `user`/`admin`, secret nằm ngoài Git, HTTPS từ người dùng đến CloudFront và giới hạn inbound theo Security Group.
- **Vận hành:** ALB dùng `/actuator/health` để health check; CloudWatch cung cấp metrics AWS; frontend và backend hiện được triển khai thủ công.
## 6. Phạm vi chức năng

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

## 7. Lợi ích dự kiến

- Tạo sản phẩm thực hành full-stack và AWS có thể demo end-to-end.
- Tách rõ giao diện, API và cơ sở dữ liệu.
- Áp dụng xác thực, phân quyền và transaction vào bài toán có số dư.
- Hỗ trợ giao diện responsive và nội dung tiếng Việt UTF-8.
- Tạo nền tảng để nghiên cứu thêm ECS, CI/CD, Auto Scaling, WAF và giám sát nâng cao.

## 8. Kế hoạch thực hiện

| Giai đoạn | Nội dung |
| --- | --- |
| Tuần 1–2 | Phân tích yêu cầu, thiết kế kiến trúc, database và khởi tạo source |
| Tuần 3–5 | Xây dựng xác thực, nghiệp vụ ví, giao diện người dùng và admin |
| Tuần 6 | Kiểm thử, sửa lỗi và Docker hóa backend |
| Tuần 7–8 | Triển khai S3, CloudFront, EC2, RDS, Amazon SES và ALB; kiểm tra production |
| Tuần 9 | Hoàn thiện sản phẩm, tài liệu và báo cáo |
| Tuần 10–11 | Tìm hiểu ECS và CI/CD như hướng phát triển, chưa triển khai production |

## 9. Rủi ro và biện pháp giảm thiểu

| Rủi ro | Ảnh hưởng | Biện pháp |
| --- | --- | --- |
| Lộ secret | Cao | Tách file môi trường, dùng placeholder, không commit giá trị thật |
| Sai lệch số dư | Cao | Transaction, validation và khóa hàng ví |
| Backend gián đoạn | Cao | Health check ALB; ghi nhận giới hạn một target và đề xuất mở rộng |
| Chi phí AWS | Trung bình | Theo dõi Billing/Cost Explorer và cleanup tài nguyên |
| Email không gửi được | Trung bình | Xác minh domain trong SES, kiểm tra trạng thái sandbox, STARTTLS, SMTP credentials, bounce và complaint |

## 10. Chi phí

Chi phí dưới đây là **ước tính**, không phải hóa đơn thực tế. Nhóm chúng em giả định tài nguyên đặt tại Region Singapore (`ap-southeast-1`), sử dụng giá On-Demand, chạy 730 giờ/tháng và chưa gồm thuế hoặc Free Tier. Mức tối đa chỉ là cận trên trong phạm vi giả định của báo cáo; AWS không tự giới hạn chi phí nếu lưu lượng hoặc tài nguyên tiếp tục tăng.

### Chi phí ban đầu

| Khoản chi | Chi phí |
| --- | ---: |
| Mua tên miền `cloud-ewallet.com` qua Cloudflare | **10,98 USD** |
| Phí khởi tạo dịch vụ AWS | **0,00 USD** |
| **Tổng chi phí ban đầu, thanh toán một lần** | **10,98 USD** |

Tên miền được ghi nhận là khoản mua ban đầu theo số tiền nhóm đã thanh toán và **không được phân bổ vào chi phí duy trì hằng tháng** trong bảng dưới đây. Phí gia hạn trong tương lai chưa được tính vì chưa có số liệu gia hạn thực tế.

### Giả định sử dụng

| Hạng mục | Tối thiểu | Trung bình | Tối đa giả định |
| --- | --- | --- | --- |
| EC2 và EBS | 1 `t3.micro`, 730 giờ, 8 GB gp3 | Giống mức tối thiểu | Giống mức tối thiểu |
| RDS MySQL | 1 `db.t3.micro` Single-AZ, 20 GB | Giống mức tối thiểu | Giống mức tối thiểu |
| ALB | 730 giờ, trung bình 0,1 LCU | 730 giờ, trung bình 0,3 LCU | 730 giờ, trung bình 1 LCU |
| S3 | 1 GB, ít request | 5 GB, khoảng 30.000 GET và 3.000 PUT | 20 GB, khoảng 100.000 GET và 10.000 PUT |
| CloudFront | 5 GB và khoảng 50.000 request | 30 GB và khoảng 250.000 request | 100 GB và khoảng 1.000.000 request |
| Amazon SES | 1.000 email văn bản/tháng | 3.000 email văn bản/tháng | 10.000 email văn bản/tháng |
| CloudWatch | Chỉ metrics cơ bản | 1 GB log được ingest | 5 GB log được ingest |

### Chi phí duy trì hằng tháng

| Dịch vụ | Tối thiểu (USD) | Trung bình (USD) | Tối đa giả định (USD) |
| --- | ---: | ---: | ---: |
| EC2 `t3.micro` | 9,64 | 9,64 | 9,64 |
| EBS gp3 8 GB | 0,80 | 0,80 | 0,80 |
| RDS MySQL `db.t3.micro` + 20 GB | 21,74 | 21,74 | 21,74 |
| Application Load Balancer + LCU | 18,98 | 20,15 | 24,24 |
| S3 storage và request | 0,03 | 0,15 | 0,70 |
| CloudFront data transfer và request | 0,61 | 3,65 | 12,10 |
| Amazon SES | 0,16 | 0,48 | 1,60 |
| CloudWatch | 0,00 | 0,50 | 2,50 |
| **Tổng duy trì ước tính/tháng** | **51,96** | **57,11** | **73,32** |
| **Tổng tháng đầu nếu cộng tiền mua tên miền** | **62,94** | **68,09** | **84,30** |

### Điều kiện của từng mức chi phí

- **Tối thiểu – 51,96 USD/tháng:** Hệ thống demo chạy liên tục với một EC2 `t3.micro`, một RDS `db.t3.micro` Single-AZ và một ALB; tối đa khoảng 1 GB trên S3, 5 GB qua CloudFront, 1.000 email SES và chỉ dùng metrics CloudWatch cơ bản. Không tính Free Tier, thuế, snapshot hoặc tài nguyên phát sinh ngoài bảng.
- **Trung bình – 57,11 USD/tháng:** Giữ nguyên cấu hình compute/database nhưng có mức sử dụng thường xuyên hơn: khoảng 5 GB S3, 30 GB CloudFront, 3.000 email SES, 1 GB CloudWatch Logs và trung bình 0,3 LCU. Đây là kịch bản phù hợp cho nhóm dùng thử và trình diễn định kỳ.
- **Tối đa giả định – 73,32 USD/tháng:** Vẫn giữ một EC2 và một RDS kích thước nhỏ nhưng giả định lưu lượng tăng đến 20 GB S3, 100 GB CloudFront, 10.000 email SES, 5 GB log và trung bình 1 LCU. Nếu phải nâng loại EC2/RDS, thêm target, Multi-AZ, NAT Gateway, WAF hoặc vượt các ngưỡng này, chi phí thực tế có thể cao hơn mức trên.

Giá AWS thay đổi theo thời điểm, Region, loại tài khoản và mức sử dụng thực tế. Trước khi vận hành lâu dài, nhóm cần nhập cấu hình thật vào AWS Pricing Calculator và đối chiếu Billing/Cost Explorer. Tham khảo: [AWS EC2 Pricing](https://aws.amazon.com/ec2/pricing/on-demand/), [Amazon RDS for MySQL Pricing](https://aws.amazon.com/rds/mysql/pricing/), [Elastic Load Balancing Pricing](https://aws.amazon.com/elasticloadbalancing/pricing/), [Amazon CloudFront Pricing](https://aws.amazon.com/cloudfront/pricing/), [Amazon S3 Pricing](https://aws.amazon.com/s3/pricing/) và [Amazon SES Pricing](https://aws.amazon.com/ses/pricing/).
> **Hình cần bổ sung:** Kết quả AWS Pricing Calculator hoặc Billing đã che thông tin nhạy cảm.

<!-- IMAGE_PATH: /images/2-Proposal/aws-cost-estimate.png -->
<!-- Sau khi thêm file, bỏ comment dòng Markdown sau: ![Ước tính chi phí AWS](/images/2-Proposal/aws-cost-estimate.png) -->

## 11. Kết quả mong đợi

Sản phẩm có thể truy cập qua `https://cloud-ewallet.com`; frontend được phân phối bởi CloudFront/S3; API đi qua CloudFront/ALB đến Spring Boot container; backend kết nối RDS và gửi email bằng Amazon SES SMTP. Các workflow chính được kiểm thử và giới hạn kiến trúc được trình bày trung thực.


