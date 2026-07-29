---
title: "Chuẩn bị môi trường triển khai"
date: 2024-01-01
weight: 1
chapter: false
pre: " <b> 5.1. </b> "
---

## Mục tiêu

Chuẩn bị đầy đủ công cụ, source, tài khoản và cấu hình trước khi tạo hoặc cập nhật tài nguyên AWS.

## Công cụ

| Thành phần | Kiểm tra | Yêu cầu |
| --- | --- | --- |
| Java 17 | `java -version` | Build Spring Boot |
| Node.js/npm | `node --version`, `npm --version` | Build React/Vite |
| Docker | `docker --version` | Build và chạy backend image |
| Git | `git --version` | Quản lý source |
| AWS account | Đăng nhập Console | Phân quyền phù hợp cho S3, CloudFront, EC2, ALB, RDS và SES |
| Cloudflare | Kiểm tra zone | Quản lý `cloud-ewallet.com` và các record xác minh Amazon SES |

> **Hình cần bổ sung:** Terminal kiểm tra phiên bản công cụ.

<!-- IMAGE_PATH: /images/5-Workshop/5.1-Prerequisites/tool-versions.png -->

> **Hình cần bổ sung:** Cây thư mục source, không hiển thị file môi trường thật.

<!-- IMAGE_PATH: /images/5-Workshop/5.1-Prerequisites/source-structure.png -->

## Phân quyền IAM

Trong trường hợp phân quyền riêng cho người triển khai, có thể sử dụng IAM user hoặc role thay vì root account. Policy tham khảo dưới đây bao gồm các quyền cần thiết cho những công việc thường xuyên của dự án: cập nhật frontend trên S3, tạo CloudFront invalidation, xem trạng thái EC2/ALB/RDS, kiểm tra CloudWatch và gửi/kiểm tra email bằng Amazon SES.

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "UpdateFrontendBucket",
      "Effect": "Allow",
      "Action": [
        "s3:ListBucket",
        "s3:GetBucketLocation"
      ],
      "Resource": "arn:aws:s3:::<FRONTEND_BUCKET_NAME>"
    },
    {
      "Sid": "ManageFrontendObjects",
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject",
        "s3:DeleteObject"
      ],
      "Resource": "arn:aws:s3:::<FRONTEND_BUCKET_NAME>/*"
    },
    {
      "Sid": "RefreshCloudFront",
      "Effect": "Allow",
      "Action": [
        "cloudfront:GetDistribution",
        "cloudfront:GetDistributionConfig",
        "cloudfront:CreateInvalidation"
      ],
      "Resource": "arn:aws:cloudfront::<AWS_ACCOUNT_ID>:distribution/<DISTRIBUTION_ID>"
    },
    {
      "Sid": "InspectRuntimeResources",
      "Effect": "Allow",
      "Action": [
        "ec2:DescribeInstances",
        "ec2:DescribeSecurityGroups",
        "elasticloadbalancing:DescribeLoadBalancers",
        "elasticloadbalancing:DescribeListeners",
        "elasticloadbalancing:DescribeTargetGroups",
        "elasticloadbalancing:DescribeTargetHealth",
        "rds:DescribeDBInstances",
        "rds:DescribeDBSubnetGroups",
        "cloudwatch:ListMetrics",
        "cloudwatch:GetMetricData",
        "cloudwatch:GetMetricStatistics"
      ],
      "Resource": "*"
    },
    {
      "Sid": "SendFromProjectIdentity",
      "Effect": "Allow",
      "Action": [
        "ses:SendEmail",
        "ses:SendRawEmail",
        "sesv2:GetEmailIdentity"
      ],
      "Resource": "arn:aws:ses:ap-southeast-1:<AWS_ACCOUNT_ID>:identity/cloud-ewallet.com"
    },
    {
      "Sid": "InspectSESAccount",
      "Effect": "Allow",
      "Action": [
        "ses:GetSendQuota",
        "ses:GetSendStatistics",
        "sesv2:GetAccount"
      ],
      "Resource": "*"
    }
  ]
}
```

Các giá trị `<FRONTEND_BUCKET_NAME>`, `<AWS_ACCOUNT_ID>` và `<DISTRIBUTION_ID>` phải được thay bằng ID thật trước khi tạo policy. Những quyền `Describe`, CloudWatch và API quota của SES không hỗ trợ giới hạn theo ARN nên cần `Resource: "*"`. Quyền tạo/xóa VPC, EC2, ALB hoặc RDS không đưa vào policy vận hành này; nếu cần dựng hoặc dọn toàn bộ hạ tầng, nhóm sử dụng một role triển khai riêng có thời hạn và chỉ cấp thêm quyền đúng với bước đang thực hiện.


## Chuẩn bị cấu hình môi trường

Trước khi build và triển khai, nhóm chúng em tách cấu hình của frontend và backend thành hai phần.

### Frontend

Frontend đọc file `frontend/.env.production` khi chạy lệnh build. File này khai báo `VITE_API_BASE_URL`, tức địa chỉ API mà ứng dụng React sẽ gọi sau khi được đưa lên S3 và CloudFront. Vì biến của Vite được nhúng vào mã JavaScript khi build, file này không được chứa mật khẩu hoặc secret.

Có thể cấu hình frontend theo một trong hai cách sau:

```dotenv
# Cách 1: Dùng domain mặc định của CloudFront
# Thay <CLOUDFRONT_DISTRIBUTION_DOMAIN> bằng domain riêng của distribution
VITE_API_BASE_URL=https://<CLOUDFRONT_DISTRIBUTION_DOMAIN>

# Ví dụ định dạng: https://dxxxxxxxxxxxxx.cloudfront.net
```

```dotenv
# Cách 2: Dùng tên miền tùy chỉnh đã trỏ đến CloudFront
VITE_API_BASE_URL=https://cloud-ewallet.com
```

Mỗi CloudFront distribution có một domain riêng nên cần lấy đúng giá trị trong AWS Console; domain này là định danh công khai, không phải password hoặc secret. Dự án của nhóm sử dụng `https://cloud-ewallet.com`. Chỉ giữ một giá trị `VITE_API_BASE_URL` đang áp dụng trong file thật. Sau khi thay đổi biến, frontend phải được build và upload lại lên S3; nếu CloudFront còn cache phiên bản cũ thì tạo invalidation cho `/*`.

### Backend

Backend trên EC2 đọc file `/home/ec2-user/ewallet-backend.env` khi Docker container khởi động. Dưới đây là cấu hình theo đúng tên biến của dự án; các thông tin bí mật được thay bằng placeholder:

```dotenv
SPRING_PROFILES_ACTIVE=prod

DB_URL=jdbc:mysql://<RDS_ENDPOINT>:3306/<DB_NAME>?useUnicode=true&characterEncoding=UTF-8&useSSL=true&requireSSL=true&serverTimezone=UTC
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

Các biến được chia theo mục đích:

| Nhóm cấu hình | Biến sử dụng | Mục đích |
| --- | --- | --- |
| Cơ sở dữ liệu | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Kết nối backend với Amazon RDS MySQL |
| Xác thực | `JWT_SECRET` | Ký và xác minh access token |
| Domain và CORS | `FRONTEND_BASE_URL`, `CORS_ALLOWED_ORIGINS` | Tạo đường dẫn gửi cho người dùng và chỉ cho phép frontend hợp lệ gọi API |
| Dịch vụ email | `EMAIL_PROVIDER=ses`, các biến `SES_SMTP_*` và `SES_MAIL_FROM_ADDRESS` | Kết nối Amazon SES SMTP để gửi email xác minh và đặt lại mật khẩu |

File môi trường thật chỉ được lưu trên máy triển khai và không được commit lên Git. Trong source và báo cáo, nhóm chỉ sử dụng file mẫu `.env.production.example` với các giá trị thay thế như `<DB_ENDPOINT>`, `<JWT_SECRET>` hoặc `<SES_SMTP_PASSWORD>`.

## Kiểm tra trước khi triển khai

- Source có đủ ba thư mục `frontend/`, `backend/` và `database/`.
- `frontend/.env.production` trỏ đến đúng API production và không chứa secret.
- `.env.production.example` chỉ chứa placeholder, không chứa credential thật.
- File `/home/ec2-user/ewallet-backend.env` đã được tạo trực tiếp trên EC2 và được giới hạn quyền truy cập.
- Tài khoản AWS có các quyền cần thiết theo nguyên tắc đặc quyền tối thiểu.
- Nhóm đã thống nhất Region Singapore (`ap-southeast-1`), quy tắc đặt tên và danh sách tài nguyên cần triển khai.

## Kết quả mong đợi

Mọi thành viên hiểu cùng quy trình và không cần chia sẻ secret qua source hoặc báo cáo.
