---
title: "Bản đề xuất"
date: 2024-01-01
weight: 2
chapter: false
pre: " <b> 2. </b> "
---


# Cloud E-Wallet  
## Hệ thống ví điện tử ứng dụng kiến trúc điện toán đám mây AWS


### 1. Giới thiệu tổng quan

Nhóm đề xuất dự án **Cloud E-Wallet**, một ứng dụng web mô phỏng ví điện tử cho phép người dùng quản lý tài khoản, nạp tiền, rút tiền, chuyển tiền và thanh toán các dịch vụ trực tuyến. Bên cạnh việc xây dựng các chức năng giao dịch cốt lõi, dự án còn tập trung vào việc triển khai và vận hành toàn bộ hệ thống trên hạ tầng điện toán đám mây AWS. Mục tiêu của dự án là mang đến một môi trường thực hành toàn diện, giúp nhóm nắm bắt quy trình vận hành phần mềm thực tế. Vì đây là dự án thuần túy mang tính giáo dục, mọi giao dịch đều là giả lập và hoàn toàn độc lập với các hệ thống ngân hàng thực tế.

### 2. Tuyên bố vấn đề
#### *Vấn đề hiện tại*
Trong cuộc sống hàng ngày, các giao dịch tài chính truyền thống bằng tiền mặt thường mang lại nhiều bất tiện như mất thời gian chờ đợi, rủi ro rơi rớt, nhầm lẫn khi thối tiền lẻ và đặc biệt là khó khăn trong việc theo dõi chi tiêu một cách có hệ thống. Bên cạnh đó, việc thanh toán các dịch vụ tiện ích như điện, nước hay cước viễn thông theo phương thức truyền thống đòi hỏi người dùng phải đến các điểm thu hộ, gây tiêu tốn thời gian và công sức.
Bên cạnh đó, việc triển khai một ứng dụng ví điện tử đòi hỏi tính bảo mật cao, dữ liệu giao dịch phải nhất quán và hệ thống cần có khả năng mở rộng. Nếu chỉ phát triển và chạy thử nghiệm trên máy cá nhân (localhost), nhóm sẽ khó đánh giá được hiệu năng thực tế, thiếu môi trường để cấu hình tên miền (domain), phân tách luồng mạng hay thiết lập bảo mật HTTPS. Điều này đặt ra yêu cầu phải có một giải pháp triển khai đám mây toàn diện để giải quyết triệt để các vấn đề trên.

#### *Giải pháp*

Để giải quyết những bất cập của thanh toán truyền thống, nền tảng **Cloud E-Wallet** sử dụng các dịch vụ AWS để triển khai hệ thống: **Amazon EC2** và **Application Load Balancer (ALB)** đóng vai trò xử lý các giao dịch một cách mượt mà; **Amazon RDS** (MySQL) được sử dụng để lưu trữ dữ liệu an toàn, áp dụng database transaction nhằm đảm bảo tính toàn vẹn của số dư; **Amazon S3** và **CloudFront** cung cấp giao diện người dùng tốc độ cao. Cuối cùng, hệ thống tích hợp **Amazon SES** cho quy trình gửi email tự động xác thực tài khoản, mang đến trải nghiệm liền mạch và bảo mật như các ứng dụng tài chính thực tế.

### 3. Kiến trúc giải pháp

#### *Sơ đồ*

![Kiến trúc triển khai Cloud E-Wallet trên AWS](/images/5-Workshop/5.1-Prerequisites/architecture.png)
<p align="center"><i>Sơ đồ triển khai</i></p>

Mô tả luồng:
1. **Truy cập:** Người dùng truy cập qua tên miền do **Cloudflare DNS** quản lý, sau đó được điều hướng đến **Amazon CloudFront**.
2. **Định tuyến:** CloudFront chuyển yêu cầu tải giao diện tĩnh đến **Amazon S3**, và chuyển các yêu cầu gọi API đến bộ cân bằng tải **ALB**.
3. **Xử lý logic:** ALB phân phối đều yêu cầu API cho máy chủ **Amazon EC2** để xử lý các giao dịch ví điện tử.
4. **Dữ liệu & Giao tiếp:** EC2 lưu trữ dữ liệu vào cơ sở dữ liệu **Amazon RDS** và dùng **Amazon SES** để tự động gửi email.
5. **Giám sát:** Hoạt động của hệ thống được theo dõi thông qua **Amazon CloudWatch**.

#### *Dịch vụ sử dụng*

| Thành phần | Vai trò |
| --- | --- |
| Cloudflare DNS | Quản lý `cloud-ewallet.com` và record xác minh sender domain |
| CloudFront | Nhận HTTPS từ trình duyệt; định tuyến frontend và `/api/*` |
| S3 | Lưu static build React |
| ALB | Chuyển tiếp API, thực hiện health check backend |
| EC2 | Chạy Spring Boot trong Docker |
| RDS MySQL | Lưu dữ liệu trong private subnet |
| Amazon SES SMTP | Gửi email xác minh và đặt lại mật khẩu; dùng SMTP `587`, xác thực và STARTTLS |
| CloudWatch | Theo dõi metrics của các dịch vụ AWS |

### 3.3. Thiết kế thành phần

- **Phân giải tên miền:** Cloudflare DNS chịu trách nhiệm quản lý tên miền `cloud-ewallet.com` và lưu trữ các bản ghi để xác minh email.
- **Giao diện Web:** Amazon S3 lưu trữ các tệp tĩnh của ứng dụng React, kết hợp với mạng phân phối nội dung Amazon CloudFront để tăng tốc độ truy cập và cung cấp kết nối an toàn qua HTTPS.
- **Định tuyến & Cân bằng tải:** Application Load Balancer (ALB) nhận các yêu cầu API, thực hiện kiểm tra tình trạng (health check) và phân bổ tải một cách an toàn tới các máy chủ backend.
- **Xử lý nghiệp vụ:** Amazon EC2 đóng vai trò là máy chủ tính toán, chạy các container Spring Boot (Docker) để xử lý toàn bộ logic giao dịch của ví điện tử.
- **Lưu trữ dữ liệu:** Amazon RDS (MySQL) được triển khai độc lập trong mạng nội bộ (private subnet) để bảo vệ tuyệt đối thông tin người dùng, số dư và lịch sử giao dịch.
- **Giao tiếp người dùng:** Amazon SES SMTP đảm nhận việc gửi email xác minh và khôi phục mật khẩu thông qua kết nối STARTTLS một cách tự động.
- **Giám sát hệ thống:** Amazon CloudWatch được sử dụng để theo dõi metrics hiệu năng và ghi nhận tình trạng hoạt động thực tế của các dịch vụ AWS.

## 4. Phạm vi chức năng

### 4.1. Người dùng

- Đăng ký, xác minh/gửi lại email xác minh, đăng nhập và đăng xuất.
- Quên và đặt lại mật khẩu.
- Xem/cập nhật hồ sơ và số dư.
- Nạp tiền mô phỏng, tra cứu người nhận, chuyển tiền và thanh toán dịch vụ.
- Xem lịch sử giao dịch.

### 4.2. Quản trị viên

- Dashboard tổng quan.
- Xem và khóa/mở khóa người dùng.
- Xem giao dịch.
- Thêm, sửa, kích hoạt hoặc vô hiệu hóa dịch vụ.


## 5. Triển khai kĩ thuật

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

## 6. Kế hoạch thực hiện

| Giai đoạn | Nội dung công việc chi tiết |
| --- | --- |
| Tuần 1 | Khảo sát yêu cầu, thiết kế kiến trúc tổng thể AWS, sơ đồ cơ sở dữ liệu và chuẩn bị kho lưu trữ mã nguồn. |
| Tuần 2 | Khởi tạo dự án, cấu hình môi trường phát triển cục bộ, thiết lập các API cơ bản và cấu trúc thư mục frontend. |
| Tuần 3 | Lập trình các module cốt lõi: Đăng ký, đăng nhập (JWT), xác thực người dùng và phân quyền (Admin/User). |
| Tuần 4 | Lập trình nghiệp vụ ví điện tử (1): Tích hợp tính năng nạp tiền, theo dõi số dư và quản lý thông tin tài khoản. |
| Tuần 5 | Lập trình nghiệp vụ ví điện tử (2): Tính năng chuyển khoản, thanh toán dịch vụ và ghi nhận lịch sử giao dịch. |
| Tuần 6 | Xây dựng giao diện trang quản trị viên (Admin Dashboard), kiểm thử (Unit test/Integration test) toàn bộ hệ thống cục bộ. |
| Tuần 7 | Đóng gói ứng dụng (Docker hóa Spring Boot) và cấu hình hạ tầng mạng AWS cơ sở (VPC, Security Groups, EC2, RDS). |
| Tuần 8 | Triển khai các dịch vụ AWS phụ trợ: Thiết lập S3, CloudFront cho frontend và cấu hình Application Load Balancer (ALB). |
| Tuần 9 | Cấu hình tên miền với Cloudflare DNS, tích hợp tính năng gửi email xác thực bằng Amazon SES, và kiểm thử production. |
| Tuần 10 | Đánh giá tổng kết, tối ưu hóa hiệu năng, xử lý lỗi tồn đọng, hoàn thiện báo cáo và tài liệu hướng dẫn dự án. |

## 7. Ước tính ngân sách

Chi phí dưới đây là **ước tính**, không phải hóa đơn thực tế. Nhóm chúng em giả định tài nguyên đặt tại Region Singapore (`ap-southeast-1`), sử dụng giá On-Demand, chạy 730 giờ/tháng và chưa gồm thuế hoặc Free Tier. Mức tối đa chỉ là cận trên trong phạm vi giả định của báo cáo; AWS không tự giới hạn chi phí nếu lưu lượng hoặc tài nguyên tiếp tục tăng.

### Chi phí ban đầu

| Khoản chi | Chi phí |
| --- | ---: |
| Mua tên miền `cloud-ewallet.com` qua Cloudflare | **10,98 USD** |
| Phí khởi tạo dịch vụ AWS | **0,00 USD** |
| **Tổng chi phí ban đầu, thanh toán một lần** | **10,98 USD** |

Tên miền được ghi nhận là khoản mua ban đầu theo số tiền nhóm đã thanh toán và **không được phân bổ vào chi phí duy trì hằng tháng** trong bảng dưới đây.

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

Giá AWS thay đổi theo thời điểm, Region, loại tài khoản và mức sử dụng thực tế. Tham khảo: [AWS EC2 Pricing](https://aws.amazon.com/ec2/pricing/on-demand/), [Amazon RDS for MySQL Pricing](https://aws.amazon.com/rds/mysql/pricing/), [Elastic Load Balancing Pricing](https://aws.amazon.com/elasticloadbalancing/pricing/), [Amazon CloudFront Pricing](https://aws.amazon.com/cloudfront/pricing/), [Amazon S3 Pricing](https://aws.amazon.com/s3/pricing/) và [Amazon SES Pricing](https://aws.amazon.com/ses/pricing/).

## 8. Đánh giá rủi ro và chiến lược giảm thiểu

| Rủi ro | Mức độ ảnh hưởng | Khả năng xảy ra | Chiến lược giảm thiểu |
| --- | --- | --- | --- |
| **Lộ lọt dữ liệu nhạy cảm (Credentials)** | Rất Cao | Thấp | - Loại bỏ toàn bộ thông tin nhạy cảm (mật khẩu DB, JWT secret, AWS keys) ra khỏi mã nguồn.<br>- Sử dụng file `.env` cục bộ và tính năng quản lý biến môi trường của nền tảng khi triển khai lên mây. |
| **Sai lệch dữ liệu giao dịch & số dư** | Rất Cao | Trung bình | - Áp dụng **Database Transaction** (ACID) cho mọi nghiệp vụ nạp/chuyển tiền.<br>- Sử dụng cơ chế khóa dòng (Row-level locking) trong MySQL để tránh lỗi ghi đè khi có nhiều giao dịch đồng thời (Race condition). |
| **Gián đoạn dịch vụ Backend (Downtime)** | Cao | Trung bình | - Thiết lập cơ chế kiểm tra sức khỏe liên tục (**Health check**) thông qua ALB để loại bỏ các node lỗi.<br>- Cấu hình Docker tự động khởi động lại container khi có sự cố crash ứng dụng. |
| **Phát sinh chi phí AWS ngoài kiểm soát** | Trung bình | Trung bình | - Thường xuyên theo dõi dashboard **AWS Billing & Cost Explorer**.<br>- Chủ động xóa (cleanup) tài nguyên không sử dụng sau khi kết thúc dự án. |
| **Gián đoạn tính năng gửi Email (SES)** | Trung bình | Cao | - Hoàn tất cấu hình các bản ghi DNS (DKIM, SPF) trên Cloudflare để tránh việc email bị đánh dấu là spam.<br>- Theo dõi bounce, complaint và hạn mức gửi trong Amazon SES. |

## 9. Kết quả đạt được

Dự án Cloud E-Wallet mang lại các giá trị chức năng cụ thể:

- **Đối với người dùng cuối:** Mang đến một nền tảng ví điện tử trực tuyến tiện lợi, an toàn. Người dùng có thể dễ dàng nạp tiền (mô phỏng), thực hiện các giao dịch chuyển khoản nội bộ nhanh chóng và thanh toán các hóa đơn dịch vụ (điện, nước, internet,...) chỉ với vài thao tác. Tất cả lịch sử chi tiêu đều được lưu trữ và thống kê minh bạch giúp quản lý tài chính cá nhân hiệu quả hơn.
- **Đối với quản trị viên:** Cung cấp một bảng điều khiển trung tâm (Admin Dashboard) trực quan, cho phép quản lý chặt chẽ tài khoản người dùng, theo dõi toàn cảnh dòng tiền mô phỏng trong hệ thống, cũng như giám sát và cấu hình linh hoạt danh mục các dịch vụ thanh toán.
- **Về mặt hệ thống:** Một nền tảng được triển khai trên AWS với HTTPS, phân tách mạng và kiểm soát truy cập bằng Security Group. Trải nghiệm người dùng được tối ưu hóa với tốc độ tải trang nhanh, xác thực an toàn bằng mã hóa và các quy trình tự động hóa vận hành mượt mà.


