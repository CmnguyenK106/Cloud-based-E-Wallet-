---
title: "Dọn dẹp tài nguyên"
date: 2024-01-01
weight: 7
chapter: false
pre: " <b> 5.7. </b> "
---

## Mục tiêu

Tránh chi phí sau demo mà không xóa nhầm dữ liệu hoặc tài nguyên đang được sử dụng. Không dùng lệnh xóa hàng loạt với ID chưa xác minh.

## Thứ tự dọn dẹp

1. Sao lưu database và ảnh bằng chứng cần giữ.
2. Nếu không còn dùng, disable rồi xóa CloudFront.
3. Xóa object và S3 bucket sau khi xác minh đúng bucket.
4. Xóa ALB, listener và target group.
5. Dừng hoặc terminate EC2. `stop` không xóa EBS; kiểm tra EBS, snapshot và Elastic IP riêng.
6. Xóa RDS sau khi quyết định final snapshot; xóa có thể làm mất dữ liệu.
7. Xóa Security Group khi không còn phụ thuộc.
8. Chỉ xóa subnet/VPC khi không còn network interface/resource.
9. Chỉ gỡ Cloudflare/SES verification và DKIM records, xóa SES identity hoặc SMTP credentials khi domain và ứng dụng không còn sử dụng email.
10. Kiểm tra AWS Billing/Cost Explorer sau cleanup.

> **Hình cần bổ sung:** Danh sách tài nguyên trước cleanup và Billing trước/sau cleanup.

<!-- IMAGE_PATH: /images/5-Workshop/5.7-Cleanup/resource-inventory.png -->
<!-- IMAGE_PATH: /images/5-Workshop/5.7-Cleanup/billing-before.png -->
<!-- IMAGE_PATH: /images/5-Workshop/5.7-Cleanup/billing-after.png -->

## Cảnh báo

CloudFront có thể cần thời gian disable trước khi delete. Không xóa Cloudflare zone nếu còn phục vụ domain/email khác. Snapshot, EBS, Elastic IP và S3 có thể phát sinh chi phí riêng.
