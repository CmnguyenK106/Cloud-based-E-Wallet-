---
title: "Cấu hình ALB, CloudFront, DNS và bảo mật mạng"
date: 2024-01-01
weight: 5
chapter: false
pre: " <b> 5.5. </b> "
---

## Mục tiêu

Hoàn thiện đường đi của request từ người dùng đến frontend và backend, đồng thời giới hạn kết nối mạng theo chuỗi CloudFront → Application Load Balancer → EC2 → Amazon RDS.


## Cấu hình target group

Nhóm chúng em tạo target group cho backend với các thông tin chính:

- Target type: EC2 instance.
- Protocol: HTTP.
- Port: `8080`.
- Health check path: `/actuator/health`.
- EC2 backend được đăng ký làm target.

ALB chỉ chuyển traffic đến EC2 sau khi health check trả kết quả hợp lệ và target chuyển sang trạng thái **Healthy**.

![Target Group của backend ở trạng thái Healthy](/images/5-Workshop/5.5-Traffic-security/target-group-healthy.png)

<p style="text-align: center;"><em>Hình 5.15. Target group sử dụng HTTP port 8080 và EC2 backend đang ở trạng thái Healthy.</em></p>

## Cấu hình Application Load Balancer

Application Load Balancer được triển khai dạng internet-facing trong các public subnet của `ewallet-vpc`. Listener HTTP port `80` chuyển request đến backend target group port `8080`.

Kết nối từ trình duyệt đến CloudFront sử dụng HTTPS. CloudFront kết nối đến ALB origin theo cấu hình hiện tại, sau đó ALB chuyển request đến Spring Boot container trên EC2.
![Thông tin và listener của Application Load Balancer](/images/5-Workshop/5.5-Traffic-security/alb-details-listener.png)

<p style="text-align: center;"><em>Hình 5.16. ALB internet-facing với listener HTTP port 80 chuyển request đến backend target group.</em></p>

## Giới hạn bằng Security Group

Các Security Group được cấu hình theo quan hệ nguồn thay vì mở toàn bộ port trực tiếp ra Internet:

| Tài nguyên | Inbound rule cần thiết | Mục đích |
| --- | --- | --- |
| ALB | HTTP port `80` theo phạm vi public của listener hiện tại | Nhận request API từ CloudFront |
| EC2 backend | TCP `8080` với source là ALB Security Group | Chỉ cho ALB gọi Spring Boot backend |
| EC2 backend | SSH `22` từ IP quản trị `/32` | Cho phép quản trị thủ công bằng MobaXterm |
| Amazon RDS | MySQL `3306` với source là EC2 Security Group | Chỉ cho backend kết nối database |

EC2 không mở port `8080` cho `0.0.0.0/0`; các port `80` và `443` trên EC2 cũng không cần mở vì máy không chạy Nginx. Public IPv4 của EC2 chỉ phục vụ quản trị SSH trong phạm vi workshop, không phải endpoint ứng dụng.

![Inbound rules của ALB Security Group](/images/5-Workshop/5.5-Traffic-security/alb-security-group-inbound.png)

<p style="text-align: center;"><em>Hình 5.17. ALB Security Group cho phép lưu lượng HTTP port 80 và HTTPS port 443 từ Internet.</em></p>

Inbound rule của EC2 đã được minh chứng tại [mục 5.3.1](../5.3-Backend-deployment/5.3.1-Backend-environment/), còn inbound rule của RDS được trình bày tại [mục 5.2.1](../5.2-Database-deployment/5.2.1-RDS-configuration/), nên không lặp lại các ảnh trong phần này.

## Định tuyến API bằng CloudFront

Distribution có hai origin và hai behavior:

- Default `(*)` sử dụng S3 frontend origin với cache policy `Managed-CachingOptimized`.
- `/api/*` sử dụng `ewallet-alb-origin` với `Managed-CachingDisabled` để response API không bị cache ngoài ý muốn.

CloudFront origin và behavior đã được minh chứng tại [mục 5.4.1](../5.4-Frontend-deployment/5.4.1-S3-CloudFront-configuration/). Phần 5.5 tập trung giải thích vai trò của chúng trong luồng định tuyến tổng thể thay vì chụp lại cùng một cấu hình.

## Cấu hình domain trên Cloudflare

Cloudflare quản lý DNS zone của `cloud-ewallet.com`. Các record trong zone được lấy từ nhiều dịch vụ khác nhau:

| Nhóm record | Nguồn cung cấp | Mục đích |
| --- | --- | --- |
| CNAME `cloud-ewallet.com` | Domain của CloudFront distribution | Chuyển domain chính đến CloudFront |
| CNAME `www` | Domain chính `cloud-ewallet.com` | Cho phép truy cập bằng `www.cloud-ewallet.com` |
| CNAME xác minh certificate | AWS Certificate Manager (ACM) | Xác minh domain và hỗ trợ tự động gia hạn certificate TLS |
| Các CNAME DKIM | Amazon SES domain identity | Xác minh DKIM cho email gửi từ domain dự án |
| MX và TXT của `send.cloud-ewallet.com` | Amazon SES Custom MAIL FROM | Cấu hình MAIL FROM và SPF cho SES |
| TXT `_dmarc` | Chính sách do nhóm cấu hình | Công bố chính sách DMARC của domain |

Các record xác minh AWS và email được đặt **DNS only**, không bật Cloudflare proxy, để ACM và SES đọc đúng giá trị DNS. Phần tạo SES domain identity, Easy DKIM và SMTP credential được trình bày tại [mục 5.3.1](../5.3-Backend-deployment/5.3.1-Backend-environment/).


![Các DNS record của Cloud E-Wallet trên Cloudflare](/images/5-Workshop/5.5-Traffic-security/cloudflare-dns-records.png)

<p style="text-align: center;"><em>Hình 5.18. Các record CloudFront, ACM và Amazon SES trong Cloudflare DNS.</em></p>

## Kiểm tra kết quả

Sau khi hoàn tất cấu hình, nhóm chúng em xác nhận:

- `https://cloud-ewallet.com` tải frontend từ S3 thông qua CloudFront.
- Request `/api/*` được CloudFront chuyển đến ALB rồi đến EC2 port `8080`.
- EC2 target trong target group ở trạng thái **Healthy**.
- Truy cập trực tiếp public EC2 port `8080` bị Security Group chặn.
- EC2 chỉ kết nối RDS qua port `3306` theo Security Group source.
- Domain, certificate và các record xác minh SES hoạt động qua Cloudflare DNS.


