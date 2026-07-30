---
title: "Dọn dẹp tài nguyên"
date: 2024-01-01
weight: 7
chapter: false
pre: " <b> 5.7. </b> "
---

## Các bước dọn dẹp

Việc dọn dẹp được thực hiện theo thứ tự phụ thuộc để tránh xóa nhầm dữ liệu và hạn chế tài nguyên còn sót tiếp tục phát sinh chi phí.

1. **Sao lưu dữ liệu cần thiết:** xuất dữ liệu cần lưu và tạo final snapshot cho Amazon RDS trước khi xóa database.
2. **Dọn dẹp Amazon CloudFront:** gỡ các alternate domain name nếu cần, disable distribution, chờ trạng thái triển khai hoàn tất rồi xóa distribution.
3. **Dọn dẹp Amazon S3:** xóa toàn bộ object và version trong frontend bucket, sau đó xóa bucket khi không còn được CloudFront sử dụng.
4. **Xóa Application Load Balancer:** xóa listener, Application Load Balancer và target group của backend.
5. **Dọn dẹp Amazon EC2:** terminate EC2 backend; kiểm tra và xóa các EBS volume, snapshot hoặc Elastic IP không còn sử dụng.
6. **Xóa Amazon RDS:** xác nhận final snapshot đã được tạo, sau đó xóa DB instance và DB subnet group không còn cần thiết.
7. **Xóa tài nguyên mạng:** xóa các Security Group khi không còn tài nguyên tham chiếu, sau đó xóa route table tùy chỉnh, subnet, Internet Gateway và VPC.
8. **Dọn dẹp IAM:** gỡ policy và xóa IAM role hoặc IAM user chỉ phục vụ dự án; vô hiệu hóa và xóa SES SMTP credential không còn sử dụng.
9. **Dọn dẹp Amazon SES và DNS:** xóa SES identity, DKIM, MAIL FROM, ACM validation và các DNS record Cloudflare khi domain không còn được dùng cho website hoặc email.
10. **Kiểm tra chi phí:** kiểm tra AWS Billing và Cost Explorer để xác nhận không còn tài nguyên ngoài dự kiến tiếp tục phát sinh phí.

Khi xóa tài nguyên, nhóm kiểm tra chính xác tên, Region và quan hệ phụ thuộc trước mỗi thao tác. CloudFront cần được disable trước khi xóa; EC2 ở trạng thái stopped vẫn có thể phát sinh phí EBS; snapshot, Elastic IP và dữ liệu S3 cũng có thể được tính phí riêng.