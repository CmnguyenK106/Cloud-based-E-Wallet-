---
title: "Cài đặt và cấu hình môi trường backend"
date: 2024-01-01
weight: 1
chapter: false
pre: " <b> 5.3.1. </b> "
---

## Mục tiêu

Chuẩn bị môi trường chạy Spring Boot backend trên Amazon EC2, cài đặt Docker và cấu hình kết nối đến Amazon RDS cùng Amazon SES. Phần này trình bày ngắn gọn quy trình thực hiện và sử dụng các ảnh trạng thái sau khi cấu hình làm minh chứng.

## Quy trình tổng quát

Nhóm chúng em thực hiện theo bốn giai đoạn:

1. Tạo EC2 trong VPC và subnet đã chuẩn bị, sau đó gắn IAM role cùng Security Group phù hợp.
2. Kết nối EC2 bằng SSH, cài đặt Docker và kiểm tra Docker Engine.
3. Chuẩn bị Amazon SES tại Region Singapore và tạo SMTP credential riêng cho backend.
4. Tạo file biến môi trường trên EC2 để container kết nối RDS, SES và domain frontend mà không đưa secret vào source.

## Chuẩn bị Amazon EC2

EC2 backend sử dụng Amazon Linux 2023 và instance type `t3.micro`. Instance được đặt trong public subnet `ewallet-subnet-public1-ap-southeast-1a` của `ewallet-vpc` để nhóm có thể quản trị thủ công qua SSH trong phạm vi workshop.

IAM role `CloudEwalletEC2Role` được gắn cho instance để ứng dụng hoặc công cụ trên EC2 sử dụng quyền AWS cần thiết mà không lưu access key dài hạn trên máy chủ. Instance yêu cầu IMDSv2 nhằm tăng mức bảo vệ khi truy cập Instance Metadata Service.

Security Group của EC2 được cấu hình theo nguyên tắc:

- SSH TCP `22` chỉ nhận từ IP quản trị `/32`.
- Port backend `8080` chỉ nhận traffic từ Security Group của Application Load Balancer.
- Không mở trực tiếp port `8080` cho `0.0.0.0/0`.
- Cho phép outbound cần thiết để tải package, kết nối RDS và kết nối Amazon SES SMTP.


![Thông tin EC2 backend sau khi cấu hình](/images/5-Workshop/5.3-Backend-deployment/5.3.1-Backend-environment/ec2-instance-summary.png)

<p style="text-align: center;"><em>Hình 5.5. EC2 backend đang hoạt động trong ewallet-vpc với IAM role và IMDSv2.</em></p>
![Inbound rules của EC2 Security Group](/images/5-Workshop/5.3-Backend-deployment/5.3.1-Backend-environment/ec2-security-group-inbound.png)

<p style="text-align: center;"><em>Hình 5.6. EC2 Security Group chỉ cho phép SSH từ IP quản trị và traffic backend port 8080 từ ALB Security Group.</em></p>

## Cài đặt Docker

Sau khi EC2 ở trạng thái **Running**, nhóm kết nối bằng MobaXterm qua SSH và cài Docker trên Amazon Linux 2023. Các lệnh chính gồm:

```bash
sudo dnf update -y
sudo dnf install -y docker
sudo systemctl enable --now docker
sudo usermod -aG docker ec2-user
```

Sau khi thêm `ec2-user` vào group `docker`, cần đăng xuất rồi kết nối lại để quyền group có hiệu lực. Docker được kiểm tra bằng:

```bash
docker version
```

Kết quả xác nhận Docker Client và Docker Engine đã hoạt động trên kiến trúc `linux/amd64`, sẵn sàng chạy Spring Boot container.

![Kết quả kiểm tra Docker trên EC2](/images/5-Workshop/5.3-Backend-deployment/5.3.1-Backend-environment/docker-version.png)

<p style="text-align: center;"><em>Hình 5.7. Docker Client và Docker Engine đã được cài đặt trên EC2.</em></p>

## Chuẩn bị Amazon SES

Backend sử dụng Amazon SES SMTP để gửi email xác minh tài khoản và đặt lại mật khẩu. Nhóm cấu hình SES tại Singapore (`ap-southeast-1`) để đồng nhất với Region triển khai chính:

1. Tạo và xác minh domain identity `cloud-ewallet.com`.
2. Bật Easy DKIM và thêm các record SES cung cấp vào Cloudflare DNS; các record email không bật proxy.
3. Tạo SMTP credential riêng cho ứng dụng bằng IAM user `ses-smtp-user-cloud-ewallet`.
4. Sử dụng SMTP endpoint `email-smtp.ap-southeast-1.amazonaws.com`, port `587` và STARTTLS.
5. Yêu cầu production access để gửi email đến người nhận chưa xác minh.

Tài khoản SES hiện có trạng thái **Healthy**, hạn mức `50.000` email trong 24 giờ và tốc độ tối đa `14` email mỗi giây. Đây là hạn mức gửi tối đa được AWS phê duyệt, không phải số email miễn phí.

![Trạng thái và hạn mức Amazon SES](/images/5-Workshop/5.3-Backend-deployment/5.3.1-Backend-environment/ses-account-dashboard.png)

<p style="text-align: center;"><em>Hình 5.8. Amazon SES tại Region Singapore ở trạng thái Healthy với hạn mức gửi đã được phê duyệt.</em></p>

## Chuẩn bị biến môi trường backend

Trên EC2, nhóm tạo file `/home/ec2-user/ewallet-backend.env` và giới hạn quyền đọc:

```bash
chmod 600 /home/ec2-user/ewallet-backend.env
```

File thật chứa cấu hình RDS, JWT, frontend/CORS và SES. Báo cáo chỉ sử dụng placeholder:

```dotenv
SPRING_PROFILES_ACTIVE=prod

DB_URL=jdbc:mysql://<DB_ENDPOINT>:3306/<DB_NAME>?useUnicode=true&characterEncoding=UTF-8&useSSL=true&requireSSL=true&serverTimezone=UTC
DB_USERNAME=<DB_USERNAME>
DB_PASSWORD=<DB_PASSWORD>

JWT_SECRET=<STRONG_RANDOM_SECRET_AT_LEAST_32_BYTES>
JWT_EXPIRATION=3600

FRONTEND_BASE_URL=https://cloud-ewallet.com
CORS_ALLOWED_ORIGINS=https://cloud-ewallet.com

MAIL_DEVELOPMENT_LOG_ENABLED=false
EMAIL_VERIFICATION_MINUTES=1440
PASSWORD_RESET_MINUTES=30
EMAIL_PROVIDER=ses

SES_SMTP_HOST=email-smtp.ap-southeast-1.amazonaws.com
SES_SMTP_PORT=587
SES_SMTP_USERNAME=<SES_SMTP_USERNAME>
SES_SMTP_PASSWORD=<SES_SMTP_PASSWORD>
SES_MAIL_FROM_ADDRESS=noreply@cloud-ewallet.com
```

Credential thật không được commit vào Git, ghi trong báo cáo hoặc hiển thị trong ảnh chụp màn hình.

## Kiểm tra kết quả

Sau khi hoàn tất, nhóm chúng em xác nhận:

- EC2 ở trạng thái **Running**, thuộc đúng VPC và public subnet.
- IAM role và IMDSv2 được cấu hình cho instance.
- Docker Client và Docker Engine hoạt động bình thường.
- Security Group cho phép EC2 kết nối RDS qua port `3306`; outbound của EC2 cho phép kết nối SES SMTP qua port `587`.
- SES ở trạng thái Healthy và đã có production sending quota.
- File môi trường được đặt quyền `600`, chỉ owner được đọc/ghi và không xuất hiện trong repository.

Quy trình build image, chạy container và kiểm tra backend được thực hiện ở mục 5.3.2.