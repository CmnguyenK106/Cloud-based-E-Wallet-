# PROJECT_STATUS.md

## 1. Tổng quan dự án

| Mục | Thông tin hiện tại |
| --- | --- |
| Tên / loại dự án | Cloud-based E-wallet, một hệ thống ví điện tử mô phỏng chạy dạng web application |
| Mục đích chính | Cho phép người dùng đăng ký bằng số điện thoại, đăng nhập, có ví riêng, xem số dư, chuyển tiền cho ví khác, và chuẩn bị nền tảng cho thanh toán dịch vụ / lịch sử giao dịch |
| Frontend | React 19, TypeScript, Vite, React Router, Axios, Zustand, Zod |
| Backend | Java 17, Spring Boot 4.1.0, Spring Web MVC, JDBC qua `JdbcTemplate`, MySQL Driver, BCrypt password encoder |
| Database | MySQL 8.0, chạy bằng Docker, schema nằm trong `database/schema.sql` |
| Trạng thái hiện tại | Đã có đăng ký, đăng nhập, lấy thông tin ví, chuyển tiền thật qua backend/database, lấy lịch sử giao dịch qua API. UI dịch vụ còn mock, chưa có API thanh toán dịch vụ, chưa có nạp tiền, chưa có admin dashboard. |

Dự án hiện tại đã đi qua giai đoạn khởi tạo và có một luồng user cơ bản hoạt động với database thật. Tuy nhiên, kiến trúc backend hiện tại còn đơn giản: controller thao tác trực tiếp bằng `JdbcTemplate`, chưa có entity, repository, service layer hoặc security filter/JWT thật.

## 2. Cấu trúc thư mục

| Đường dẫn | Vai trò |
| --- | --- |
| `backend/` | Ứng dụng Spring Boot backend, chứa Maven wrapper, `pom.xml`, controller REST API và cấu hình kết nối database |
| `backend/src/main/java/com/khoi/ewallet/` | Package root của backend |
| `backend/src/main/java/com/khoi/ewallet/controller/` | Chứa các controller hiện có: auth, wallet user, API test database |
| `backend/src/main/resources/application.properties` | Cấu hình server port, datasource MySQL, tên app |
| `backend/src/test/` | Test mặc định `contextLoads` của Spring Boot |
| `frontend/` | Ứng dụng React TypeScript dùng Vite |
| `frontend/src/apis/` | Axios client và các hàm gọi API auth/wallet |
| `frontend/src/pages/` | Các page chính: Home, Login, Register, Dashboard |
| `frontend/src/store/` | Zustand auth store, lưu token/user/wallet vào `localStorage` |
| `frontend/src/schema/` | Zod schema validate form login/register |
| `frontend/src/App.tsx` | Routing, layout header, protected route, wallet nav tabs |
| `database/schema.sql` | Tạo database, bảng, ràng buộc, index và dữ liệu seed |
| `docker-compose.yml` | Chạy MySQL 8.0 container và mount schema init |
| `start-dev.ps1` | Script PowerShell để start MySQL, backend, frontend |
| `description.md` | Tài liệu yêu cầu/định hướng ban đầu của dự án. File này trong terminal hiện bị lỗi encoding hiển thị, nhưng nội dung là mô tả yêu cầu e-wallet. |

## 3. Backend hiện tại

### 3.1 Công nghệ và cấu hình

| Mục | Giá trị |
| --- | --- |
| Spring Boot | `4.1.0` trong `backend/pom.xml` |
| Java | `17` |
| Package root | `com.khoi.ewallet` |
| Server port | `8080` |
| Database URL | `jdbc:mysql://localhost:3307/ewallet_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh` |
| DB user/password | `ewallet_user` / `ewallet_pass` |
| SQL init | `spring.sql.init.mode=never`, database được init bởi Docker mount schema |
| CORS | Mỗi controller dùng `@CrossOrigin(origins = "http://localhost:5173")` |
| Auth hiện tại | Demo token dạng `demo-token-{userId}`. API protected yêu cầu header `Authorization: Bearer demo-token-{userId}`. Chưa có JWT thật, chưa có Spring Security filter chain. |

Dependencies quan trọng trong `pom.xml`:

| Dependency | Vai trò |
| --- | --- |
| `spring-boot-starter-webmvc` | REST controller / web API |
| `spring-boot-starter-batch-jdbc` | Kéo JDBC infrastructure; code thực tế dùng `JdbcTemplate` |
| `mysql-connector-j` | Kết nối MySQL |
| `spring-security-crypto` | Dùng `BCryptPasswordEncoder` để hash/check password |
| `spring-boot-devtools` | Hỗ trợ dev local |

### 3.2 Controller: `AuthController`

File: `backend/src/main/java/com/khoi/ewallet/controller/AuthController.java`

| Method | Endpoint | Purpose | Request body | Response | Status |
| --- | --- | --- | --- | --- | --- |
| `POST` | `/api/auth/register` | Đăng ký user, hash password, tạo `users`, `user_profiles`, `wallets` với số dư `10.00` | `{ "phone": "...", "password": "...", "fullName": "..." }` | `message`, `token`, `user`, `wallet` | Đã có, dùng DB thật |
| `POST` | `/api/auth/login` | Đăng nhập bằng phone/password, check BCrypt, check status blocked | `{ "phone": "...", "password": "..." }` | `message`, `token`, `user`, `wallet` | Đã có, dùng DB thật |

Hành vi đáng chú ý:

- Validate phone bằng regex `^0[0-9]{9}$`.
- Password đăng ký tối thiểu 6 ký tự.
- Register dùng `@Transactional`.
- Register trả token nhưng frontend hiện không tự login sau register, mà chuyển về trang login.
- Login với admin có thể trả `wallet = null` vì admin không có ví.
- Token chỉ là chuỗi demo, không ký số, không hết hạn.

### 3.3 Controller: `UserWalletController`

File: `backend/src/main/java/com/khoi/ewallet/controller/UserWalletController.java`

| Method | Endpoint | Purpose | Request body | Response | Status |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/api/user/wallet/me` | Lấy thông tin user và ví hiện tại từ demo token | Không có | `{ user, wallet }` | Đã có, dùng DB thật |
| `POST` | `/api/user/wallet/transfer` | Chuyển tiền từ ví user hiện tại sang user khác bằng số điện thoại | `{ "receiverPhone": "...", "amount": 10, "description": "..." }` | `message`, `balance`, `transaction` | Đã có, dùng DB thật |
| `GET` | `/api/user/wallet/transactions` | Lấy lịch sử giao dịch liên quan đến ví hiện tại | Không có | `{ transactions: [...] }` | Đã có backend API, frontend chưa hiển thị |

Hành vi chuyển tiền:

- Xác thực bằng demo token trong header.
- Check receiver phone đúng regex.
- Check amount `> 0` và `<= 10000000`.
- Không cho chuyển cho chính mình.
- Lock ví người gửi bằng `SELECT ... FOR UPDATE`.
- Trừ số dư ví gửi, cộng số dư ví nhận, tạo transaction type `transfer`.
- Chỉ lưu `balance_before` / `balance_after` theo phía người gửi.

### 3.4 Controller: `TestDbController`

File: `backend/src/main/java/com/khoi/ewallet/controller/TestDbController.java`

| Method | Endpoint | Purpose | Request body | Response | Status |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/api/test/ping` | Kiểm tra backend còn chạy | Không có | `message`, `status` | Đã có |
| `GET` | `/api/test/db` | Kiểm tra kết nối DB, đếm users/wallets/transactions | Không có | `message`, counts | Đã có |
| `GET` | `/api/test/users` | Lấy danh sách user kèm ví | Không có | Array rows | Đã có, endpoint test không auth |
| `GET` | `/api/test/transactions` | Lấy 10 transaction mới nhất | Không có | Array rows | Đã có, endpoint test không auth |
| `GET` | `/api/test/transactions/{phone}` | Lấy transaction theo phone | Không có | Array rows | Đã có, endpoint test không auth |

### 3.5 API chưa có ở backend

| Nhóm chức năng | Trạng thái |
| --- | --- |
| Deposit / nạp tiền mô phỏng | Chưa có endpoint |
| Payment service / thanh toán dịch vụ | Chưa có endpoint |
| List services cho frontend | Chưa có endpoint public/user |
| Admin APIs | Chưa có endpoint `/api/admin/**` |
| JWT / refresh token / role guard | Chưa có |

## 4. Frontend hiện tại

### 4.1 Framework, tooling, dependencies

| Mục | Thông tin |
| --- | --- |
| Framework | React `^19.2.7` |
| Build tool | Vite `^8.1.0` |
| Language | TypeScript `~6.0.2` |
| Routing | `react-router-dom` `^7.18.0` |
| API client | Axios `^1.18.1` |
| State | Zustand `^5.0.14` |
| Validation | Zod `^4.4.3` |

Scripts trong `frontend/package.json`:

| Script | Lệnh |
| --- | --- |
| `dev` | `vite --open` |
| `build` | `tsc -b && vite build` |
| `lint` | `eslint .` |
| `preview` | `vite preview` |

### 4.2 Routing

File: `frontend/src/App.tsx`

| Route | Component | Bảo vệ | Ghi chú |
| --- | --- | --- | --- |
| `/` | `HomePage` | Không | Landing/home UI |
| `/login` | `LoginPage` | Không | Gọi API login thật |
| `/register` | `RegisterPage` | Không | Gọi API register thật |
| `/dashboard` | `DashboardPage` | Có `ProtectedRoute` dựa trên token trong Zustand/localStorage | Chứa các tab Wallet Info, Transfer Money, Services |

### 4.3 API files

| File | Mục đích | API gọi |
| --- | --- | --- |
| `frontend/src/apis/axiosClient.ts` | Tạo Axios instance hardcode base URL `http://localhost:8080/api` | Không |
| `frontend/src/apis/authApi.ts` | Định nghĩa type và hàm auth | `POST /auth/register`, `POST /auth/login` |
| `frontend/src/apis/walletApi.ts` | Định nghĩa type và hàm wallet | `GET /user/wallet/me`, `POST /user/wallet/transfer`, `GET /user/wallet/transactions` |

### 4.4 Store auth

File: `frontend/src/store/authStore.ts`

| State / action | Vai trò |
| --- | --- |
| `token` | Lấy từ `localStorage.token`, dùng để bảo vệ route |
| `user` | Lấy từ `localStorage.user` |
| `wallet` | Lấy từ `localStorage.wallet` |
| `setAuth(token, user, wallet?)` | Lưu token/user/wallet vào localStorage và Zustand |
| `setWalletData(user, wallet)` | Cập nhật user/wallet sau khi gọi `/wallet/me` |
| `logout()` | Xóa token/user/wallet khỏi localStorage, reset store |

### 4.5 Pages/components

| File path | Purpose | State/data quan trọng | APIs called |
| --- | --- | --- | --- |
| `frontend/src/pages/HomePage.tsx` | Trang giới thiệu e-wallet với CTA register/login và feature cards | Không có state | Không gọi API |
| `frontend/src/pages/LoginPage.tsx` | Form login | `form`, `errors`, `message`, `isLoading`; dùng `loginSchema`; gọi `setAuth` khi thành công | `authApi.login` |
| `frontend/src/pages/RegisterPage.tsx` | Form register | `form`, `errors`, `message`, `isSuccess`, `isLoading`; dùng `registerSchema` | `authApi.register` |
| `frontend/src/pages/DashboardPage.tsx` | Dashboard với 3 tab wallet/transfer/services | `transferForm`, message/loading states, `user/wallet` từ auth store | `walletApi.getMyWallet`, `walletApi.transferMoney` |
| `frontend/src/App.tsx` / `AppHeader` | Header, navigation, user dropdown, protected route | `activeTab`, dropdown state, token/user/wallet from store | `walletApi.getMyWallet` khi có token |

### 4.6 Luồng UI hiện tại

| Khu vực | Trạng thái |
| --- | --- |
| Login flow | Real API. Login thành công lưu token/user/wallet vào localStorage và vào dashboard. |
| Register flow | Real API. Register thành công tạo user/wallet trong DB, sau đó frontend chuyển sang `/login`. |
| Dashboard layout | Đã có hero, current balance card, wallet nav tabs. |
| Wallet Info tab | Real API-backed qua `getMyWallet`; hiển thị full name, phone, role, status, balance. |
| Transfer Money tab | Real API-backed qua `transferMoney`; sau transfer gọi lại `loadWallet()` để refresh balance. |
| Services tab | UI/mock. Danh sách services hardcoded trong `DashboardPage.tsx`; nút Pay chỉ hiện message "will be connected later". |
| User dropdown | Đã có; hiển thị user info và balance; logout xóa localStorage. |
| Current balance display | Dựa trên wallet trong store, được refresh từ backend khi header/dashboard load. |
| Transaction history UI | Chưa có page/component hiển thị, dù `walletApi.getMyTransactions` đã tồn tại. |

## 5. Database hiện tại

### 5.1 Database và bảng

Database name: `ewallet_db`

| Table | Purpose | Important columns | Notes |
| --- | --- | --- | --- |
| `users` | Tài khoản chung cho user/admin | `id`, `phone`, `password`, `role`, `status`, `created_at`, `updated_at` | `phone` unique, regex check `^0[0-9]{9}$`, role enum `user/admin`, status enum `active/blocked` |
| `user_profiles` | Hồ sơ riêng của user thường | `user_id`, `full_name`, `date_of_birth`, `address` | FK `user_id -> users.id`, cascade delete |
| `admin_profiles` | Hồ sơ admin | `user_id`, `full_name`, `position` | Admin không có wallet |
| `wallets` | Ví của user | `id`, `user_id`, `balance` | `user_id` unique, FK tới `user_profiles.user_id`, `balance >= 0`, default `10.00` |
| `services` | Dịch vụ ảo để thanh toán | `id`, `name`, `price`, `description`, `is_active` | Seed 5 service, nhưng backend chưa có API payment/service |
| `transactions` | Lịch sử giao dịch | `transaction_code`, `sender_wallet_id`, `receiver_wallet_id`, `service_id`, `amount`, `balance_before`, `balance_after`, `type`, `status`, `description`, `created_by`, `created_at` | Type enum `deposit/transfer/payment`, status enum `success/failed` |

### 5.2 Relationships

| Quan hệ | Mô tả |
| --- | --- |
| `user_profiles.user_id -> users.id` | Một user thường có một profile |
| `admin_profiles.user_id -> users.id` | Một admin có một admin profile |
| `wallets.user_id -> user_profiles.user_id` | Một user profile có đúng một ví |
| `transactions.sender_wallet_id -> wallets.id` | Ví gửi, nullable |
| `transactions.receiver_wallet_id -> wallets.id` | Ví nhận, nullable |
| `transactions.service_id -> services.id` | Dịch vụ thanh toán, nullable |
| `transactions.created_by -> users.id` | User tạo giao dịch, nullable |

### 5.3 Seed data

| Loại seed | Nội dung |
| --- | --- |
| Users | 5 user thường, 1 admin |
| Wallets | 5 ví tương ứng user id 1-5 |
| Services | 5 dịch vụ: phone top-up, điện, nước, internet, game |
| Transactions | Nhiều transaction mẫu loại `deposit`, `transfer`, `payment` từ ngày 2026-06-01 đến 2026-06-05 |
| Password hashing | Comment trong SQL ghi password được lưu bằng BCrypt |

### 5.4 Demo accounts

| Role | Phone | Password | Notes |
| --- | --- | --- | --- |
| User | `0912345678` | `123456` | Seed user id 1, wallet balance `105.00` |
| User | `0987654321` | `123456` | Seed user id 2, wallet balance `55.00` |
| User | `0901234567` | `123456` | Seed user id 3, wallet balance `105.00` |
| User | `0933333333` | `123456` | Seed user id 4, wallet balance `100.00` |
| User | `0977777777` | `123456` | Seed user id 5, wallet balance `40.00` |
| Admin | `0900000000` | `admin123` | Seed admin id 6, không có wallet |

## 6. Docker / Local run setup

### 6.1 Docker Compose

File: `docker-compose.yml`

| Mục | Giá trị |
| --- | --- |
| Service | `mysql` |
| Image | `mysql:8.0` |
| Container name | `ewallet_mysql` |
| Port mapping | Host `3307` -> Container `3306` |
| Root password | `root` |
| Database | `ewallet_db` |
| User/password | `ewallet_user` / `ewallet_pass` |
| Volume data | `mysql_data:/var/lib/mysql` |
| Init schema | `./database/schema.sql:/docker-entrypoint-initdb.d/schema.sql` |

### 6.2 Useful commands

Start database:

```powershell
docker compose up -d
```

Stop and remove database volume:

```powershell
docker compose down -v
```

Start backend:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Start frontend:

```powershell
cd frontend
npm install
npm run dev
```

Run all with script:

```powershell
.\start-dev.ps1
```

`start-dev.ps1` sẽ:

- Chạy `docker compose up -d`.
- Chờ 5 giây.
- Mở PowerShell mới chạy backend bằng `cmd /c mvnw.cmd spring-boot:run`.
- Chờ 5 giây.
- Mở PowerShell mới chạy frontend bằng `npm run dev -- --open`.
- In backend `http://localhost:8080` và frontend `http://localhost:5173`.

## 7. Chức năng đã làm được

| Feature | Frontend | Backend | Database | Status | Notes |
| --- | --- | --- | --- | --- | --- |
| Register account | Có form, Zod validation, gọi API | `POST /api/auth/register` | Insert `users`, `user_profiles`, `wallets` | Working real backend/database | Frontend chuyển về login sau register |
| Login account | Có form, lưu Zustand/localStorage | `POST /api/auth/login` | Check user/password/status | Working real backend/database | Dùng demo token |
| Logout | Có user dropdown logout | Không cần backend | Xóa localStorage frontend | Working frontend-only | Không revoke token vì token demo |
| Protected dashboard | Có `ProtectedRoute` theo token | Không liên quan | Không liên quan | Working frontend-only | Chỉ check token tồn tại |
| Display dashboard | Có UI dashboard | Không trực tiếp | Không trực tiếp | Working UI | Dashboard load wallet từ backend |
| User dropdown | Có UI và logout | Không trực tiếp | Không trực tiếp | Working UI | Balance lấy từ store/API |
| Wallet info | Có tab Wallet Info | `GET /api/user/wallet/me` | Read `users`, `user_profiles`, `wallets` | Working real backend/database | Refresh khi vào dashboard/header |
| Current balance display | Có ở hero, dropdown, wallet tab | API wallet/me và transfer response | Read/update `wallets.balance` | Working real backend/database | Sau transfer có refresh |
| Transfer money | Có form | `POST /api/user/wallet/transfer` | Update 2 wallets, insert transaction | Working real backend/database | Có transaction DB, validate cơ bản |
| Transaction history API | Frontend có hàm API nhưng chưa có UI | `GET /api/user/wallet/transactions` | Read `transactions` | Backend ready, UI missing | Cần page/tab hiển thị |
| Services cards UI | Có hardcoded cards | Chưa có API | Seed services có sẵn | UI only/mock | Pay button chỉ hiện message |
| Payment service | Có nút Pay mock | Chưa có API | Schema hỗ trợ `payment` | Not implemented | Cần backend API và frontend connect |
| Deposit | Home mô tả mock deposit | Chưa có UI/API | Schema hỗ trợ `deposit` và seed data | Not implemented | Cần thêm endpoint/form |
| Admin | Login admin có thể thành công | Chưa có admin API | Có role/admin profile seed | Partially prepared | Admin vào dashboard user sẽ lỗi wallet/me |

## 8. Chức năng đang lỗi hoặc cần kiểm tra

| Issue | Location | Explanation | Suggested fix |
| --- | --- | --- | --- |
| Token chỉ là demo token | Backend `UserWalletController.extractUserId`, `AuthController.buildDemoToken` | Token dạng `demo-token-{userId}` dễ giả mạo, không có expiry/signature | Sau khi ổn chức năng, thay bằng JWT hoặc session auth |
| Frontend protected route chỉ check token tồn tại | `frontend/src/App.tsx` | Nếu localStorage có token giả, route vẫn mở; API sẽ báo Unauthorized sau | Gọi `/api/user/wallet/me` hoặc endpoint `/me` để validate token khi app load |
| Services UI chưa gọi DB/API | `frontend/src/pages/DashboardPage.tsx` | Danh sách service hardcoded, nút Pay chỉ hiện message mock | Tạo backend API list/pay services và frontend `serviceApi.ts` |
| Chưa có deposit API/UI | Backend/frontend | Schema có transaction `deposit`, seed có deposit, nhưng app chưa có chức năng nạp tiền | Tạo `POST /api/user/wallet/deposit` và tab/form deposit |
| Transaction history chưa hiển thị | `frontend/src/apis/walletApi.ts`, dashboard | Có `getMyTransactions` nhưng không page/component gọi | Thêm tab/page Transactions, gọi API và render table |
| Admin login không có dashboard riêng | `frontend/src/App.tsx`, `DashboardPage.tsx`, backend auth | Admin không có wallet, nhưng dashboard user gọi wallet/me nên có thể lỗi | Redirect admin sang route admin riêng hoặc chặn admin vào dashboard user |
| Test endpoints không auth | `TestDbController` | `/api/test/users` và transactions trả dữ liệu nhạy cảm trong dev | Giữ dev-only hoặc xóa/khóa trước deploy |
| CORS hardcoded | Backend controllers | Chỉ cho `http://localhost:5173` | Đưa origin vào config/env khi deploy |
| API base URL hardcoded | `frontend/src/apis/axiosClient.ts` | Frontend cố định `http://localhost:8080/api` | Dùng `import.meta.env.VITE_API_URL` |
| Register frontend log password | `frontend/src/pages/RegisterPage.tsx` | `console.log('Register payload:', { phone, password, fullName })` in password ra console | Xóa log trước khi demo/deploy |
| Chưa có backend service layer | Backend controllers | Controller chứa SQL và business logic trực tiếp | Khi mở rộng, tách service/repository để dễ test/maintain |
| Lock receiver wallet chưa dùng `FOR UPDATE` | `UserWalletController.transferMoney` | Sender wallet được lock, receiver wallet được đọc từ query thường trước khi update | Cân nhắc lock cả receiver wallet trong transaction để chắc chắn hơn khi concurrency cao |
| `description.md` / comment SQL bị lỗi hiển thị encoding trong terminal | `description.md`, `database/schema.sql` comments | Nội dung tiếng Việt bị mojibake khi đọc bằng shell hiện tại | Kiểm tra encoding UTF-8 và lưu lại đúng encoding nếu cần |

## 9. Việc cần làm tiếp theo

| Priority | TODO | Files likely need editing | API/database involved |
| --- | --- | --- | --- |
| High | Kết nối transaction history UI | `frontend/src/pages/DashboardPage.tsx`, có thể thêm `TransactionHistory` component | Dùng sẵn `GET /api/user/wallet/transactions` |
| High | Hoàn thiện transfer validation frontend | `frontend/src/pages/DashboardPage.tsx`, có thể thêm schema wallet/transfer | Dùng `POST /api/user/wallet/transfer`; validate phone/amount trước khi gửi |
| High | Thêm deposit API và UI | Backend thêm controller method, frontend thêm tab/form và `walletApi.deposit` | Update `wallets`, insert `transactions` type `deposit` |
| High | Kết nối services từ database | Backend thêm `GET /api/user/services` hoặc `/api/services`, frontend thêm `serviceApi.ts` | Read table `services` |
| High | Thêm payment service API | Backend thêm endpoint payment, frontend nối nút Pay thật | Update `wallets`, insert `transactions` type `payment`, dùng `services` |
| Medium | Refresh balance sau payment/deposit | `DashboardPage.tsx`, store wallet | Gọi lại `getMyWallet` sau giao dịch |
| Medium | Tách backend service/repository | `backend/src/main/java/...` | Giữ SQL hoặc chuyển dần sang repository/JPA |
| Medium | Thêm admin dashboard sau | Frontend routes `/admin`, backend `/api/admin/**` | Dùng `users`, `wallets`, `transactions`, `services` |
| Medium | Đưa config vào env | `axiosClient.ts`, `application.properties`, Docker config | `VITE_API_URL`, DB env vars, CORS origin |
| Low | Cải thiện bảo mật | Backend auth/security config, frontend route guard | JWT/session, role guard, token expiry, remove test endpoints |
| Low | Thêm test | Backend test controller/service, frontend build/lint | Test auth, wallet, transfer, validation |

## 10. API testing commands

Các lệnh dưới đây dùng PowerShell, backend `localhost:8080`, token demo dạng `Bearer demo-token-{userId}`.

Ping backend:

```powershell
Invoke-RestMethod -Method GET -Uri "http://localhost:8080/api/test/ping"
```

Check database:

```powershell
Invoke-RestMethod -Method GET -Uri "http://localhost:8080/api/test/db"
```

Register:

```powershell
$body = @{
  phone = "0911111111"
  password = "123456"
  fullName = "Demo User"
} | ConvertTo-Json

Invoke-RestMethod -Method POST `
  -Uri "http://localhost:8080/api/auth/register" `
  -ContentType "application/json" `
  -Body $body
```

Login:

```powershell
$body = @{
  phone = "0912345678"
  password = "123456"
} | ConvertTo-Json

Invoke-RestMethod -Method POST `
  -Uri "http://localhost:8080/api/auth/login" `
  -ContentType "application/json" `
  -Body $body
```

Get wallet info:

```powershell
Invoke-RestMethod -Method GET `
  -Uri "http://localhost:8080/api/user/wallet/me" `
  -Headers @{ Authorization = "Bearer demo-token-1" }
```

Transfer money:

```powershell
$body = @{
  receiverPhone = "0987654321"
  amount = 5
  description = "Test transfer"
} | ConvertTo-Json

Invoke-RestMethod -Method POST `
  -Uri "http://localhost:8080/api/user/wallet/transfer" `
  -Headers @{ Authorization = "Bearer demo-token-1" } `
  -ContentType "application/json" `
  -Body $body
```

Get transactions:

```powershell
Invoke-RestMethod -Method GET `
  -Uri "http://localhost:8080/api/user/wallet/transactions" `
  -Headers @{ Authorization = "Bearer demo-token-1" }
```

Endpoints chưa tồn tại nên chưa có command thật:

- Deposit: chưa có `POST /api/user/wallet/deposit`.
- Payment: chưa có `POST /api/user/wallet/payment`.
- Services list: chưa có endpoint chính thức ngoài test/raw DB.

## 11. Database checking commands

Vào MySQL trong Docker:

```powershell
docker exec -it ewallet_mysql mysql -uewallet_user -pewallet_pass ewallet_db
```

SELECT users + wallet balance:

```sql
SELECT
  u.id,
  u.phone,
  u.role,
  u.status,
  up.full_name,
  w.id AS wallet_id,
  w.balance
FROM users u
LEFT JOIN user_profiles up ON u.id = up.user_id
LEFT JOIN wallets w ON u.id = w.user_id
ORDER BY u.id;
```

SELECT latest transactions:

```sql
SELECT
  t.id,
  t.transaction_code,
  t.type,
  sender_user.phone AS sender_phone,
  receiver_user.phone AS receiver_phone,
  s.name AS service_name,
  t.amount,
  t.balance_before,
  t.balance_after,
  t.status,
  t.description,
  t.created_at
FROM transactions t
LEFT JOIN wallets sender_wallet ON t.sender_wallet_id = sender_wallet.id
LEFT JOIN users sender_user ON sender_wallet.user_id = sender_user.id
LEFT JOIN wallets receiver_wallet ON t.receiver_wallet_id = receiver_wallet.id
LEFT JOIN users receiver_user ON receiver_wallet.user_id = receiver_user.id
LEFT JOIN services s ON t.service_id = s.id
ORDER BY t.created_at DESC
LIMIT 20;
```

SELECT services:

```sql
SELECT
  id,
  name,
  price,
  description,
  is_active,
  created_at,
  updated_at
FROM services
ORDER BY id;
```

Check one user's wallet and transactions:

```sql
SELECT
  u.phone,
  up.full_name,
  w.id AS wallet_id,
  w.balance
FROM users u
JOIN user_profiles up ON u.id = up.user_id
JOIN wallets w ON u.id = w.user_id
WHERE u.phone = '0912345678';

SELECT
  t.transaction_code,
  t.type,
  t.amount,
  t.status,
  t.description,
  t.created_at
FROM transactions t
JOIN wallets w ON t.sender_wallet_id = w.id OR t.receiver_wallet_id = w.id
JOIN users u ON w.user_id = u.id
WHERE u.phone = '0912345678'
ORDER BY t.created_at DESC;
```

## 12. Notes for future AI/Codex

Dự án là Cloud-based E-wallet mô phỏng gồm React TypeScript frontend, Spring Boot Java backend và MySQL Docker database. Hiện frontend có Home/Login/Register/Dashboard, auth store bằng Zustand + localStorage, Axios base URL hardcoded `http://localhost:8080/api`. Backend dùng Spring Boot 4.1.0, Java 17, `JdbcTemplate`, MySQL, BCrypt. Auth hiện tại là demo token `demo-token-{userId}`, không phải JWT.

Các chức năng real backend/database đã có: register tạo user/profile/wallet với balance 10, login check BCrypt/status, lấy wallet hiện tại, transfer money giữa hai ví, ghi transaction transfer, API lấy transaction history. Các phần còn mock/chưa xong: services tab hardcoded và Pay chỉ hiện message, chưa có deposit API/UI, chưa có payment API, chưa có transaction history UI, chưa có admin dashboard/API. Database đã có bảng `users`, `user_profiles`, `admin_profiles`, `wallets`, `services`, `transactions` và seed data đầy đủ.

Bước nên làm tiếp theo: trước tiên thêm UI transaction history vì backend API đã sẵn; sau đó thêm deposit endpoint/UI; tiếp theo connect services từ DB và implement payment API. Khi chức năng ổn, cải thiện auth bằng JWT/session, tách service/repository, đưa API URL/CORS/DB config vào env, và xử lý admin route riêng.
