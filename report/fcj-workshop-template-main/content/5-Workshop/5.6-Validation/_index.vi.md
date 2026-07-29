---
title: "Kiểm tra hệ thống sau triển khai"
date: 2024-01-01
weight: 6
chapter: false
pre: " <b> 5.6. </b> "
---

## Mục tiêu

Thu thập bằng chứng build, health, network và workflow production sau khi hoàn thành triển khai.

## Kiểm tra kỹ thuật

| Hạng mục | Kết quả mong đợi | Đã ghi nhận |
| --- | --- | --- |
| Backend test | 68 tests pass | Có |
| Backend package | Build thành công | Có |
| Frontend build/lint | Thành công | Có |
| Frontend automated tests | Không có script | Không áp dụng |
| ALB target | Healthy | Có |
| Actuator | `/actuator/health` trả `UP` | Có trong cấu hình/kiểm tra dự án |
| Direct EC2:8080 | Bị chặn | Có |
| Protected API không JWT | `401` | Chỉ chứng minh request đến Spring Security |

> **Hình cần bổ sung:** Backend tests, frontend build/lint, ALB Healthy và health UP.

<!-- IMAGE_PATH: /images/5-Workshop/5.6-Validation/backend-tests.png -->
<!-- IMAGE_PATH: /images/5-Workshop/5.6-Validation/frontend-build-lint.png -->
<!-- IMAGE_PATH: /images/5-Workshop/5.6-Validation/alb-health.png -->
<!-- IMAGE_PATH: /images/5-Workshop/5.6-Validation/actuator-health.png -->

## Smoke test production

Kiểm tra đăng ký, xác minh email, đăng nhập, profile, recipient lookup, nạp tiền mô phỏng, chuyển tiền, thanh toán, lịch sử, admin và reset password. Che email, phone, token và dữ liệu cá nhân trong ảnh.

> **Hình cần bổ sung:** Bộ ảnh smoke test người dùng và quản trị viên.

<!-- IMAGE_PATH: /images/5-Workshop/5.6-Validation/user-workflows.png -->
<!-- IMAGE_PATH: /images/5-Workshop/5.6-Validation/admin-workflows.png -->
<!-- IMAGE_PATH: /images/5-Workshop/5.6-Validation/email-workflows.png -->

## Monitoring

Actuator cung cấp health/liveness/readiness. CloudWatch có metrics mặc định cho dịch vụ AWS; repository chưa xác minh custom log group, dashboard, alarm hoặc agent. Chỉ thêm ảnh từ cấu hình thật.

> **Hình cần bổ sung:** CloudWatch metrics hoặc logs đã thực sự cấu hình.

<!-- IMAGE_PATH: /images/5-Workshop/5.6-Validation/cloudwatch-evidence.png -->

## Kết quả

Mỗi kết luận phải gắn với bằng chứng tương ứng. `401` không thay thế kiểm thử nghiệp vụ end-to-end.
