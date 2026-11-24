# Context dự án — Ứng dụng kết nối phụ huynh và nhà trường

> **Mục đích file:** Lưu context gốc của dự án để tiếp tục làm việc qua nhiều cuộc trò chuyện. Mỗi khi chốt xong một phần, cập nhật trực tiếp file này thay vì phải gửi lại nhiều file.
>
> **Trạng thái:** Đã chốt phạm vi, ERD/DDL, quy tắc validation và rà soát quyền API. Còn lại: wireframe/prototype Figma và user flow.
> **Cập nhật lần cuối:** 2026-11-04

---

## 1. Thông tin chung

- **Tên đề tài:** Ứng dụng kết nối phụ huynh và nhà trường.
- **Định hướng tham khảo:** vnEdu Connect.
- **Quy mô:** 2 người thực hiện, khoảng 8 tuần.
- **Bối cảnh:** Đồ án môn giao diện/UI-UX, có backend và các yêu cầu bảo mật phù hợp.
- **Mục tiêu:** Xây dựng ứng dụng web giúp phụ huynh theo dõi thông tin học tập của con và giúp giáo viên thực hiện một số nghiệp vụ lớp học.

## 2. Công nghệ dự kiến

- **Frontend:** React + Tailwind CSS, gọi REST JSON. Công cụ build và ngôn ngữ (JS/TS): do người phụ trách UI chốt.
- **Backend:** Java 17+, Spring Boot (Spring Web, Spring Security, Spring JDBC, Jakarta Bean Validation).
- **Database:** SQLite (driver sqlite-jdbc), truy cập bằng Spring JDBC, không dùng JPA.
- **Xác thực:** Session cookie (HttpSession, tên cookie `SCSID`); không dùng JWT + refresh token.
- **Session bền vững:** bắt đầu bằng session Tomcat `persistent=true`; nâng lên Spring Session JDBC nếu còn thời gian (xem §16).
- **Demo:** Ưu tiên chạy localhost. Nếu cần truy cập qua LAN/điện thoại, cấu hình HTTPS cục bộ và bật cookie `Secure`.
- **Phong cách code:** Backend Java theo lớp controller → service → repository; không bổ sung framework ngoài nhu cầu để giữ phạm vi đơn giản.

## 3. Vai trò người dùng

### Phụ huynh (`PARENT`)
- Đăng nhập, đăng xuất.
- Xem thông tin học sinh đã liên kết với tài khoản.
- Xem điểm theo môn học, học kỳ và năm học.
- Xem thời khóa biểu.
- Xem lịch sử điểm danh.
- Gửi và sửa đơn xin nghỉ học cho học sinh đã liên kết.
- Xem thông báo của lớp.

### Giáo viên (`TEACHER`)
- Xem các lớp/môn được phân công.
- Xem danh sách học sinh trong lớp được phân công.
- Nhập và cập nhật điểm trong môn được phân công.
- Nếu là giáo viên chủ nhiệm: ghi nhận điểm danh, duyệt đơn xin nghỉ và đăng thông báo cho lớp chủ nhiệm.

### Quản trị viên (`ADMIN`)
- Tạo và quản lý tài khoản phụ huynh, giáo viên.
- Quản lý lớp học và hồ sơ học sinh.
- Liên kết/gỡ liên kết phụ huynh với học sinh.
- Phân công giáo viên phụ trách lớp/môn.
- Xem audit log theo phạm vi được định nghĩa.
- Không có quyền sửa điểm và xem toàn bộ dữ liệu học tập.
- Thao tác liên kết/gỡ liên kết phụ huynh và phân công giáo viên cần được ghi audit log.

## 4. Phạm vi tính năng

### Bắt buộc
1. Đăng nhập/đăng xuất và phân quyền ở backend.
2. Quản lý tài khoản, lớp, học sinh và liên kết phụ huynh–học sinh.
3. Xem, nhập và cập nhật điểm; ghi audit log cho thay đổi điểm.
4. Điểm danh theo ngày.
5. Đơn xin nghỉ học và quy trình duyệt.
6. Phân quyền đối tượng: phụ huynh chỉ truy cập học sinh đã liên kết; giáo viên chỉ thao tác trong phạm vi được phân công.

### Có thể làm sau khi hoàn thành phần bắt buộc
- Thời khóa biểu: có thể nạp dữ liệu bằng seed script, không cần giao diện quản lý.
- Thông báo theo lớp: triển khai sau khi hoàn thành chức năng cốt lõi; có thể cắt nếu thiếu thời gian.
- Giao diện audit log đơn giản: bảng dữ liệu, có thể lọc theo học sinh; chưa cần phân trang hoặc xuất file.

### Loại khỏi phạm vi
- Nhắn tin/chat trực tiếp.
- Upload tệp/tài liệu.
- Thông báo đẩy.
- Đồng bộ offline/đa thiết bị.
- TOTP/2FA.
- Quản lý danh sách thiết bị đăng nhập.
- Quản lý sức khỏe học sinh.
- Quản lý bài tập hoặc lịch thi riêng.
- Chức năng môn học/thời khóa biểu phức tạp; dữ liệu có thể seed sẵn.

## 5. Quy tắc phân quyền cốt lõi

- **Phụ huynh:** Chỉ xem dữ liệu của học sinh có liên kết hợp lệ trong bảng liên kết phụ huynh–học sinh.
- **Giáo viên bộ môn:** Chỉ xem danh sách học sinh cần thiết và nhập/sửa điểm của lớp/môn được phân công.
- **Giáo viên chủ nhiệm:** Được điểm danh, xử lý đơn xin nghỉ và đăng thông báo trong lớp chủ nhiệm.
- **Admin:** Quản lý tài khoản, lớp, hồ sơ học sinh, liên kết phụ huynh và phân công giáo viên; Không sửa điểm, không xem dữ liệu học tập; xem audit log.
- Mọi quyền phải được kiểm tra ở backend cho từng request; ẩn nút trên giao diện không được xem là biện pháp phân quyền.
- Cần chống IDOR/BOLA bằng cách kiểm tra quan hệ người dùng–đối tượng trong truy vấn database.
- Quyền liên kết phụ huynh với học sinh nhạy cảm: audit log giúp truy vết nhưng không thay thế việc giới hạn quyền.

## 6. Bảo mật và giới hạn demo

Phạm vi bảo mật tập trung vào 6 nhóm, không triển khai các cơ chế mật mã hoặc hạ tầng bảo mật phức tạp ngoài nhu cầu của đồ án:

1. **Xác thực:** băm mật khẩu bằng bcrypt hoặc Argon2; quản lý session; regenerate session sau đăng nhập; hủy session khi đăng xuất.
2. **Phân quyền:** kiểm tra role và quyền trên từng tài nguyên ở backend; phụ huynh chỉ truy cập học sinh đã liên kết, giáo viên chỉ thao tác trong phạm vi được phân công.
3. **Kiểm tra dữ liệu đầu vào:** validate kiểu, định dạng, độ dài, khoảng giá trị và trạng thái nghiệp vụ; dùng truy vấn SQL tham số hóa.
4. **Bảo vệ cơ sở dữ liệu:** khóa ngoại, unique/check constraints, transaction; bật `PRAGMA foreign_keys = ON` trên mỗi kết nối SQLite; bảo vệ file database và thông tin cấu hình.
5. **Ghi log:** ghi các sự kiện quan trọng như đăng nhập/đăng xuất và các thao tác thay đổi dữ liệu cần kiểm soát.
6. **Truy vết:** audit log ghi actor, hành động, đối tượng, thời điểm và giá trị cũ/mới khi phù hợp; cập nhật dữ liệu và audit log trong cùng transaction.


### HTTPS và mã hóa
- Dùng HTTPS khi truy cập qua mạng (kể cả LAN); localhost có thể dùng HTTP trong môi trường demo cục bộ.
- HTTPS/TLS mã hóa dữ liệu trên đường truyền, vì vậy không cần tự mã hóa thêm payload API hoặc từng cột dữ liệu trong SQLite cho phạm vi đồ án này.
- **Vẫn phải băm mật khẩu:** HTTPS không bảo vệ mật khẩu đã lưu trong database. Không lưu mật khẩu dạng rõ; dùng bcrypt hoặc Argon2.
- Không triển khai mã hóa dữ liệu ở cấp ứng dụng, quản lý khóa riêng hoặc các cơ chế mã hóa phức tạp khác trong phạm vi hiện tại.
- Session cookie có `HttpOnly` và `SameSite=Lax`; bật `Secure` khi dùng HTTPS.
- Với request ghi dữ liệu, kiểm tra `Origin` phù hợp để giảm rủi ro CSRF; không dùng GET cho thao tác thay đổi dữ liệu.
- Audit log append-only ở mức ứng dụng; trigger SQLite có thể chặn `UPDATE`/`DELETE`, nhưng không chống được người có quyền ghi trực tiếp file database.
- Bỏ `must_change_password` để giảm khối lượng.

## 7. Mô hình dữ liệu (đã chốt, xem DDL ở mục 12)

### Ghi chú thiết kế
- `users.role`: `PARENT`, `TEACHER`, `ADMIN`.
- `classes.homeroom_teacher_id` xác định giáo viên chủ nhiệm.
- `parent_students` có khóa chính ghép `(parent_id, student_id)` và `student_id UNIQUE`: một phụ huynh có thể liên kết nhiều học sinh, mỗi học sinh chỉ liên kết với một tài khoản phụ huynh.
- `teaching_assignments` liên kết giáo viên, lớp và môn; dự kiến mỗi lớp/môn có một giáo viên phụ trách trong phạm vi demo.
- `grades` có ràng buộc điểm từ 0 đến 10 và tránh trùng đầu điểm theo học sinh/môn/năm học/học kỳ/loại điểm.
- `attendance` ghi nhận theo ngày, không theo từng tiết; mỗi học sinh tối đa một bản ghi/ngày. Ngày nghiệp vụ lưu `YYYY-MM-DD`; giao diện hiển thị thứ và ngày theo tiếng Việt.
- `leave_requests` phải đảm bảo phụ huynh tạo đơn có liên kết với học sinh đó; ngày bắt đầu không sau ngày kết thúc. Không tạo đơn nghỉ có `from_date` trong quá khứ.
- `timetable` và `subjects` có thể dùng seed script.
- `audit_logs` ghi tối thiểu actor, action, đối tượng, giá trị cũ/mới khi cần và thời điểm.
- Kiểm tra role thực tế của các ID tham chiếu (ví dụ `parent_id` phải thuộc role `PARENT`) ở service/backend.

**Lưu ý:** ERD/DDL đã được rà soát và chốt, xem mục 12. Các ràng buộc SQLite không biểu diễn được (ví dụ `parent_id` phải thuộc role `PARENT`, `teacher_id` phải thuộc role `TEACHER`) kiểm tra ở lớp service.

## 8. Kế hoạch 8 tuần

- **Tuần 1:** Chốt yêu cầu, ERD, ma trận phân quyền, API contract, wireframe/prototype và threat model cơ bản.
- **Tuần 2:** Khởi tạo dự án, database, đăng nhập/đăng xuất, session cookie, rate limiting.
- **Tuần 3:** Tài khoản, lớp, học sinh, liên kết phụ huynh–học sinh.
- **Tuần 4:** Điểm, thời khóa biểu seed và kiểm tra quyền truy cập đối tượng.
- **Tuần 5:** Điểm danh và đơn xin nghỉ.
- **Tuần 6:** Audit log, thông báo nếu còn thời gian, hoàn thiện UI.
- **Tuần 7:** Kiểm thử IDOR/BOLA, brute force, SQL injection, XSS, các vai trò và responsive.
- **Tuần 8:** Sửa lỗi, regression test, báo cáo và chuẩn bị demo.

## 9. Thứ tự ưu tiên khi thiếu thời gian

1. Không cắt đăng nhập và phân quyền backend.
2. Không cắt kiểm soát quyền truy cập dữ liệu học sinh.
3. Không cắt cập nhật điểm kèm audit log.
4. Không cắt điểm danh và đơn xin nghỉ.
5. Dùng seed script thay cho màn hình quản lý môn học/thời khóa biểu.
6. Giữ audit log ở giao diện bảng đơn giản.
7. Cắt thông báo theo lớp trước nếu không kịp.
8. Không bổ sung tính năng ngoài phạm vi khi chưa hoàn thành phần bắt buộc.

## 10. Các quyết định đã chốt và phần còn mở

### Đã chốt
- Đề tài kết nối phụ huynh và nhà trường, tham khảo vnEdu Connect.
- Quy mô 2 người, khoảng 8 tuần.
- Stack: React/Vite + JavaScript + Tailwind, Spring Boot (Java), SQLite.
- Dùng session cookie thay cho JWT + refresh token.
- Có ba vai trò `PARENT`, `TEACHER`, `ADMIN`.
- Phân biệt giáo viên bộ môn và giáo viên chủ nhiệm.
- Admin không sửa điểm, không xem dữ liệu học tập; ngoại lệ có chủ ý: audit log chứa giá trị điểm cũ/mới
- Phạm vi không có chat, upload tệp, thông báo đẩy, TOTP hay đồng bộ offline.
- HTTP chỉ dùng cho localhost; demo qua LAN cần HTTPS cục bộ.
- ERD/DDL cuối cùng (mục 12) và bảng API kèm quyền (mục 13).
- Bỏ `must_change_password`; giữ `failed_logins` và `locked_until`.
- Không xóa điểm, chỉ sửa kèm audit log; Admin chỉ vô hiệu hóa tài khoản (`is_active`), không xóa cứng.
- Thao tác liên kết/gỡ liên kết phụ huynh và phân công giáo viên đều ghi audit log.
- Sửa đơn nghỉ PENDING: chỉ kiểm tra fromDate không quá khứ khi fromDate bị đổi.
- Wireframe/prototype, design system, quy ước frontend và tiêu chí nghiệm thu đã chốt; còn lại API test/sample; (làm sau khi code fe). 

### Đã loại bỏ (không bàn lại)
- Thông báo toàn trường (`announcements.class_id` bắt buộc).
- `timetable.teacher_id` (suy ra từ `teaching_assignments`).
- `attendance.class_id` (suy ra từ học sinh).
- `students.gender`.
- Giao diện quản lý môn học và thời khóa biểu (dùng seed script).
- Tự reject và deadline đơn nghỉ, actor_type/SYSTEM, quyền sửa điểm của Admin.

### Cần chốt
- thông báo theo lớp là phần còn phụ thuộc tiến độ.

## 11. Quy ước cập nhật file context

Mỗi khi hoàn thành một phần:
1. Cập nhật quyết định đã chốt vào mục tương ứng.
2. Chuyển mục đó từ “Cần chốt tiếp” sang “Đã chốt”.
3. Ghi các lựa chọn bị loại bỏ để tránh thảo luận lại.
4. Cập nhật trạng thái và ngày sửa.
5. Giữ file này làm nguồn context chính; chỉ tạo file riêng khi thực sự cần cho một đầu ra cụ thể (ví dụ báo cáo hoặc tài liệu API đầy đủ).

---

## 12. ERD và DDL cuối cùng

Database gồm 12 bảng, phục vụ quản lý tài khoản, lớp học, học sinh, phụ huynh, giáo viên và hoạt động học tập.
- DDL chi tiết: ddl.md; ERD: dbdiagram

### 1. Danh sách bảng

| Bảng | Chức năng |
|---|---|
| `users` | Lưu tài khoản người dùng gồm phụ huynh, giáo viên và admin. Phân biệt bằng trường `role`. Lưu thông tin đăng nhập, trạng thái tài khoản và thời điểm tạo. |
| `classes` | Lưu danh sách lớp theo năm học. Mỗi lớp có một giáo viên chủ nhiệm. Cặp `class_code` và `school_year` là duy nhất. Một giáo viên chủ nhiệm chỉ phụ trách tối đa một lớp trong 1 năm học. |
| `students` | Lưu thông tin học sinh, mã học sinh dạng TEXT, ngày sinh và lớp đang theo học. `class_id` không UNIQUE vì một lớp có nhiều học sinh. |
| `parent_students` | Bảng liên kết tài khoản phụ huynh với học sinh. Một tài khoản phụ huynh có thể quản lý nhiều con; mỗi học sinh chỉ liên kết với một tài khoản phụ huynh. Trường `relationship` gồm `PARENT` hoặc `GUARDIAN`, không phân biệt cha, mẹ hay người thân cụ thể. |
| `subjects` | Lưu danh sách môn học, gồm tên môn và mã môn. |
| `teaching_assignments` | Phân công giáo viên dạy môn học cho từng lớp. Mỗi lớp chỉ có một giáo viên phụ trách một môn; một lớp có thể có nhiều giáo viên và một giáo viên có thể dạy nhiều lớp, nhiều môn. |
| `timetable` | Lưu thời khóa biểu theo lớp, môn học, thứ, tiết, phòng học, năm học và học kỳ. Một lớp không thể có hai môn học trùng cùng một tiết. Không quản lý nhiều phiên bản thời khóa biểu trong phạm vi đồ án; cập nhật lịch hiện tại và ghi thay đổi vào audit log. |
| `grades` | Lưu điểm của từng học sinh theo môn học, năm học, học kỳ và loại điểm (`TX1`, `TX2`, `GK`, `CK`). Giáo viên có thể cập nhật điểm; thông tin người cập nhật và thời điểm cập nhật được lưu lại. |
| `attendance` | Lưu tình trạng điểm danh của học sinh theo ngày, không chia theo môn học. Mỗi học sinh có tối đa một bản ghi điểm danh trong một ngày. |
| `leave_requests` | Lưu đơn xin nghỉ học do phụ huynh gửi, có thể xin nghỉ nhiều ngày. Khi mới gửi, đơn có trạng thái `PENDING`, `reviewed_by` và `reviewed_at` là `NULL`. Khi được xử lý, lưu người duyệt, thời điểm duyệt và trạng thái `APPROVED` hoặc `REJECTED`. |
| `announcements` | Lưu thông báo của giáo viên dành cho một lớp. Mỗi lớp có thể có nhiều thông báo. |
| `audit_logs` | Lưu nhật ký kiểm toán các thao tác quan trọng, gồm người thực hiện, hành động, loại đối tượng, ID đối tượng và dữ liệu trước/sau khi thay đổi. |

### 2. Các mối quan hệ

1. Một lớp có đúng một giáo viên chủ nhiệm; một giáo viên chỉ được làm chủ nhiệm tối đa một lớp trong 1 năm học.
2. Một lớp có nhiều học sinh; mỗi học sinh thuộc một lớp.
3. Một tài khoản phụ huynh có thể quản lý nhiều học sinh; mỗi học sinh chỉ liên kết với một tài khoản phụ huynh.
4. Một giáo viên có thể dạy nhiều lớp và nhiều môn; một lớp có nhiều môn học. Mỗi cặp lớp – môn học chỉ có một giáo viên phụ trách.
5. Một phân công giảng dạy có thể tương ứng với nhiều tiết trong thời khóa biểu. Mỗi tiết học phải thuộc một phân công lớp – môn học hợp lệ.
6. Một học sinh có nhiều điểm theo môn học, năm học, học kỳ và loại điểm. Giáo viên có thể cập nhật điểm nhiều lần; lịch sử thay đổi được ghi nhận qua audit log.
7. Một học sinh có nhiều bản ghi điểm danh theo ngày; mỗi ngày tối đa một bản ghi.
8. Một cặp phụ huynh – học sinh có thể phát sinh nhiều đơn xin nghỉ học.
9. Một giáo viên có thể duyệt nhiều đơn xin nghỉ; đơn chưa được duyệt không có người duyệt.
10. Một lớp có thể có nhiều thông báo; mỗi thông báo thuộc một lớp và có một người tạo.
11. Một tài khoản người dùng có thể tạo nhiều bản ghi audit log.

### 3. Quy ước và phạm vi

- `role`: `PARENT`, `TEACHER`, `ADMIN`.
- `relationship`: `PARENT`, `GUARDIAN`.
- `term` trong bảng điểm: `HK1`, `HK2`.
- `score_type`: `TX1`, `TX2`, `GK`, `CK`.
- `status` của đơn xin nghỉ: `PENDING`, `APPROVED`, `REJECTED`.
- `status` điểm danh: `PRESENT`, `EXCUSED`, `UNEXCUSED`, `LATE`.
- Việc kiểm tra vai trò của tài khoản khi phân công giáo viên, chủ nhiệm lớp hoặc liên kết phụ huynh – học sinh được thực hiện ở tầng ứng dụng.


## 13. Bảng API và quyền truy cập

**Quy ước chung**
- Base path: `/api`. Ví dụ `POST /api/auth/login`.
- Các API yêu cầu đăng nhập dùng session cookie; frontend gọi cùng origin (proxy của dev server trong môi trường phát triển). Không gửi session ID trong JSON.
- Request/response dùng JSON và tên trường `camelCase`. ID là chuỗi (SQLite có thể lưu INTEGER, backend serialize thành string nhất quán).
- Response thành công theo envelope: `{ "success": true, "data": ... }`.
- Response lỗi theo envelope: `{ "success": false, "error": { "code": "...", "message": "..." } }`.
- Không trả password hash, session ID, thông tin nội bộ hoặc stack trace cho frontend.
- `401` = chưa đăng nhập/hết session; `403` = đã đăng nhập nhưng không có quyền; `404` = không tìm thấy hoặc cố ý che giấu tài nguyên ngoài phạm vi; `400` = dữ liệu đầu vào sai; `409` = xung đột trạng thái/trùng dữ liệu; `500` = lỗi server.
- Với thao tác ghi, kiểm tra `Origin`/CSRF theo cấu hình; không thực hiện thay đổi dữ liệu bằng GET.
- Tên field và ví dụ dưới đây là hợp đồng giữa frontend/backend. Nếu thay đổi phải cập nhật cả hai phía và file context.
- 403 chỉ dành cho sai vai trò hoặc Origin không hợp lệ; thiếu quan hệ sở hữu/liên kết/phân công thì trả 404.


### 13.1. Xác thực

| Method + Endpoint      | Role                   | Request chính                                   | Response `data` khi thành công                                  |
|------------------------|------------------------|-------------------------------------------------|-----------------------------------------------------------------|
| `POST   /auth/login`   | công khai              | `{ "email": "p01@...com", "password": "..." }`  | `{ "user": { "id": "..", "fullName": "..", "role": "PARENT" }}` |
|                        |                        |                                                 |           ;server tạo session cookie và regenerate session ID.  |
| `POST   /auth/logout`  | mọi role đã đăng nhập  | Không cần body                                  | `{ "message": "Đã đăng xuất" }`          ; server hủy session.  |
| `GET    /auth/me`      | mọi role đã đăng nhập  | Không cần                                       | `{ "user": { "id": "..", "fullName": "..", "role": "PARENT" }}` |


### 13.2. Phụ huynh

| Method + Endpoint                  | điều kiện              | Request chính                       | Response `data` khi thành công                                  |
|------------------------------------|------------------------|-------------------------------------------------|-------------------------------------------|
| `GET  /students`                   | Chỉ con đã liên kết              | Không cần | `{ "items": [{ "id", "studentCode", "fullName", "dateOfBirth", "class": { "id", "name", "classCode", "schoolYear" } }] }` |

| `GET  /students/:id/grades`        | phải có liên kết với `:id`       | Query tùy chọn: `schoolYear`, `term`, `subjectId` | `{ "studentId": "s1", "items": [{ "id": "g1", "subject": { "id": "sub1", "name": "Toán" }, "scoreType": "TX1", "score": 8.5, "term": "HK1", "schoolYear": "2026-2027" }] }` |

| `GET  /students/:id/attendance`    | phải có liên kết                 | Query tùy chọn: `from`, `to` | `{ "studentId": "s1", "items": [{ "date": "2026-10-31", "status": "PRESENT"}] }` | 

| `GET  /students/:id/timetable`     | phải có liên kết                 | Query tùy chọn: `schoolYear`, `term` | `{ "studentId": "s1", "items": [{ "weekday": 2, "period": 1, "subject":{ "id", "name" } }] }` |

| `GET  /students/:id/leave-requests`| phải có liên kết                 | Query tùy chọn: `status` | `{ "studentId": "s1", "items": [{ "id": "lr1", "fromDate": "2026-11-02", "toDate": "2026-11-03", "reason": "Bị ốm", "status": "PENDING", "createdAt": "2026-10-31T08:00:00Z" }] }` |

| `POST /leave-requests`             | `studentId` phải thuộc phụ huynh | `{ "studentId": "s1", "fromDate": "2026-11-02", "toDate": "2026-11-03", "reason": "Bị ốm" }` | HTTP `201`: `{ "leaveRequest": { "id": "lr1", "studentId": "s1", "status": "PENDING" } }` |

| `PATCH /leave-requests/:id`        |`studentId` phải thuộc phụ huynh, đơn của phụ huynh đang PENDING | chỉ sửa 3 trường `{"fromDate":"2026-10-31", "toDate":"2026-10-31", "reason":"..."}`|`{ "leaveRequest": { "id": "lr1", "fromDate":"2026-10-31", "toDate":"2026-10-31", "reason":"..." }}`|

| `GET  /classes/:id/announcements`  | có con trong lớp                 | Không cần | `{ "items": [{ "id": "a1", "title": "Lịch kiểm tra", "content": "...", "createdAt": "2026-10-31T08:00:00Z" }] }` |

- Query tùy chọn: không có query thì trả tất cả, có query thì chỉ trả phần khớp.

### 13.3. Giáo viên

| Method + endpoint                     | Điều kiện   | Request chính | Response `data` khi thành công |
|---------------------------------------|------------------------|-------------------------------------------------|-------------------------------------------|
| `GET   /teacher/classes`              |    N/A   |  Không cần | `{ "items": [{ "classId": "c1", "className": "10A1", "subjectId": "sub1", "subjectName": "Toán", "isHomeroom": false }] }` | 

| `GET   /classes/:id/students`         | Là GVCN hoặc được phân công môn tại lớp | Không cần | `{ "items": [{ "id": "s1", "fullName": "Tran Thi B", "studentCode": "HS001" }] }` |

| `GET   /classes/:id/grades?subjectId=`| Phải được phân công đúng lớp/môn | Query bắt buộc `subjectId`; có thể có `term`, `schoolYear`, `scoreType` | `{ "classId": "c1", "subjectId": "sub1", "items": [{ "studentId": "s1", "studentName": "Tran Thi B", "grades": [{ "id": "g1", "Scoretype": "TX1", "score": 8.5 }] }] }` |

| `PUT   /grades`                       | Đúng phân công lớp/môn | `{ "classId": "c1", "subjectId": "sub1", "studentId": "s1", "scoreType": "TX1", "term": "HK1", "schoolYear": "2026-2027", "score": 8.5 }` | `{ "grade": { "id": "g1", "studentId": "s1", "subjectId": "sub1", "scoreType": "TX1", "score": 8.5 } }`; cập nhật grade và audit log trong cùng transaction. |

| `GET   /classes/:id/attendance?date=` | Là GVCN | Query bắt buộc `date` | `{ "classId": "c1", "date": "2026-10-31", "items": [{ "studentId": "s1", "studentName": "Tran Thi B", "status": "PRESENT" }] }` |

| `PUT   /classes/:id/attendance`       | Là GVCN | `{ "date": "2026-10-31", "items": [{ "studentId": "s1", "status": "PRESENT" }] }` | `{ "date": "2026-10-31", "updatedCount": 1 }`; mọi `studentId` phải thuộc lớp `:id`. |

| `GET   /classes/:id/leave-requests`   | Là GVCN | Query tùy chọn `status` | `{ "items": [{ "id": "lr1", "studentId": "s1", "studentName": "Tran Thi B", "fromDate": "2026-11-02", "toDate": "2026-11-03", "reason": "Bị ốm", "status": "PENDING" }] }` |

| `PATCH /leave-requests/:id`           | Là GVCN; đơn đang `PENDING` | `{ "status": "APPROVED" }` | `{ "leaveRequest": { "id": "lr1", "status": "APPROVED", "reviewedAt": "2026-10-31T09:00:00Z" } }` | 

| `POST  /classes/:id/announcements` | Là GVCN | `{ "title": "Lịch kiểm tra", "content": "..." }` | HTTP `201`: `{ "announcement": { "id": "a1", "classId": "c1", "title": "Lịch kiểm tra" } }` |

| `GET /classes/:id/announcements` | Là GVCN lớp đó | không có | `{ "items": [{ "id": "a1", "title": "Lịch kiểm tra", "content": "...", "createdAt": "2026-10-31T08:00:00Z" }] }`|

| `GET /subjects` |	TEACHER, ADMIN | không có | `{ "items": [{ "id", "name", "subjectCode" }] }` |

### 13.4. Quản trị viên
| Method + endpoint                                  | Request chính | Response `data` khi thành công |
|----------------------------------------------------|------------------------|------------------------------------------------|

| `GET /admin/users` | Query tùy chọn `role`, `search` | `{ "items": [{ "id": "u1", "email": "p01@...com", "fullName": "Nguyen Van A", "role": "PARENT", "isActive": true }] }` |

| `POST /admin/users` | `{ "email": "p02@...com", "password": "...", "fullName": "...", "role": "PARENT" }` | HTTP `201`: `{ "user": { "id": "u2", "email": "p02@...com", "fullName": "...", "role": "PARENT" } }` |

| `PATCH /admin/users/:id` | allowlist: fullName, isActive; password (đặt lại mật khẩu), unlock: true (xóa failed_logins và locked_until) | `{ "user": { "id": "u2", "fullName": "...", "isActive": false } }` |

| `GET /admin/classes` | Query tùy chọn `search` | `{ "items": [{ "id": "c1", "name": "10A1", "classCode" ,"schoolYear": "2026-2027", "homeroomTeacherId": "t1" }] }` |

| `POST /admin/classes` | `{ "name": "10A2", "classCode" : "..", "schoolYear": "2026-2027", "homeroomTeacherId": "t1" }` | HTTP `201`: `{ "class": { "id": "c2", "name": "10A2" } }` |

| `PATCH /admin/classes/:id` | Các field được phép sửa: name, classCode, homeroomTeacherId, schoolYear | `{ "class": { "id": "c2", "name": "10A2" } }` |

| `GET /admin/students` | Query tùy chọn `classId`, `search` | `{ "items": [{ "id", "studentCode", "fullName", "dateOfBirth", "classId" }] }` |

| `POST /admin/students` | `{ "studentCode": "HS002", "fullName": "...", "dateOfBirth", "classId": "c1" }` | HTTP `201`: `{ "student": { "id": "s2", "studentCode": "HS002", "fullName": "...", "dateOfBirth", "classId": "c1" } }` |

| `PATCH /admin/students/:id` | Các field được phép sửa: fullName, dateOfBirth, classId | `{ "student": { "id": "s2", "fullName": "...", "classId": "c1" } }` |

| `GET /admin/parent-students` | query: parentId, studentId	| `{ "items": [{ "parentId", "studentId", "relationship" }] }` |

| `POST /admin/parent-students` | `{ "parentId": "u1", "studentId": "s1" , "relationship"}` | HTTP `201`: `{ "link": { "parentId": "u1", "studentId": "s1" , "relationship"} }`; ghi audit log. |

| `DELETE /admin/parent-students/:parentId/:studentId` | Hai ID nằm trên URL, không cần body | `{ "deleted": true }`; ghi audit log. |

| `GET /admin/teaching-assignments` | query: classId, teacherId | `{ "items": [{ "teacherId", "classId", "subjectId" }] }` |

| `POST /admin/teaching-assignments` | `{ "teacherId": "t1", "classId": "c1", "subjectId": "sub1" }` | HTTP `201`: `{ "assignment": { "teacherId": "t1", "classId": "c1", "subjectId": "sub1" } }`; ghi audit log. |

| `DELETE /admin/teaching-assignments/:teacherId/:classId/:subjectId` | Ba ID nằm trên URL, không cần body | `{ "deleted": true }`; ghi audit log. |

| `GET /admin/audit-logs?studentId=...` | Query tùy chọn `studentId`, `action` | `{ "items": [{ "id": "al1", "actorId": "t1", "action": "UPDATE", "entityType": "grades", "entityId": "g1", "oldValue": { "score": 6.5 }, "newValue": { "score": 7.5 }, "createdAt": "2026-10-31T09:00:00Z" }] }` |


### 13.5. Enum và validation cần dùng thống nhất

- `role`: `PARENT`, `TEACHER`, `ADMIN`.
- `relationship` : `PARENT`, `GUARDIAN`.
- `leave_requests.status`: `PENDING`, `APPROVED`, `REJECTED`. Chỉ cho phép duyệt khi đơn đang `PENDING`.
- `attendance.status`: `PRESENT`, `UNEXCUSED`, `LATE`, `EXCUSED`.
- `grades.scoreType`: `TX1`, `TX2`, `GK`, `CK`.
- `grades.term` : `HK1`, `HK2`.
- `grades.score`: số từ `0` đến `10`; không nhận chuỗi tùy tiện hoặc giá trị ngoài khoảng.
- Ngày nghiệp vụ dùng `YYYY-MM-DD`; timestamp dùng ISO 8601 UTC, ví dụ `2026-10-31T09:00:00Z`. Frontend hiển thị theo giờ Việt Nam nhưng không tự đổi ngày nghiệp vụ.
- `fromDate` phải nhỏ hơn hoặc bằng `toDate`; `date`, `schoolYear`, `term` phải được validate ở backend.
- Danh sách trả về dùng `{ items: [] }` kể cả khi không có dữ liệu, không trả `null` thay cho mảng rỗng.
- Các endpoint cập nhật theo lô (điểm danh) phải validate toàn bộ danh sách trước khi ghi; nếu một phần tử sai thì không ghi dở dang.
- `PUT /grades` cần kiểm tra đồng thời: giáo viên được phân công đúng lớp/môn; học sinh thuộc lớp; môn thuộc lớp; điểm hợp lệ. Dùng ownership predicate trong câu SQL hoặc kiểm tra tương đương trong transaction.
- Với endpoint liên kết/gỡ liên kết và phân công/gỡ phân công, validate role của các ID tham chiếu; không chỉ kiểm tra ID tồn tại.
- `audit_logs.action`: `CREATE`, `UPDATE`, `DELETE`.


### 13.6. Ví dụ response lỗi

```json
{
  "success": false,
  "error": {
    "code": "FORBIDDEN",
    "message": "Bạn không có quyền truy cập tài nguyên này."
  }
}
```

Một số `error.code` thống nhất: `VALIDATION_ERROR`, `UNAUTHENTICATED`, `FORBIDDEN`, `NOT_FOUND`, `CONFLICT`, `INTERNAL_ERROR`. Frontend dựa vào `code` để xử lý; không dựa vào việc so sánh chuỗi `message`.

**Lưu ý thiết kế:** API contract này chốt đường đi và cấu trúc dữ liệu ở mức đủ để bắt đầu thiết kế frontend/backend. Trước khi code cần rà soát lại tên field theo ERD cuối cùng. Có thể bổ sung phân trang (`limit`, `cursor`/`page`) nếu dữ liệu demo lớn; không cần triển khai phức tạp cho đồ án nhỏ.


## 14. Những việc cần hoàn thành để frontend và backend đồng nhất

Nên hoàn thành các mục dưới đây trước khi chia nhau code song song:

1. **Chốt wireframe và luồng màn hình:** đăng nhập; màn hình phụ huynh; màn hình giáo viên; màn hình admin. Mỗi màn hình cần ghi rõ role, dữ liệu hiển thị, thao tác và API tương ứng.`Xong`
2. **Lập bảng mapping UI ↔ API:** mỗi nút/form gắn với endpoint, method, request body/query, trạng thái loading, response thành công và lỗi cần hiển thị.`Xong`
3. **Chốt quy ước kỹ thuật:** cấu trúc thư mục, tên biến `camelCase`, ID dạng string ở API, timezone, format lỗi, HTTP status, cách gọi API bằng `fetch`, credentials/session và proxy của dev server. `Xong`
4. **Chuẩn bị seed data:** tài khoản demo đủ ba role; lớp, học sinh, phụ huynh đã liên kết; giáo viên chủ nhiệm; phân công môn; điểm, điểm danh và đơn nghỉ mẫu. Nhờ đó frontend có dữ liệu ổn định để dựng giao diện. `Xong`
5. **Tạo API test/sample:** dùng Postman/Thunder Client hoặc file JSON mẫu để kiểm tra endpoint và giúp frontend tích hợp trước khi toàn bộ backend hoàn thành.
6. **Chốt tiêu chí nghiệm thu:** mỗi chức năng có happy path và trường hợp bị từ chối quyền (ví dụ phụ huynh thử xem học sinh không liên kết; giáo viên sửa điểm lớp/môn không được phân công).`Xong`

### Cách phối hợp hai người

- Cùng chốt ERD, API contract, enum và wireframe trước; đây là phần tránh lệch giao diện/dữ liệu nhiều nhất.
- Người làm backend chịu trách nhiệm API, session, database, validation, phân quyền và seed data; cung cấp response mẫu đúng contract.
- Người làm frontend dựng layout/route theo role, form/table và trạng thái loading/empty/error; dùng response mẫu hoặc mock data có cùng cấu trúc API.
- Tích hợp theo từng luồng dọc, không đợi xong toàn bộ: đăng nhập → phụ huynh xem học sinh → xem điểm; sau đó giáo viên nhập điểm; tiếp đến điểm danh/đơn nghỉ; cuối cùng admin.
- Khi đổi API, phải cập nhật contract và thông báo người còn lại trước khi sửa code. Không tự ý đổi tên field hoặc enum ở riêng một phía.

**Thứ tự nên làm tiếp theo:** (1) wireframe + mapping màn hình/API → (2) seed data và response mẫu → (3) chia việc frontend/backend và bắt đầu code.


---

## 15. Chốt validation và rà soát quyền API (2026-11-04)

Mục này bổ sung và ưu tiên hơn các ví dụ cũ ở mục 13 nếu có khác biệt. Những quyết định chưa chốt được ghi rõ là đang mở.

### 15.1. Validation tài khoản (`users`)

| Trường | Quy tắc |
|---|---|
| `full_name` | TEXT, bắt buộc, trim, 5–50 ký tự; cho phép chữ tiếng Việt có dấu, dấu cách và dấu gạch nối |
| `email` | TEXT, bắt buộc, trim, 5–50 ký tự, đúng định dạng email, UNIQUE |
| `password` đầu vào | Bắt buộc, tối thiểu 8 ký tự; chưa yêu cầu quy tắc chữ hoa/chữ thường/số/ký tự đặc biệt |
| `password_hash` trong DB | Lưu hash bcrypt hoặc Argon2, không lưu mật khẩu rõ |
| `role` | Chỉ `PARENT`, `TEACHER`, `ADMIN` |
| `is_active` | Chỉ `0` hoặc `1` |

Quy ước email/mật khẩu:
- Username đăng nhập chính là email; API dùng field `email`, không dùng `username`.
- Email không phân biệt hoa/thường: trim và chuyển về chữ thường trước khi lưu và trước khi truy vấn đăng nhập. DB UNIQUE trên giá trị đã chuẩn hóa.
- Mật khẩu phân biệt hoa/thường; không trim và không chuyển đổi chữ hoa/chữ thường.
- Tên hiển thị lấy từ `fullName`, không lấy email.
- Khóa tài khoản: sai mật khẩu liên tiếp 5 lần thì đặt locked_until = hiện tại + 5 phút; đăng nhập đúng sau khi hết khóa thì failed_logins = 0; trong lúc khóa mọi đăng nhập, kể cả đúng, đều bị từ chối với cùng thông báo 401.

### 15.2. Validation lớp, học sinh và liên kết

| Bảng/trường | Quy tắc |
|---|---|
| `classes.name` | TEXT, bắt buộc, 2–20 ký tự |
| `classes.class_code` | TEXT, bắt buộc, 2–10 ký tự |
| `classes.school_year` | TEXT, định dạng `YYYY-YYYY`, ví dụ `2026-2027` |
| `classes.homeroom_teacher_id` | INTEGER, NOT NULL, UNIQUE cùng school_year; tài khoản phải có role `TEACHER` |
| `students.student_code` | TEXT, NOT NULL, UNIQUE|
| `students.full_name` | TEXT, NOT NULL, trim, 5–50 ký tự |
| `students.date_of_birth` | TEXT, định dạng `YYYY-MM-DD`, ngày hợp lệ và không lớn hơn ngày hiện tại , NOT NULL|
| `students.class_id` | INTEGER, NOT NULL, không UNIQUE; mỗi học sinh có một lớp hiện tại, một lớp có nhiều học sinh |
| `parent_students.parent_id` | INTEGER, NOT NULL, tài khoản role `PARENT`, không UNIQUE |
| `parent_students.student_id` | INTEGER, NOT NULL, UNIQUE; mỗi học sinh chỉ liên kết một tài khoản phụ huynh |
| `parent_students.relationship` | NOT NULL, chỉ `PARENT`, `GUARDIAN` |

Mã học sinh:
- Kiểu dữ liệu chốt là TEXT.
- student_code: Niên khóa-lớp-firstName-stt Trong Lớp; 12-10A1-An-01; bỏ dấu tên. (Niên khóa là tính từ ngày thành lập trường)

### 15.3. Validation môn học, phân công và thời khóa biểu

| Bảng/trường | Quy tắc |
|---|---|
| `subjects.name` | TEXT, NOT NULL, 2–60 ký tự |
| `subjects.subject_code` | TEXT, NOT NULL, UNIQUE, 2–10 ký tự |
| `teaching_assignments.teacher_id` | INTEGER, NOT NULL; tài khoản phải có role `TEACHER` |
| `teaching_assignments.class_id`, `subject_id` | INTEGER, NOT NULL |
| `timetable.weekday` | INTEGER, NOT NULL, từ 2 đến 6 (thứ Hai đến thứ Sáu) |
| `timetable.period` | INTEGER, NOT NULL, từ 1 đến 12 |
| `timetable.term` | TEXT, NOT NULL, chỉ `HK1` hoặc `HK2` |
| `timetable.school_year` | TEXT, NOT NULL, định dạng `YYYY-YYYY` |
| `timetable.room` | TEXT, nullable; nếu có thì 2–20 ký tự. Là tên phòng học/lớp học vật lý theo quy ước của trường, ví dụ `7A2` hoặc `A101` |

Ràng buộc nghiệp vụ:
- Giáo viên được phân công phải có role `TEACHER`.
- UNIQUE `(class_id, subject_id)` trong `teaching_assignments`: một môn trong một lớp chỉ có một giáo viên phụ trách.
- UNIQUE `(class_id, school_year, term, weekday, period)` trong `timetable`: một lớp không có hai môn trùng cùng tiết.
- Mỗi dòng thời khóa biểu phải tham chiếu cặp `(class_id, subject_id)` có trong `teaching_assignments`.
- `room` có thể để NULL khi chưa phân công và được cập nhật sau.
- Giáo viên chủ nhiệm đồng thời là giáo viên bộ môn vẫn được phép; hai quyền được kiểm tra theo hai quan hệ riêng.

### 15.4. Validation điểm và điểm danh

| Bảng/trường | Quy tắc |
|---|---|
| `grades.score` | REAL, NOT NULL, từ 0 đến 10; tối đa 2 chữ số sau dấu thập phân |
| `grades.term` | NOT NULL, `HK1` hoặc `HK2` |
| `grades.score_type` | NOT NULL, `TX1`, `TX2`, `GK`, `CK` |
| `grades.school_year` | TEXT, NOT NULL, định dạng `YYYY-YYYY` |
| `grades.student_id`, `subject_id` | INTEGER, NOT NULL |
| `grades.updated_by` | INTEGER, NOT NULL; người cập nhật là giáo viên được phân công đúng lớp/môn |
| `attendance.date` | TEXT, NOT NULL, lưu `YYYY-MM-DD`; chỉ ngày thứ Hai đến thứ Sáu |
| `attendance.status` | NOT NULL, `PRESENT`, `EXCUSED`, `UNEXCUSED`, `LATE` |
| `attendance.student_id`, `recorded_by` | INTEGER, NOT NULL; người ghi nhận phải là GVCN của lớp học sinh |

Quy tắc:
- Điểm chấp nhận số nguyên hoặc số thập phân tối đa 2 chữ số, ví dụ `8`, `8.5`, `8.25`; từ chối điểm âm, lớn hơn 10 hoặc quá 2 chữ số thập phân.
- Giáo viên chỉ nhập/sửa điểm khi được phân công đúng lớp và môn; học sinh phải thuộc lớp đang thao tác.
- UNIQUE `(student_id, subject_id, school_year, term, score_type)`; không UNIQUE riêng `student_id` hoặc `subject_id`.
- UNIQUE `(student_id, date)` cho điểm danh; không UNIQUE riêng `student_id`.
- Không điểm danh ngày tương lai; cho phép điểm danh bù ngày quá khứ, nhưng phải ghi `created_at` và audit log.
- Chỉ điểm danh thứ Hai đến thứ Sáu.

Hiển thị ngày điểm danh:
- Lưu/API dùng `YYYY-MM-DD`, ví dụ `2026-01-10`.
- Giao diện hiển thị dạng `Thứ bảy, ngày 10/01/2026` nếu ngày đó là thứ bảy; tuy nhiên ngày thứ bảy không hợp lệ để tạo bản ghi điểm danh.
- Tên thứ phải được tính từ ngày thực tế, không ghép chuỗi thứ do frontend tự đoán.

### 15.5. Validation và quy trình đơn xin nghỉ

| Trường | Quy tắc |
|---|---|
| `student_id` | INTEGER, NOT NULL; học sinh phải liên kết với phụ huynh gửi đơn |
| `parent_id` | INTEGER, NOT NULL, tài khoản role `PARENT` |
| `from_date`, `to_date` | TEXT, NOT NULL, ngày hợp lệ `YYYY-MM-DD`; `to_date >= from_date` |
| `reason` | TEXT, NOT NULL, trim; 10–500 ký tự sau trim |
| `status` | NOT NULL, `PENDING`, `APPROVED`, `REJECTED` |
| `reviewed_by` | Nullable khi chưa duyệt; nếu giáo viên duyệt thì phải là GVCN |
| `reviewed_at` | Nullable khi chưa duyệt; khi giáo viên duyệt/từ chối phải lưu thời điểm hợp lệ |

Quy tắc nghiệp vụ:
- Không cho phụ huynh gửi đơn có `from_date` trong quá khứ.
- Phụ huynh chỉ sửa khi đơn còn PENDING.
- Khi phụ huynh sửa đơn PENDING: chỉ kiểm tra `fromDate` không ở quá khứ nếu `fromDate` gửi lên khác giá trị đang lưu. Nếu giữ nguyên `fromDate` thì không kiểm tra lại (vẫn phải đảm bảo `toDate >= fromDate`). "Quá khứ" tính theo ngày hiện tại giờ Việt Nam.
- Giáo viên chủ nhiệm duyệt hoặc từ chối bất cứ lúc nào khi đơn PENDING

### 15.6. Validation thông báo và audit log

| Bảng/trường | Quy tắc |
|---|---|
| `announcements.title` | TEXT, NOT NULL, trim, 5–50 ký tự |
| `announcements.content` | TEXT, NOT NULL, trim, 10–1000 ký tự |
| `announcements.class_id` | INTEGER, NOT NULL |
| `announcements.author_id` | INTEGER, NOT NULL; phải là GVCN của lớp |
| `audit_logs.actor_user_id` | Người dùng thực hiện phải tồn tại |
| `audit_logs.student_id` |INTEGER, nullable, có khóa ngoại, Học sinh phải tồn tại; điểm, điểm danh, đơn nghỉ, liên kết phụ huynh thì điền; tài khoản, lớp, phân công thì để NULL. |
| `audit_logs.action` | Giữ enum `Action` đã có trong Class Diagram: `CREATE`, `UPDATE`, `DELETE`. |
| `audit_logs.entity_type` | Tên bảng hợp lệ: `users`, `classes`, `students`, `parent_students`, `subjects`, `teaching_assignments`, `timetable`, `grades`, `attendance`, `leave_requests`, `announcements` |
| `audit_logs.entity_id` | TEXT, ID của bản ghi bị tác động; không phải enum |
| `audit_logs.old_value`, `new_value` | TEXT chứa JSON hợp lệ; JSON lưu tên cột và giá trị cũ/mới. Khi xóa, `new_value = NULL`; khi tạo, `old_value = NULL` |
| `audit_logs.created_at` | TEXT, timestamp ISO 8601 UTC |

Ví dụ audit log cập nhật điểm:
```json
{
  "action": "UPDATE",
  "entity_type": "grades",
  "entity_id": "125",
  "old_value": {"score": 8.0},
  "new_value": {"score": 9.0}
}
```

### 15.7. Rà soát quyền API

Quy tắc tổng quát:
- Mọi endpoint trừ `POST /auth/login` yêu cầu session hợp lệ, trừ khi ghi rõ khác.
- Mọi quyền phải kiểm tra ở backend. ID truyền từ client không được xem là bằng chứng quyền sở hữu.
- Khi không tìm thấy tài nguyên hoặc tài nguyên nằm ngoài phạm vi truy cập, có thể trả `404` để tránh tiết lộ sự tồn tại; `403` dùng cho trường hợp đã xác định tài nguyên nhưng role không có quyền.
- ADMIN không sửa điểm, không xem dữ liệu học tập.
- TEACHER có thể đồng thời là GVCN và giáo viên bộ môn; phải kiểm tra đúng quan hệ tương ứng với từng endpoint.

| Endpoint | PARENT | TEACHER | ADMIN | Điều kiện backend bắt buộc |
|---|---:|---:|---:|---|
| `POST /auth/login` | Công khai | Công khai | Công khai | Đăng nhập bằng email + password |
| `POST /auth/logout`, `GET /auth/me` | Có | Có | Có | Session hợp lệ |
| `GET /students` | Có | Không | Không | Chỉ trả học sinh liên kết với parent đang đăng nhập |
| `GET /students/:id/grades` | Có | Không | Không | Parent phải liên kết với học sinh |
| `GET /students/:id/attendance` | Có | Không | Không  | Parent phải liên kết với học sinh |
| `GET /students/:id/timetable` | Có | Không | Không| Parent phải liên kết với học sinh |
| `GET /students/:id/leave-requests` | Có | Không | Không | Parent phải liên kết với học sinh và chỉ xem đơn của mình |
| `POST /leave-requests` | Có | Không | Không | `parentId` lấy từ session, không nhận từ client; student phải thuộc parent; fromDate không ở quá khứ |
| `PATCH /leave-requests/:id` (phụ huynh sửa) | Có | Không | Không | Chỉ sửa đơn do chính parent tạo; chỉ cho phép field `fromDate`, `toDate`, `reason`; đơn đang PENDING; nếu fromDate khác giá trị đang lưu thì phải không ở quá khứ, nếu giữ nguyên thì bỏ qua kiểm tra này|
| `GET /classes/:id/announcements` | Có | Có | Không | Parent có con trong lớp; Teacher là GVCN lớp đó |
| `GET /teacher/classes` | Không | Có | Không | Chỉ trả các lớp/môn được phân công và lớp chủ nhiệm của teacher đang đăng nhập |
| `GET /classes/:id/students` | Không | Có | Không | Teacher là GVCN hoặc có teaching assignment tại lớp |
| `GET /classes/:id/grades?subjectId=` | Không | Có | Không | Teacher phải được phân công đúng lớp/môn; học sinh trong kết quả thuộc lớp |
| `PUT /grades` | Không | Có | Không | Teacher phải được phân công đúng lớp/môn; student phải thuộc classId và subject phải được phân công cho lớp; ghi audit trong cùng transaction |
| `GET /classes/:id/attendance?date=` | Không | Có | Không | Chỉ GVCN lớp |
| `PUT /classes/:id/attendance` | Không | Có | Không | Chỉ GVCN; mọi studentId thuộc lớp; validate toàn bộ batch trước khi ghi; ngày thứ Hai–thứ Sáu, không tương lai; cho phép ngày quá khứ |
| `GET /classes/:id/leave-requests` | Không | Có | Không | Chỉ GVCN lớp |
| `PATCH /leave-requests/:id` (giáo viên duyệt) | Không | Có | Không | Chỉ GVCN của học sinh; đơn PENDING; chỉ cập nhật status/reviewer fields |
| `POST /classes/:id/announcements` | Không | Có | Không | Chỉ GVCN lớp |
| `/admin/users`, `/admin/classes`, `/admin/students` | Không | Không | Có | Chỉ ADMIN; validate role/foreign key và field allowlist |
| `POST/DELETE /admin/parent-students` | Không | Không | Có | Chỉ ADMIN; parent phải là PARENT, student tồn tại; mỗi student chỉ có một parent; ghi audit |
| `POST/DELETE /admin/teaching-assignments` | Không | Không | Có | Chỉ ADMIN; teacher phải là TEACHER; class/subject tồn tại; unique class-subject; ghi audit |
| `GET /admin/audit-logs` | Không | Không | Có | Chỉ ADMIN; lọc được theo studentId, action, entityType, entityId. |
| `GET /subjects` | Không | Có | Có | Session hợp lệ và role `TEACHER` hoặc `ADMIN`; chỉ trả `id`, `name`, `subjectCode` |
| `GET /admin/parent-students` | Không | Không | Có | Chỉ ADMIN; lọc theo `parentId`, `studentId`; validate ID là số hợp lệ; không trả dữ liệu học tập của học sinh |
| `GET /admin/teaching-assignments` | Không | Không | Có | Chỉ ADMIN; lọc theo `classId`, `teacherId`; validate ID là số hợp lệ |

Ghi chú:
- Phụ huynh sửa và giáo viên duyệt có thể dùng chung `PATCH /leave-requests/:id`, nhưng service phải phân nhánh theo role và allowlist field; tuyệt đối không cho parent tự gửi `status`, `reviewedBy`, `reviewedAt`.
- API hiện tại không có endpoint quản lý môn học/thời khóa biểu; hai phần này dùng seed data theo phạm vi dự án.
- API lọc audit log theo học sinh: dựa trên studentId.

## 16. Ghi chú kỹ thuật cần thảo luận (Spring Boot)

- **Stack:** Backend Spring Boot (Java 17+), SQLite, session cookie, không JWT. Frontend: react + rest.
- **Truy cập DB:** Spring JDBC (JdbcClient/JdbcTemplate), không dùng JPA vì SQLite không có dialect Hibernate chính thức. Bật khóa ngoại: `spring.datasource.hikari.connection-init-sql=PRAGMA foreign_keys = ON`; pool size 1 để tránh lỗi `database is locked`.
- **Validate input:** Jakarta Bean Validation (`spring-boot-starter-validation`) cho kiểu/độ dài/khoảng giá trị; quy tắc nghiệp vụ (toDate >= fromDate, không quá khứ/tương lai, thứ Hai–Sáu) đặt trong service. Cho phép tự viết nếu không làm chậm tiến độ.
- **Session:** bắt đầu bằng session Tomcat với `server.servlet.session.persistent=true` (sống qua restart bình thường). Nâng lên Spring Session JDBC nếu còn thời gian; lưu ý SQLite không có script tạo bảng chính thức, phải tự viết DDL `SPRING_SESSION`, `SPRING_SESSION_ATTRIBUTES`.
- **Cookie:** đổi tên `server.servlet.session.cookie.name=SCSID` để khỏi trùng `JSESSIONID` của dự án khác trên localhost (cookie phân theo host, không phân theo cổng). `HttpOnly`, `SameSite=Lax`, `Secure` bật khi chạy HTTPS.
- **CSRF:** tắt CSRF token mặc định của Spring Security (`csrf.disable()`, ghi chú lý do trong code); thay bằng `OncePerRequestFilter` kiểm tra `Origin` cho POST/PUT/PATCH/DELETE theo allowlist trong cấu hình (thiếu Origin hoặc không khớp thì 403), kèm SameSite=Lax và chỉ nhận `application/json` cho request ghi. Allowlist phải gồm origin của frontend khi chạy. (Spring Security mặc định bật CSRF token, tức mọi POST sẽ bị 403 nếu không gửi kèm token. Frontend phải đọc token và gắn vào header, tốn công cho đồ án nhỏ).
- **Mật khẩu:** `BCryptPasswordEncoder` hoặc `Argon2PasswordEncoder`; Spring Security tự đổi session ID sau đăng nhập.
- **Ngày giờ:** `LocalDate` cho ngày nghiệp vụ, `Instant` cho timestamp, bean `Clock` đặt `Asia/Ho_Chi_Minh` để lấy "hôm nay". Thứ trong tuần lấy từ `getDayOfWeek()`.
- **ID/điểm:** DB INTEGER, DTO `String id`; `score` là `BigDecimal` scale tối đa 2. Không bật `WRITE_NUMBERS_AS_STRINGS` toàn cục.
- **Lỗi:** `@RestControllerAdvice` + `AppException(code, status)`; trả đúng envelope §13.6, mã lỗi map 400/401/403/404/409/500.


- **Lỗi:** một `enum ErrorCode` (6 mã dưới đây, mỗi mã gắn HTTP status) và `AppException(ErrorCode, message)`. `@RestControllerAdvice` biến mọi exception thành envelope §13.6. Quy tắc chọn mã:
  - `401`: chưa xác thực hoặc đăng nhập thất bại.
  - `403`: đã đăng nhập nhưng **sai vai trò**; hoặc `Origin` không hợp lệ.
  - `404`: không tồn tại **hoặc** không có quan hệ sở hữu/liên kết/phân công/chủ nhiệm. Hai trường hợp phải trả phản hồi **y hệt nhau**.
  - `400`: dữ liệu gửi lên sai (path, query, body, quy tắc nghiệp vụ trên dữ liệu gửi lên).
  - `409`: dữ liệu đúng nhưng trạng thái hiện tại không cho phép (đã xử lý, trùng, đang được tham chiếu).
  - `500`: lỗi không lường trước.

| # | Tình huống | HTTP | `error.code` | `message` mặc định | Phát hiện / xử lý trong Spring |
|---|---|---|---|---|---|
| 1 | Không có session hoặc hết hạn (mọi endpoint trừ login) | 401 | `UNAUTHENTICATED` | Vui lòng đăng nhập. | `AuthenticationEntryPoint` tùy chỉnh (mặc định Spring Security trả 403 chứ không phải 401) |
| 2 | Đăng nhập: sai mật khẩu, email không tồn tại, tài khoản vô hiệu hóa, đang bị khóa | 401 | `UNAUTHENTICATED` | Email hoặc mật khẩu không đúng. | `AuthService`; **cùng một message** cho mọi nguyên nhân; sai 5 lần liên tiếp thì khóa 5 phút (`locked_until`) |
| 3 | Sai vai trò (PARENT gọi `/teacher/*`, `/admin/*`; TEACHER gọi `/admin/*`...) | 403 | `FORBIDDEN` | Bạn không có quyền truy cập tài nguyên này. | `@PreAuthorize("hasRole(...)")` + `AccessDeniedHandler` tùy chỉnh |
| 4 | Request ghi (POST/PUT/PATCH/DELETE) có `Origin` ngoài allowlist hoặc thiếu | 403 | `FORBIDDEN` | Yêu cầu không hợp lệ. | `OriginFilter` ghi thẳng envelope (filter chạy trước `@RestControllerAdvice`) |
| 5 | ID trên URL không tồn tại (học sinh, lớp, đơn nghỉ...) | 404 | `NOT_FOUND` | Không tìm thấy tài nguyên. | Repository trả `Optional.empty()` rồi service ném `NotFoundException` |
| 6 | Có thật nhưng không có quan hệ: học sinh không liên kết, lớp không có con, đơn nghỉ của phụ huynh khác, giáo viên không được phân công hoặc không chủ nhiệm | 404 | `NOT_FOUND` | **Y hệt dòng 5** | Điều kiện sở hữu nằm ngay trong câu SQL (`WHERE ... AND parent_id = ?`); không tra "có tồn tại" riêng rồi mới kiểm tra quyền |
| 7 | Đường dẫn API không tồn tại | 404 | `NOT_FOUND` | Không tìm thấy tài nguyên. | `NoHandlerFoundException` / `NoResourceFoundException` trong advice |
| 8 | Body sai Bean Validation (thiếu field, độ dài, định dạng, khoảng giá trị) | 400 | `VALIDATION_ERROR` | Dữ liệu không hợp lệ. | `MethodArgumentNotValidException` |
| 9 | JSON sai cú pháp, body rỗng, sai kiểu dữ liệu | 400 | `VALIDATION_ERROR` | Dữ liệu không hợp lệ. | `HttpMessageNotReadableException` |
| 10 | Body có field ngoài allowlist (`parentId`, `status`, `reviewedBy`...) | 400 | `VALIDATION_ERROR` | Dữ liệu không hợp lệ. | `spring.jackson.deserialization.fail-on-unknown-properties=true` (mặc định của Spring Boot là `false`, phải bật) |
| 11 | Path/query sai kiểu hoặc giá trị: `/students/abc`, `term=HK3`, `schoolYear=2024`, `status=ABC`, `from` lớn hơn `to` | 400 | `VALIDATION_ERROR` | Tham số không hợp lệ. | `MethodArgumentTypeMismatchException`, `ConstraintViolationException`, kiểm tra trong service |
| 12 | Quy tắc nghiệp vụ trên dữ liệu gửi lên: `toDate < fromDate`, `fromDate` quá khứ, điểm danh T7/CN/tương lai, điểm ngoài 0–10 hoặc quá 2 chữ số thập phân; batch có phần tử sai (không ghi gì) | 400 | `VALIDATION_ERROR` | Nêu quy tắc ngắn, ví dụ "Ngày bắt đầu không được ở quá khứ." | Service ném `AppException(VALIDATION_ERROR, ...)` |
| 13 | ID tham chiếu **trong body** sai: `studentId` không thuộc `classId`, `parentId` không phải PARENT, `teacherId` không phải TEACHER | 400 | `VALIDATION_ERROR` | Dữ liệu tham chiếu không hợp lệ. | Service kiểm tra role và quan hệ trước khi ghi (ID trên URL không thấy thì là 404, ID trong body sai thì là 400) |
| 14 | Method hoặc `Content-Type` không hỗ trợ (405/415); `Accept` không đáp ứng được (406) | 400 | `VALIDATION_ERROR` | Yêu cầu không được hỗ trợ. | `HttpRequestMethodNotSupportedException`, `HttpMediaTypeNotSupportedException`, `HttpMediaTypeNotAcceptableException`; gộp có chủ ý, xem ghi chú "Quyết định gộp 405/415/406" bên dưới |
| 15 | Sửa hoặc duyệt đơn nghỉ không còn `PENDING` | 409 | `CONFLICT` | Đơn đã được xử lý, không thể thay đổi. | `UPDATE ... WHERE id = ? AND status = 'PENDING'`; 0 dòng bị ảnh hưởng sau khi đã xác nhận quyền thì trả 409 (chống race) |
| 16 | Trùng dữ liệu: email, mã lớp + năm học, GVCN đã chủ nhiệm lớp khác cùng năm, mã học sinh, học sinh đã có phụ huynh, phân công trùng (lớp, môn) | 409 | `CONFLICT` | "<Trường> đã tồn tại." | Đọc mã lỗi của `SQLiteException`: `SQLITE_CONSTRAINT_UNIQUE` / `SQLITE_CONSTRAINT_PRIMARYKEY` (Spring thường bọc thành `DataIntegrityViolationException`) |
| 17 | Gỡ liên kết hoặc phân công đang được dùng (liên kết có đơn nghỉ; phân công có thời khóa biểu) | 409 | `CONFLICT` | Không thể xóa vì đang được sử dụng. | `SQLITE_CONSTRAINT_FOREIGNKEY` |
| 18 | Lỗi không lường trước (NPE, lỗi SQL, `database is locked`) | 500 | `INTERNAL_ERROR` | Đã xảy ra lỗi hệ thống. Vui lòng thử lại. | `@ExceptionHandler(Exception.class)`; log đầy đủ ở server, response không có stack hay SQL |

Frontend xử lý theo `error.code`, không so sánh chuỗi `message`:

| `error.code` | Frontend |
|---|---|
| `UNAUTHENTICATED` | Xóa phiên và chuyển về trang đăng nhập; riêng màn đăng nhập thì hiện message tại form |
| `FORBIDDEN` | Trang hoặc thông báo "Bạn không có quyền truy cập" |
| `NOT_FOUND` | Trang hoặc khung "Không tìm thấy" |
| `VALIDATION_ERROR` | Hiện message ở form/bộ lọc tương ứng |
| `CONFLICT` | Hiện message tại thao tác và tải lại dữ liệu (ví dụ đơn đã được xử lý) |
| `INTERNAL_ERROR` | Thông báo chung, cho phép thử lại, không hiện chi tiết |

Ghi chú triển khai:
- Lỗi phát sinh trong filter (như `OriginFilter`) **không** đi qua `@RestControllerAdvice`; dùng chung một lớp `ApiErrorWriter` để ghi envelope.
- Dòng 2 và 6 là hai chỗ dễ làm lộ thông tin nhất: message phải giống hệt giữa các nguyên nhân.
- Nếu form cần biết lỗi ở field nào, có thể bổ sung `error.fields: [{"field","message"}]` vào envelope. Frontend cũ chỉ đọc `code` nên không bị ảnh hưởng. Hai bạn quyết định có làm hay không.
- **Quyết định gộp 405/415/406 về 400 (có chủ ý, không phải thiếu sót):** API giữ đúng 6 mã lỗi (§16). Ba lỗi này là lỗi phía client lập trình (gọi sai phương thức HTTP, gửi sai `Content-Type`, đòi sai `Accept`), do trình duyệt hoặc công cụ gọi API gây ra, người dùng thao tác trên giao diện gần như không gặp. Nhóm nhỏ, ưu tiên nguồn lực cho phần trình bày giao diện, nên không bổ sung thêm mã và không thêm nhánh xử lý riêng ở frontend.
  - Đánh đổi: không còn đúng ngữ nghĩa chuẩn HTTP (405/415/406); công cụ ngoài đọc API sẽ thấy 400 chung.
  - Quy tắc "chỉ nhận `application/json` cho request ghi" vẫn giữ làm một lớp phòng CSRF; kết quả bị từ chối là 400 thay vì 415.
  - Không có handler riêng thì Spring trả 405/415/406 với body ngoài envelope; handler `unsupported` trong `GlobalExceptionHandler` chặn lại để mọi lỗi đều đúng envelope §13.6.
  - Nếu sau này cần đúng chuẩn: thêm mã vào `ErrorCode`, sửa handler, cập nhật dòng 14 và bảng xử lý ở frontend.
### Quy ước bổ sung về lỗi API

- `error.fields` là trường tùy chọn, chỉ trả về khi lỗi validation cần chỉ rõ các trường không hợp lệ. Nếu không cần chỉ rõ trường, chỉ trả về `error.code` và `error.message`.
- GVCN gọi endpoint không thuộc phạm vi quyền được phép thực hiện: trả về `403 Forbidden`.

### Quy ước Frontend

- **Stack:** React + JavaScript (không dùng TypeScript) + Tailwind CSS.
- **API client:** Tập trung gọi API qua module dùng chung (`src/services/api.js`). Endpoint sử dụng tiền tố `/api`; request gửi session cookie bằng `credentials: "include"`. Request ghi sử dụng JSON.
- **Session:** Khi mở ứng dụng hoặc tải lại trang, gọi `GET /api/auth/me` để kiểm tra phiên. Lấy thông tin người dùng và role từ server. Không đọc hoặc lưu session ID ở JavaScript/localStorage.
- **Phân quyền giao diện:** Sử dụng route guard và role lấy từ server để kiểm soát trang, menu và thao tác. Frontend chỉ kiểm soát hiển thị; backend vẫn phải kiểm tra quyền trên mọi API.
- **Xử lý lỗi:** Xử lý tập trung tại API client theo `error.code`. `error.fields` là tùy chọn; không giả định response luôn có trường này.
- **Request bất đồng bộ:** Sử dụng `AbortController` hoặc cơ chế tương đương để tránh response cũ ghi đè dữ liệu mới khi người dùng đổi bộ lọc hoặc chuyển trang. Có trạng thái loading, empty và error cho từng khu vực dữ liệu.
- **Thao tác ghi:** Khóa nút gửi trong lúc request đang xử lý, tránh gửi lặp. Không tự động retry request ghi khi timeout; cần đọc lại dữ liệu để xác định kết quả trước khi gửi lại.
- **Dữ liệu riêng tư:** Xóa dữ liệu người dùng khỏi state/cache khi logout hoặc khi phiên hết hạn.
- **Môi trường phát triển:** Sử dụng Vite proxy `/api` đến Spring Boot backend. Frontend gọi API qua đường dẫn tương đối `/api/...`, không hardcode host/port backend trong từng service.
- **Ngày tháng:** Ngày nghiệp vụ sử dụng chuỗi `YYYY-MM-DD`, không chuyển đổi qua UTC làm thay đổi ngày. Timestamp ISO 8601 hiển thị theo múi giờ Việt Nam (`Asia/Ho_Chi_Minh`).

# 17 seed data
- Tham khảo seed data.md

- **Mã học sinh theo quy ước Học viện:** `{AT|CT|DT}{khóa 2 số}{mã lớp 2 số}{stt 2 số}`, ví dụ AT200429. Mã lớp là chữ cái (A=01, B=02, C=03, D=04): AT20D ↔ `AT2004xx`; lớp có khóa để phân biệt (AT19D khác AT20D). Mẫu `12-10A1-An-01` ở §15.2 chỉ là ví dụ chung.
- **`schoolYear` có hai nghĩa:** của lớp là khóa học (ví dụ 2023-2028), của điểm và thời khóa biểu là năm học (`YYYY-YYYY`, ví dụ 2024-2025). UI không lấy năm mặc định từ lớp mà từ năm gần nhất có dữ liệu.
- **§15.3:** `subjects.name` 2–40 ký tự.
- **Seed:** 3 lớp AT20A, CT19B, DT21C; môn học và thời khóa biểu nạp bằng seed; mật khẩu demo chung, chỉ dùng cho demo.

# 18.Tạo API test/sample


# 19. Chốt tiêu chí nghiệm thu 
- Tham khảo tieu_chi_nghiem_thu.md

## 20. Chốt wireframe, prototype và quy ước thiết kế giao diện (2026-11-13)

### 20.1. Trạng thái thiết kế

- Đã hoàn thành wireframe/prototype Figma cho ba vai trò: `PARENT`, `TEACHER`, `ADMIN`.
- Prototype bao gồm các màn hình chính, biểu mẫu, bảng dữ liệu, hộp thoại xác nhận và các trạng thái thao tác cần thiết.
- Có bố cục desktop và mobile để làm cơ sở triển khai giao diện responsive.
- File thiết kế tham chiếu: `Nha-Truong(1).pdf` (xuất từ Figma).

### 20.2. Design system đã chốt

**Bảng màu**

| Token | Giá trị | Mục đích |
|---|---|---|
| `ink` | `#253D32` | Màu chữ chính |
| `muted` | `#627369` | Chữ phụ, mô tả |
| `green` | `#236B50` | Primary, nút hành động chính |
| `forest` | `#153F32` | Sidebar, vùng nhận diện thương hiệu |
| `canvas` | `#F5F7F3` | Nền ứng dụng |
| `white` | `#FFFFFF` | Card và vùng nội dung |
| `line` | `#DCE5DC` | Đường viền, phân cách |
| `soft` | `#EDF3ED` | Nền phụ, trạng thái nhẹ |
| `accent` | `#D0E1A7` | Màu nhấn phụ |
| `amber` | `#845E16` | Cảnh báo hoặc trạng thái cần chú ý |

**Typography và kích thước**

- Font chữ: Inter.
- Heading: 30/40; tiêu đề khu vực: 22/30; nội dung: 14/22; nhãn: 12/18.
- Spacing theo lưới 4 px; khoảng cách thường dùng: 8, 16, 24, 32 px.
- Chiều cao control tiêu chuẩn: 44 px.
- Border radius thường dùng: 8 px và 12 px.

**Nguyên tắc sử dụng màu**

- Giữ phong cách xanh rừng, nền sáng và card trắng.
- Màu primary dùng chủ yếu cho hành động chính, trạng thái được chọn và điểm nhấn cần thiết.
- Không phủ màu primary lên các vùng lớn như toàn bộ sidebar hoặc nền card.
- Dùng màu nền phụ cho trạng thái nhẹ, tránh làm giao diện quá nhiều mảng xanh.

### 20.3. Nguyên tắc triển khai giao diện

- Bám theo bố cục, tên chức năng và luồng thao tác trong prototype.
- Mỗi màn hình và thao tác phải tương ứng với role, quyền backend và API contract đã chốt tại §13–§15.
- Route, menu và nút thao tác được hiển thị theo role; đây chỉ là kiểm soát giao diện, không thay thế phân quyền backend.
- Các bảng và biểu mẫu cần có trạng thái loading, empty, error và trạng thái thao tác thành công/thất bại phù hợp.
- Các thao tác có ảnh hưởng dữ liệu cần có xác nhận khi phù hợp; không tự thêm chức năng hoặc endpoint ngoài API contract.
- Thiết kế responsive dựa trên các khung desktop 1440×1000 và mobile 390×844 trong prototype.

### 20.4. Giới hạn phạm vi khi triển khai

- Thời khóa biểu sử dụng dữ liệu seed; không xây dựng giao diện quản lý thời khóa biểu.
- Môn học sử dụng dữ liệu seed; không xây dựng giao diện CRUD môn học.
- Thông báo theo lớp là chức năng ưu tiên sau; có thể cắt nếu thiếu thời gian.
- Admin không được xem hoặc chỉnh sửa dữ liệu học tập. Màn hình audit log chỉ phục vụ truy vết theo quyền đã chốt.
- Không coi các thành phần minh họa trong prototype là yêu cầu bổ sung API. Nếu một thao tác chưa có endpoint trong contract thì phải rà soát và thống nhất trước khi triển khai.

### 20.5. Trạng thái

- Wireframe/prototype và luồng màn hình ba vai trò: **Đã chốt**.
- Design system: **Đã chốt**.
- Quy ước frontend tại §16: **Đã chốt**.
- API test/sample và tiêu chí nghiệm thu: tiếp tục hoàn thiện theo §14.


## 21. Cấu trúc thư mục dự án

Dự án sử dụng cấu trúc monorepo, tách riêng backend, frontend và tài liệu.

### Backend
- Công nghệ: Spring Boot, Java 17+, SQLite, Spring JDBC (không sử dụng JPA/Hibernate).
- Package gốc: `vn.actvn.school`.
- Tổ chức package theo chức năng: `auth`, `user`, `student`, `classroom`, `grade`, `timetable`, `attendance`, `leave`, `announcement`, `assignment`, `audit`.
- Mỗi chức năng có thể gồm:
  - `Controller`: tiếp nhận HTTP request và trả response.
  - `Service`: xử lý nghiệp vụ và phân quyền.
  - `Repository`: truy vấn SQLite bằng JDBC.
  - `dto/`: request và response DTO.
- `common/`: xử lý dùng chung như exception, response, validation và util.
- `config/`: cấu hình ứng dụng, session và web.
- `resources/db/`: chứa `schema.sql`, `seed.sql` và các file migration.
- `data/`: lưu SQLite database runtime, không commit dữ liệu runtime lên Git.

### Frontend
- Công nghệ: React, Vite, JavaScript (không sử dụng TypeScript), Tailwind CSS.
- `app/`: cấu hình ứng dụng và router.
- `services/`: tập trung API calls; sử dụng chung `api.js`, gửi cookie với `credentials: "include"`.
- `components/ui/`: component giao diện tái sử dụng.
- `components/common/`: component dùng chung như Loading, EmptyState, ErrorMessage.
- `layouts/`: AuthLayout, ParentLayout, TeacherLayout, AdminLayout.
- `features/`: tổ chức component và logic theo chức năng.
- `pages/`: tổ chức màn hình theo vai trò PARENT, TEACHER, ADMIN.
- `hooks/`, `utils/`, `constants/`, `styles/`: logic và tài nguyên dùng chung.

### Quy ước tổ chức code
- Không viết truy vấn database trong Controller.
- Không viết nghiệp vụ phức tạp trong Controller hoặc Repository.
- Không gọi API trực tiếp rải rác trong các Page; sử dụng service tương ứng.
- Các component giao diện dùng chung đặt trong `components/`.
- Chỉ tạo module, component hoặc abstraction khi có nhu cầu thực tế; tránh chia nhỏ quá mức.
- Cấu trúc thư mục không làm thay đổi API contract, phân quyền hoặc phạm vi nghiệp vụ đã thống nhất.



```text
school_management_system/               <-- ĐẶT .git Ở ĐÂY (mở thư mục này bằng VS Code)
├── .git/
├── .gitignore                          
├── docs/                               <-- Chứa ddl.md, seed_data.md, file context...
│
├── frontend/                           <-- Code React + Vite
│   ├── package.json
│   └── src/
│
└── backend/                            
    ├── pom.xml
    ├── mvnw.cmd
    ├── data/                           <-- Tự sinh file school.db khi chạy
    └── src/
        └── main/
            ├── java/
            │   └── vn/actvn/school_management_system/  
            │       ├── SchoolManagementSystemApplication.java
            │       ├── common/
            │       ├── config/
            │       ├── db/
            │       └── auth/ ...
            └── resources/
                ├── application.yml
                └── db/
                    ├── schema.sql
                    └── seed.sql
```