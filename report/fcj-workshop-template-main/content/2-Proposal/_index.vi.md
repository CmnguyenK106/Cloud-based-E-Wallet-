---
title: "Bản đề xuất"
date: 2024-01-01
weight: 2
chapter: false
pre: " <b> 2. </b> "
---

# Cloud E-Wallet
## Hệ thống ví điện tử mô phỏng triển khai trên AWS

### 1. Giới thiệu tổng quan

Nhóm đề xuất dự án **Cloud E-Wallet**, một ứng dụng web mô phỏng ví điện tử cho phép người dùng đăng ký, xác minh tài khoản, quản lý hồ sơ và số dư, nạp tiền mô phỏng, chuyển tiền, thanh toán dịch vụ và xem lịch sử giao dịch. Quản trị viên có thể theo dõi tổng quan, quản lý người dùng, giao dịch và danh mục dịch vụ. Dự án phục vụ học tập và trình diễn kỹ thuật, không xử lý tiền thật, không kết nối ngân hàng hoặc cổng thanh toán và không lưu dữ liệu thẻ.

Bên cạnh chức năng nghiệp vụ, dự án tập trung triển khai một hệ thống nhiều tầng trên AWS, trong đó frontend, backend và database được tách biệt; request được định tuyến qua các lớp phù hợp; transactional email được gửi bằng Amazon SES; metrics và tình trạng tài nguyên được theo dõi bằng Amazon CloudWatch.

### 2. Tuyên bố vấn đề

Một ứng dụng ví điện tử, dù chỉ mô phỏng, vẫn cần giải quyết đồng thời nhiều yêu cầu: xác thực và phân quyền, cập nhật số dư nhất quán, lưu lịch sử giao dịch, giao diện dễ sử dụng, gửi email theo từng người dùng và vận hành ổn định khi một máy chủ gặp lỗi. Môi trường localhost không thể thể hiện đầy đủ HTTPS, định tuyến frontend/API, health check, phân tách mạng, kiểm soát truy cập hay khả năng thay thế máy chủ không khỏe.

#### *Giải pháp*

Cloud E-Wallet được thiết kế với Amazon S3 và Amazon CloudFront để lưu trữ, phân phối frontend React qua HTTPS. AWS WAF được gắn với CloudFront để kiểm tra request trước origin. Nhánh `/api/*` đi qua Application Load Balancer và Target Group đến hai EC2 backend trong hai private subnet tại hai Availability Zone, do Auto Scaling Group quản lý. Backend Spring Boot chạy trong Docker, truy cập Amazon RDS for MySQL thông qua mạng private và gửi transactional email bằng Amazon SES. Một NAT Gateway trong public subnet cung cấp outbound cho EC2 private; Amazon CloudWatch và AWS KMS hỗ trợ theo dõi tài nguyên và quản lý khóa mã hóa trong phạm vi cấu hình được xác minh.

#### *Cơ sở lựa chọn giải pháp*

- **S3 kết hợp CloudFront** giúp frontend tĩnh không phải chạy chung trên EC2, giảm tải cho backend và cung cấp một endpoint HTTPS có khả năng cache tại edge.
- **AWS WAF** giúp lọc hoặc theo dõi các mẫu request web phổ biến trước origin. WAF bổ sung một lớp bảo vệ ở edge nhưng không thay thế Spring Security, JWT và validation trong backend.
- **ALB và Target Group** tạo điểm truy cập API ổn định, thực hiện health check và chỉ phân phối request đến các target khỏe mạnh.
- **Auto Scaling Group** duy trì hai EC2 tại hai Availability Zone và có thể thay thế instance không khỏe. Cấu hình `Min = 0`, `Desired = 2`, `Max = 2` ưu tiên availability của tầng ứng dụng nhưng vẫn giới hạn số tài nguyên phát sinh.
- **EC2 trong các private subnet** không nhận kết nối trực tiếp từ Internet. Cách bố trí này giảm bề mặt truy cập và tạo thêm một lớp defense-in-depth giữa ALB với backend.
- **Một NAT Gateway trong public subnet** cung cấp outbound cho EC2 private khi tải package, Docker image hoặc kết nối public service endpoint như SES SMTP. Thiết kế một NAT Gateway phù hợp giới hạn chi phí của dự án, nhưng vẫn tạo một điểm phụ thuộc cho outbound của hai AZ.
- **RDS MySQL Single-AZ** giúp nhóm sử dụng database managed service và kiểm soát chi phí. Đổi lại, database vẫn là single point of failure của kiến trúc hiện tại.
- **Amazon SES** phù hợp với transactional email có người nhận và nội dung thay đổi theo từng tài khoản. Amazon SNS phù hợp hơn với notification hoặc alert gửi tới nhóm subscriber cố định nên không thay thế tương đương cho email xác minh người dùng.
- **CloudWatch** hỗ trợ theo dõi metrics và tình trạng hoạt động của CloudFront, WAF, ALB, EC2, ASG và RDS trong phạm vi dữ liệu mà từng dịch vụ cung cấp.

### 3. Kiến trúc giải pháp

#### *Sơ đồ*

![Kiến trúc triển khai Cloud E-Wallet trên AWS](/images/5-Workshop/5.1-Prerequisites/architecture.png)
<p align="center"><i>Sơ đồ triển khai Cloud E-Wallet trên AWS</i></p>

#### *Luồng xử lý*

1. **User → CloudFront/WAF:** Người dùng gửi HTTPS request đến CloudFront distribution được bảo vệ bởi AWS WAF.
2. **CloudFront → S3:** Với nội dung frontend, CloudFront lấy static files React từ S3 và cache tại edge locations.
3. **CloudFront → ALB:** Request có path `/api/*` được chuyển đến internet-facing Application Load Balancer.
4. **ALB → Target Group → EC2:** ALB kiểm tra `/actuator/health` và forward request đến các EC2 healthy trong Target Group trên port `8080`. ASG quản lý vòng đời EC2; request không đi qua ASG.
5. **EC2 → RDS:** Backend kết nối RDS MySQL qua port `3306` để đọc, ghi và xử lý transaction.
6. **EC2 → SES:** Backend gửi email xác minh, gửi lại email xác minh, đặt lại mật khẩu và các email giao dịch phù hợp qua Amazon SES SMTP port `587` với authentication và STARTTLS.
7. **EC2 → NAT Gateway → Internet Gateway:** Các EC2 private chủ động tạo kết nối outbound qua NAT Gateway trong public subnet và Internet Gateway khi cần truy cập Internet hoặc public service endpoint.
8. **Monitoring:** CloudWatch thu thập metrics và hỗ trợ theo dõi tình trạng của các dịch vụ phù hợp; CloudWatch và KMS không nằm trong synchronous request path.

#### *Dịch vụ sử dụng*

| Thành phần | Vai trò |
| --- | --- |
| Amazon S3 | Lưu static build của frontend React trong private bucket |
| Amazon CloudFront | Cung cấp HTTPS, cache frontend và định tuyến `/api/*` đến ALB |
| AWS WAF | Kiểm tra request tại edge bằng Web ACL gắn với CloudFront |
| Application Load Balancer | Nhận API traffic và phân phối đến target healthy |
| Target Group | Đăng ký EC2 port `8080` và health check `/actuator/health` |
| Auto Scaling Group | Quản lý hai EC2 tại hai AZ với `Min 0 / Desired 2 / Max 2` |
| Amazon EC2 | Chạy backend Spring Boot trong Docker |
| Amazon RDS for MySQL | Lưu dữ liệu giao dịch; DB subnet group bao phủ hai private subnet và DB instance chạy Single-AZ |
| Amazon SES SMTP | Gửi transactional email qua port `587`, authentication và STARTTLS |
| Amazon CloudWatch | Theo dõi metrics và health của các tài nguyên phù hợp |
| Internet Gateway | Cung cấp route Internet cho các public subnet |
| NAT Gateway | Cung cấp outbound Internet cho tài nguyên trong private subnet mà không nhận inbound trực tiếp |
| AWS KMS | Quản lý khóa mã hóa cho các tài nguyên AWS phù hợp |
| Security Groups | Giới hạn luồng ALB → EC2 → RDS và kết nối quản trị |

#### *AWS WAF*

Web ACL gắn với CloudFront sử dụng AWS Managed Rule Group `AWS-AWSManagedRulesCommonRuleSet`, tức Core Rule Set có capacity **700 WCU**. Bộ rule bao phủ các nhóm request phổ biến như User-Agent thiếu hoặc bất thường, bad bots cơ bản, SSRF hướng đến EC2 metadata, LFI/RFI, restricted extensions, XSS và giới hạn kích thước request. Một số rule đang ở chế độ **Block**; các rule như SizeRestrictions và CrossSiteScripting được để **Count** nhằm theo dõi sampled requests trước khi quyết định block. Kiến trúc hiện không tuyên bố sử dụng Bot Control, Fraud Control hoặc paid Marketplace rule groups.

#### *Thiết kế mạng và bảo mật*

Kiến trúc triển khai đặt VPC tại Region Singapore (`ap-southeast-1`) và trải trên hai Availability Zone. Mỗi AZ có một public subnet; ALB internet-facing liên kết với cả hai public subnet. Hai EC2 do ASG quản lý được bố trí trong hai private subnet. DB subnet group của RDS cũng bao phủ hai subnet này, còn RDS MySQL được triển khai Single-AZ tại một Availability Zone. NAT Gateway nằm trong một public subnet; Internet Gateway được gắn với VPC chứ không thuộc riêng một subnet hay Availability Zone.

Luồng inbound và outbound được tách rõ:

```text
User/CloudFront
  → ALB Security Group: TCP 80/443
  → Target Group
  → EC2 Security Group: TCP 8080 chỉ từ ALB Security Group
  → RDS Security Group: TCP 3306 chỉ từ EC2 Security Group

EC2 private outbound
  → private subnet route
  → NAT Gateway trong public subnet
  → Internet Gateway
  → Internet hoặc public service endpoint
```

NAT Gateway không nhận request từ người dùng và không nằm trong đường inbound của ứng dụng. Backend chỉ nhận traffic từ ALB Security Group trên port `8080`. Việc quản trị private instance cần một cơ chế truy cập được thiết kế riêng.

#### *High Availability và khả năng mở rộng*

ALB và ASG phân bố compute qua hai Availability Zone. Desired capacity bằng `2` duy trì hai EC2; khi một target không khỏe, ALB ngừng chuyển request đến target đó và ASG có thể tạo instance thay thế. Vì `Max = 2`, cấu hình ưu tiên phục hồi và availability của application tier hơn là mở rộng vượt hai instance. Một NAT Gateway vẫn là điểm phụ thuộc cho outbound, còn RDS Single-AZ là giới hạn availability lớn nhất, nên kiến trúc không được xem là HA end-to-end.
## 4. Phạm vi chức năng

### 4.1. Người dùng

- Đăng ký, xác minh/gửi lại email xác minh, đăng nhập và đăng xuất.
- Quên và đặt lại mật khẩu.
- Xem/cập nhật hồ sơ và số dư.
- Nạp tiền mô phỏng, tra cứu người nhận, chuyển tiền và thanh toán dịch vụ.
- Xem lịch sử giao dịch.

### 4.2. Quản trị viên

- Xem dashboard tổng quan.
- Xem và khóa/mở khóa người dùng.
- Xem giao dịch.
- Thêm, sửa, kích hoạt hoặc vô hiệu hóa dịch vụ.

## 5. Triển khai kỹ thuật

Dự án được thực hiện qua các giai đoạn phân tích yêu cầu và thiết kế; phát triển React, Spring Boot và MySQL ở local; Docker hóa backend; thiết lập VPC, subnet và Security Group; triển khai RDS, EC2, ASG, ALB, S3, CloudFront và WAF; tích hợp SES; sau đó kiểm tra health, nghiệp vụ, email và hoàn thiện tài liệu.

- **Frontend:** React 19, TypeScript và Vite; build thành static files trên S3 và phân phối qua CloudFront.
- **Backend:** Java 17, Spring Boot, Spring Security, JDBC và Actuator; chạy trong Docker trên EC2 port `8080`.
- **Database:** RDS MySQL Single-AZ trong private subnet; dữ liệu tiếng Việt dùng `utf8mb4`; nghiệp vụ số dư dùng database transaction và row-level locking.
- **Email:** SES SMTP tại `ap-southeast-1`, port `587`, authentication và STARTTLS.
- **Bảo mật:** BCrypt, JWT có thời hạn, role `user`/`admin`, secret nằm ngoài Git, HTTPS ở phía người dùng, WAF tại CloudFront và inbound theo Security Group.
- **Vận hành:** Target Group health check `/actuator/health`; ASG duy trì hai EC2; CloudWatch cung cấp metrics và health monitoring phù hợp.

## 6. Kế hoạch thực hiện

| Giai đoạn | Nội dung công việc chi tiết |
| --- | --- |
| Tuần 1–2 | Khảo sát yêu cầu, tìm hiểu AWS, thiết kế database và kiến trúc tổng thể. |
| Tuần 3–5 | Phát triển xác thực, phân quyền, hồ sơ, ví, giao dịch và dịch vụ. |
| Tuần 6 | Hoàn thiện giao diện quản trị và kiểm thử local. |
| Tuần 7 | Docker hóa backend; chuẩn bị VPC, subnet, Security Group và RDS. |
| Tuần 8 | Triển khai S3, CloudFront, WAF, ALB, Target Group, EC2 và ASG. |
| Tuần 9 | Tích hợp SES, kiểm thử production và hoàn thiện báo cáo. |

## 7. Ước tính ngân sách

Chi phí dưới đây là dự toán cho Region Singapore (`ap-southeast-1`), giá On-Demand và 730 giờ/tháng. Đây không phải hóa đơn thực tế. Tên miền `cloud-ewallet.com` có giá **10,98 USD** và là khoản thanh toán một lần, không lặp lại trong chi phí duy trì.

### Giả định sử dụng

Cả ba mức đều giữ đúng kiến trúc `Desired = 2`: hai EC2 `t3.micro`, hai EBS gp3 8 GB, một RDS `db.t4g.micro` Single-AZ 20 GB và một ALB. Mức sử dụng khác nhau ở ALB LCU, S3, CloudFront, SES, CloudWatch và request WAF.

| Hạng mục | Tối thiểu | Trung bình | Tối đa giả định |
| --- | --- | --- | --- |
| EC2 và EBS | 2 `t3.micro`; 2 × 8 GB gp3 | Giống mức tối thiểu | Giống mức tối thiểu |
| RDS MySQL | 1 `db.t4g.micro` Single-AZ, 20 GB | Giống mức tối thiểu | Giống mức tối thiểu |
| ALB | 0,1 LCU trung bình | 0,3 LCU trung bình | 1 LCU trung bình |
| S3 | 1 GB, ít request | 5 GB, 30.000 GET và 3.000 PUT | 20 GB, 100.000 GET và 10.000 PUT |
| CloudFront | 5 GB, 50.000 request | 30 GB, 250.000 request | 100 GB, 1.000.000 request |
| SES | 1.000 email | 3.000 email | 10.000 email |
| CloudWatch | Metrics cơ bản | 1 GB log ingest giả định | 5 GB log ingest giả định |
| AWS WAF | 1 Web ACL, 1 managed rule group, request trong ngưỡng dự toán | Giống mức tối thiểu | Giống mức tối thiểu trong cận trên 10 triệu request |
| NAT Gateway | 1 gateway, 1 GB xử lý | 1 gateway, 5 GB xử lý | 1 gateway, 20 GB xử lý |

### Chi phí duy trì hằng tháng

| Dịch vụ | Tối thiểu (USD) | Trung bình (USD) | Tối đa giả định (USD) |
| --- | ---: | ---: | ---: |
| 2 EC2 `t3.micro` | 19,28 | 19,28 | 19,28 |
| 2 EBS gp3, mỗi volume 8 GB | 1,60 | 1,60 | 1,60 |
| RDS MySQL `db.t4g.micro` + 20 GB | 21,74 | 21,74 | 21,74 |
| Application Load Balancer + LCU | 18,98 | 20,15 | 24,24 |
| AWS WAF | 12,00 | 12,00 | 12,00 |
| NAT Gateway (730 giờ + data processing) | 43,13 | 43,37 | 44,25 |
| S3 storage và request | 0,03 | 0,15 | 0,70 |
| CloudFront transfer và request | 0,61 | 3,65 | 12,10 |
| Amazon SES | 0,16 | 0,48 | 1,60 |
| CloudWatch | 0,00 | 0,50 | 2,50 |
| **Tổng duy trì ước tính/tháng** | **117,53** | **122,92** | **140,01** |
| **Tổng tháng đầu gồm tên miền** | **128,51** | **133,90** | **150,99** |

Dự toán WAF khoảng **12 USD/tháng** gồm 1 Web ACL (5 USD), 1 AWS Managed Rule Group (1 USD) và tối đa 10 triệu request trong giả định (6 USD). `AWSManagedRulesCommonRuleSet` không có subscription fee bổ sung như Bot Control hoặc Marketplace rules. Con số này không cố định: rule bổ sung, request vượt giả định, logging, CAPTCHA, Bot Control hoặc paid managed rule groups có thể làm chi phí tăng. ASG không có phí quản lý riêng; chi phí của nó được thể hiện qua hai EC2 và hai EBS. NAT Gateway được ước tính theo giả định Singapore là 0,059 USD/giờ và 0,059 USD/GB xử lý: mức tối thiểu `730 × 0,059 + 1 × 0,059`, mức trung bình dùng 5 GB và mức tối đa giả định dùng 20 GB. Giá cần được đối chiếu lại với AWS Pricing Calculator tại thời điểm triển khai.

- **Tối thiểu – 117,53 USD/tháng:** hai backend EC2 luôn được duy trì, lưu lượng demo thấp, CloudWatch chỉ dùng metrics cơ bản và WAF nằm trong giả định.
- **Trung bình – 122,92 USD/tháng:** cùng cấu hình compute/database, nhưng LCU, transfer, request, email và dữ liệu theo dõi tăng ở mức nhóm sử dụng thường xuyên.
- **Tối đa giả định – 140,01 USD/tháng:** vẫn giới hạn hai EC2 nhưng tăng LCU, S3, CloudFront, SES và CloudWatch trong phạm vi bảng.

Dự toán chưa gồm thuế, Free Tier, snapshot, backup phát sinh, data transfer ngoài giả định và tài nguyên không có trong bảng. Giá thực tế thay đổi theo thời điểm, Region và mức sử dụng.

## 8. Đánh giá rủi ro và chiến lược giảm thiểu

| Rủi ro | Chiến lược giảm thiểu |
| --- | --- |
| Một EC2 hoặc một AZ gặp lỗi | ALB chỉ chuyển request đến target healthy; ASG duy trì desired capacity và tạo instance thay thế. |
| Instance bootstrap thất bại | Chuẩn hóa Launch Template, User Data và cấu hình Docker restart; dùng ALB health check để không đưa target lỗi vào phục vụ. |
| Scale-in hoặc thay instance làm gián đoạn request | Sử dụng ALB deregistration delay/connection draining trước khi target bị loại. |
| RDS Single-AZ là điểm lỗi đơn tại tầng dữ liệu | Duy trì backup, snapshot và kiểm tra khả năng khôi phục dữ liệu định kỳ. |
| WAF tạo false positive | Đặt các rule nhạy cảm ở Count trước, theo dõi sampled requests rồi mới quyết định chuyển sang Block. |
| Chi phí WAF tăng | Theo dõi số request, rule, logging và các tính năng trả phí được bật. |
| NAT Gateway hoặc route outbound gặp lỗi | Kiểm tra route của private subnet, trạng thái NAT Gateway và Internet Gateway; tránh đưa NAT vào inbound request path. |
| Một NAT Gateway tạo điểm phụ thuộc cho outbound của hai AZ | Theo dõi trạng thái NAT Gateway, route table và cảnh báo kết nối để phát hiện sự cố sớm. |
| EC2 mới thiếu secret hoặc biến môi trường | Chuẩn hóa Launch Template/User Data và kiểm tra cấu hình runtime trước khi target được đưa vào phục vụ. |
| Chi phí compute ngoài dự kiến | Giới hạn ASG `Max = 2` và theo dõi AWS Billing/Cost Explorer. |
| Sai lệch số dư khi giao dịch đồng thời | Dùng database transaction, ACID và row-level locking. |
| SES bounce, complaint hoặc vượt quota | Theo dõi sending statistics, bounce/complaint và giới hạn gửi của SES. |

## 9. Kết quả đạt được

- Frontend tĩnh được phân phối qua S3 và CloudFront bằng HTTPS.
- WAF kiểm tra request tại edge bằng Core Rule Set với sự kết hợp giữa Block và Count.
- ALB, Target Group và ASG cải thiện availability của backend; hai EC2 hoạt động tại hai Availability Zone.
- Thiết kế Security Group giới hạn luồng CloudFront/Internet → ALB → EC2:8080 → RDS:3306; EC2 backend không nhận traffic ứng dụng trực tiếp từ Internet.
- RDS MySQL cung cấp dữ liệu nhất quán nhưng Single-AZ vẫn là giới hạn availability hiện tại.
- SES hỗ trợ các email giao dịch theo từng người dùng; CloudWatch hỗ trợ theo dõi metrics và tình trạng hệ thống.
- Kiến trúc đạt sự cân bằng phù hợp với phạm vi dự án giữa security, availability và cost optimization, nhưng không được xem là HA end-to-end.
