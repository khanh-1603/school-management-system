# Phần 3 — Mapping UI ↔ API

> Phiên bản 2.0 · 06/11/2026.
> Nguồn: `project_context_phu_huynh_nha_truong_updated.md` (04/11/2026), đặc biệt §13 và §15. Mục 15 ưu tiên hơn ví dụ cũ.
> Màn hình và luồng: [phần 2](02-wireframes-user-flows.md). Bản Markdown này thay đặc tả cũ; HTML, JSON, MJS và code prototype chưa đồng bộ.

## 1. Quy ước đọc bảng

- Endpoint viết đủ tiền tố `/api`. GET không body; POST/PUT/PATCH gửi object JSON; DELETE dùng ID trên path, không body. `{field}` trong bảng là danh sách field của object, không phải file JSON bàn giao.
- Ngoại trừ login, mọi request yêu cầu session cookie; gọi cùng origin/Vite proxy và gửi credentials. Không đưa session ID vào URL/body. Backend kiểm tra Origin cho request ghi theo context.
- ID API là string; ID tham chiếu là chuỗi số hợp lệ, backend validate và kiểm tra role/quan hệ. `entityId` audit là TEXT, không ép thành enum/số. Không ghép dữ liệu bằng tên.
- Cột Thành công mô tả `data` trong `{success:true,data:…}`. Lỗi là `{success:false,error:{code,message}}`; xử lý theo code, không so chuỗi message. Danh sách rỗng dùng `items:[]`.
- GET/PATCH/PUT/DELETE thành công 200; POST tạo bản ghi 201, login/logout 200. Các endpoint dưới đây đều đã có trong context.

**Lỗi chung, áp dụng cho mọi dòng phù hợp:** 400/VALIDATION_ERROR → lỗi trường/form/bộ lọc, giữ nháp; 401/UNAUTHENTICATED → dừng ghi, xóa cache riêng tư, về login; 403/FORBIDDEN hoặc 404/NOT_FOUND → bỏ dữ liệu không còn được phép/khả dụng và làm mới danh sách quyền. 409/CONFLICT xử lý theo từng dòng. Mạng/500/INTERNAL_ERROR → báo tại vùng liên quan, cho thử lại GET; nếu kết quả ghi chưa rõ thì đọc lại trước khi gửi lại, không tự lặp request ghi.

Loading phải đúng học sinh/lớp/môn/ngày; response cũ không ghi đè lựa chọn mới. Form khóa gửi lặp. Lookup tải lỗi thì khóa form phụ thuộc.

## 2. Đăng nhập và session

| Màn · nút/form | Method + endpoint | Query/body | Loading | Thành công và UI | Lỗi riêng cần hiển thị |
| --- | --- | --- | --- | --- | --- |
| Chung · mở app/tải lại/Thử lại kiểm tra phiên | `GET /api/auth/me` | Không | Đang kiểm tra phiên; chưa hiện dữ liệu riêng tư | 200 `user:{id,fullName,role}` → P-01/T-01/A-02 theo role server | 401 mở login là bình thường; mạng/500: “Không kiểm tra được phiên”, Thử lại, không tự kết luận hết phiên |
| AUTH-01 · Đăng nhập | `POST /api/auth/login` | `{email,password}`; email trim + chữ thường, không gửi role | Đang đăng nhập… | 200 `user:{id,fullName,role}`; server regenerate session và tạo cookie → xóa password khỏi form, vào màn theo role | 400: email 5–50/định dạng, password tối thiểu 8; 401: “Email hoặc mật khẩu không đúng”; khóa/vô hiệu hóa/rate limit: hiện theo phản hồi thực tế, không tự đặt mã lỗi/thời hạn khóa |
| Menu chung · Đăng xuất | `POST /api/auth/logout` | Không | Khóa nút Đăng xuất | 200 `{message}`; server hủy session → xóa cache riêng tư, về login | 401 cũng về login; mạng/500: “Chưa xác nhận được đăng xuất”, cho thử lại |

## 3. Phụ huynh

Tất cả dòng dưới yêu cầu `PARENT`; backend kiểm tra liên kết với học sinh. `:id` là studentId, riêng PATCH là leaveRequestId, thông báo là classId. Không dùng API admin/subjects cho phụ huynh.

| Màn · nút/form | Method + endpoint | Query/body | Loading | Thành công và UI | Lỗi riêng cần hiển thị |
| --- | --- | --- | --- | --- | --- |
| P-01/selector · tải học sinh/Thử lại | `GET /api/students` | Không | Khóa selector và chi tiết khi chưa tải được | 200 `items:[{id,studentCode,fullName,dateOfBirth,class:{id,name,classCode,schoolYear}}]` → hồ sơ/chọn con; rỗng: liên hệ trường, không gọi API con | 401; mạng/500: “Không tải được học sinh liên kết” |
| P-02 · mở/đổi con, năm, kỳ, môn | `GET /api/students/:id/grades` | Query tùy chọn `{schoolYear,term,subjectId}`; UI chọn rõ năm/kỳ | Đang tải bảng điểm | 200 `{studentId,items:[{id,subject:{id,name},scoreType,score,term,schoolYear}]}` → TX1/TX2/GK/CK; rỗng: “Chưa có điểm”; không tính trung bình | 400: năm/kỳ/môn sai; 403/404: mất quyền học sinh → tải lại `/api/students` |
| P-03 · mở/đổi con, năm, kỳ | `GET /api/students/:id/timetable` | Query tùy chọn `{schoolYear,term}`, không query tuần | Đang tải lịch | 200 `{studentId,items:[{weekday,period,subject:{id,name}}]}` → lịch cố định T2–T6, tiết 1–12; room chỉ hiện nếu response có; rỗng: “Chưa có thời khóa biểu” | 400: năm/kỳ sai; 403/404: mất quyền học sinh |
| P-04 · mở/đổi con, tháng | `GET /api/students/:id/attendance` | `{from,to}` ngày đầu/cuối tháng YYYY-MM-DD | Đang tải lịch sử | 200 `{studentId,items:[{date,status}]}` → ngày/thứ/trạng thái; thiếu bản ghi: “Chưa ghi nhận”; không có note | 400: khoảng ngày sai; 403/404: mất quyền học sinh |
| P-05 · mở/lọc/tải lại sau tạo–sửa | `GET /api/students/:id/leave-requests` | Query tùy chọn `{status}`; Tất cả thì bỏ, không gửi ALL | Đang tải đơn | 200 `{studentId,items:[{id,fromDate,toDate,reason,status,createdAt}]}` → chi tiết local, chỉ đơn của mình PENDING có Sửa; rỗng: “Chưa có đơn phù hợp” | 400: trạng thái sai; 403/404: mất quyền/liên kết |
| P-06 tạo · Gửi đơn | `POST /api/leave-requests` | `{studentId,fromDate,toDate,reason}`; không parentId/status/reviewer | Đang gửi đơn… | 201 `leaveRequest:{id,studentId,status}` → đóng form, báo đã gửi, tải P-05 | 400: ngày sai/fromDate quá khứ/toDate trước fromDate/lý do sau trim ngoài 10–500; 403/404: mất liên kết; 409 nếu có thì nêu theo server, không tự đặt quy tắc trùng đơn |
| P-06 sửa · Lưu thay đổi | `PATCH /api/leave-requests/:id` | **Chỉ `{fromDate,toDate,reason}`**; không studentId/parentId/status/reviewedBy/reviewedAt | Đang lưu thay đổi… | 200 `leaveRequest:{id,fromDate,toDate,reason}` → đóng form, tải P-05 lấy trạng thái mới | 400: ngày/lý do sai; 403/404: không phải đơn của mình hoặc mất liên kết; **409: “Đơn đã được xử lý, không thể sửa”**, tải lại và tắt sửa; 400 nếu fromDate bị đổi sang ngày quá khứ |
| P-07 · mở/đổi lớp theo con | `GET /api/classes/:id/announcements` | id = student.class.id, không query/body | Đang tải thông báo | 200 `items:[{id,title,content,createdAt}]` → danh sách/chi tiết local; rỗng: “Lớp chưa có thông báo” | 403/404: không còn con trong lớp → làm mới học sinh |

P-03 làm sau chức năng bắt buộc; P-07 khi giữ thông báo. Form đơn luôn hiện học sinh áp dụng, ngày và bộ đếm lý do 10–500. Server lấy parentId từ session, chỉ cho sửa đơn của chính parent còn PENDING.

## 4. Giáo viên và GVCN

Role tài khoản là `TEACHER`; điều kiện bộ môn/GVCN được backend kiểm tra riêng cho từng endpoint.

| Màn · nút/form · quyền | Method + endpoint | Query/body | Loading | Thành công và UI | Lỗi riêng cần hiển thị |
| --- | --- | --- | --- | --- | --- |
| T-01 · mở/làm mới · TEACHER | `GET /api/teacher/classes` | Không | Đang tải lớp/môn | 200 `items:[{classId,className,subjectId,subjectName,isHomeroom}]` → gộp lớp, giữ môn riêng; lớp chỉ chủ nhiệm có subjectId/subjectName null, isHomeroom true; rỗng: “Chưa được phân công” | 403: sai role; mạng/500: chưa cho thao tác theo phân công chưa tải được |
| T-02; roster T-03/T-04 · đổi lớp · có phân công hoặc GVCN lớp | `GET /api/classes/:id/students` | id = classId, không query/body | Đang tải học sinh | 200 `items:[{id,fullName,studentCode}]` → roster; rỗng thì khóa ghi | 403/404: mất quyền lớp → tải T-01 |
| T-03 · tải/đổi lớp, môn, năm, kỳ · đúng phân công | `GET /api/classes/:id/grades` | Bắt buộc `subjectId`; thêm `{term,schoolYear}`, tùy chọn `scoreType` | Đang tải điểm; giữ nháp đúng ngữ cảnh | 200 `{classId,subjectId,items:[{studentId,studentName,grades:[{id,scoreType,score}]}]}` theo tên chuẩn mục 15. Ghép bằng studentId/scoreType, thiếu điểm là `—` | 400: bộ lọc; 403/404: mất phân công → dừng ghi, tải T-01 |
| T-03 · Lưu · đúng lớp/môn, học sinh thuộc lớp | `PUT /api/grades` | **Mỗi ô** `{classId,subjectId,studentId,scoreType,term,schoolYear,score}`; score number 0–10, tối đa 2 chữ số thập phân | Đang lưu x/y ô…; khóa gửi lặp | 200 mỗi ô `grade:{id,studentId,subjectId,scoreType,score}` → cập nhật ô thành công, giữ ô lỗi; không nguyên tử cả bảng. Mỗi điểm + audit cùng transaction | 400: lỗi tại ô; 403/404: dừng các ô còn lại/làm mới quyền; mạng/500: giữ ô chưa xác nhận, không rollback ô đã lưu; không gửi null/rỗng để xóa điểm |
| T-04 · chọn lớp/ngày · GVCN lớp | `GET /api/classes/:id/attendance` | Query bắt buộc `{date}`; UI chỉ ngày T2–T6, không tương lai | Đang tải ngày; khóa lưu dữ liệu ngày cũ | 200 `{classId,date,items:[{studentId,studentName,status}]}` → ghép roster; chưa có bản ghi thì chờ chọn trạng thái | 400: date sai; 403/404: mất quyền chủ nhiệm |
| T-04 · Lưu · GVCN lớp | `PUT /api/classes/:id/attendance` | `{date,items:[{studentId,status}]}`, không note; tất cả ID thuộc lớp | Đang lưu điểm danh… | 200 `{date,updatedCount}` → báo số ghi nhận, tải lại ngày. **Validate toàn bộ lô trước ghi**, một dòng sai không ghi dở dang | 400: T7/CN/tương lai/trạng thái/phần tử sai → giữ cả nháp; 403/404: mất quyền; timeout: đọc lại đúng ngày trước retry |
| T-05 · mở/lọc · GVCN lớp | `GET /api/classes/:id/leave-requests` | Query tùy chọn `{status}`, bỏ khi Tất cả | Đang tải đơn | 200 `items:[{id,studentId,studentName,fromDate,toDate,reason,status}]` → PENDING có Duyệt/Từ chối, còn lại chỉ đọc | 400: status sai; 403/404: không là GVCN |
| T-05 · Xác nhận Duyệt/Từ chối · GVCN của học sinh, đơn PENDING | `PATCH /api/leave-requests/:id` | Duyệt `{status:"APPROVED"}`; từ chối `{status:"REJECTED"}`; **chỉ status**, không reviewNote; reviewer/time do server | Khóa cả hai nút; Đang xử lý… | 200 `leaveRequest:{id,status,reviewedAt}` → đóng xác nhận, tải T-05; xử lý bất cứ lúc nào còn PENDING | 400: status sai; 403/404: ngoài lớp; **409: “Đơn đã được xử lý”**, tải lại và chỉ đọc, không ghi đè |
| T-06 · mở bảng tin · GVCN lớp | `GET /api/classes/:id/announcements` | Không query/body | Đang tải thông báo | 200 `items:[{id,title,content,createdAt}]` → danh sách; rỗng vẫn cho đăng. Quyền GET đã có §13.3/15.7 | 403/404: mất quyền chủ nhiệm |
| T-06 · Đăng · GVCN lớp | `POST /api/classes/:id/announcements` | `{title,content}`; class từ path, author từ session | Đang đăng… | 201 `announcement:{id,classId,title}` → đóng form, tải bảng tin lấy nội dung/thời điểm | 400: title trim 5–50/content trim 10–1000; 403/404: ngoài lớp; timeout: kiểm tra bảng tin trước đăng lại |

Bù điểm danh quá khứ được phép; backend vẫn ghi created_at và audit log. GVCN không dạy môn không gọi API điểm. T-06 cắt cùng P-07 nếu thiếu thời gian.

## 5. Admin

Tất cả dòng dưới chỉ `ADMIN`. GET danh sách liên kết, phân công và danh mục môn đều đã có trong context.

| Màn · nút/form | Method + endpoint | Query/body | Loading | Thành công và UI | Lỗi riêng cần hiển thị |
| --- | --- | --- | --- | --- | --- |
| A-02; picker · tìm/lọc tài khoản | `GET /api/admin/users` | Query tùy chọn `{role,search}`; picker đúng role | Đang tải bảng/picker | 200 `items:[{id,email,fullName,role,isActive}]` → bảng/picker; rỗng: “Không có tài khoản phù hợp” | 400: bộ lọc; 403: sai role; lookup lỗi khóa form phụ thuộc |
| A-02 · Tạo | `POST /api/admin/users` | `{email,password,fullName,role}`; UI tạo PARENT/TEACHER theo phạm vi | Đang tạo… | 201 `user:{id,email,fullName,role}` → đóng form, xóa password, tải lại | 400: tên/email 5–50, email sai, password dưới 8, role sai; 409: “Email đã được sử dụng” tại ô email |
| A-02 · Lưu tên / Kích hoạt–Vô hiệu hóa / Đặt lại mật khẩu / Xác nhận mở khóa | `PATCH /api/admin/users/:id` | Theo nút: `{fullName}`, `{isActive:true/false}` hoặc hai field thay đổi; reset `{password}`; unlock `{unlock:true}`. Allowlist **fullName,isActive,password,unlock**; không đổi email/role | Khóa form/dòng; Đang lưu… / Đang đặt lại… / Đang mở khóa… | 200 `user` có id/field cập nhật → tải lại, báo đúng hành động. Unlock xóa failed_logins/locked_until, không tự đổi isActive. Không trả/hiện mật khẩu | 400: tên/password sai; 404: tài khoản không còn khả dụng; 409 nếu server báo xung đột. Không suy ra bị khóa từ isActive, không log password |
| A-03; picker · tải/tìm lớp | `GET /api/admin/classes` | Query tùy chọn `{search}` | Đang tải bảng/picker | 200 `items:[{id,name,classCode,schoolYear,homeroomTeacherId}]` theo ví dụ nguồn; tên GVCN ghép users | 400: query; lookup lỗi khóa form phụ thuộc |
| A-03 · Tạo | `POST /api/admin/classes` | `{name,classCode,schoolYear,homeroomTeacherId}` | Đang tạo lớp… | 201 `class:{id,name}` → đóng form, tải lại | 400: name 2–20/classCode 2–10/năm YYYY-YYYY/GVCN bắt buộc đúng role; 409: trùng mã–năm hoặc GVCN đã chủ nhiệm lớp khác cùng năm |
| A-03 · Lưu sửa | `PATCH /api/admin/classes/:id` | Field thay đổi từ `{name,classCode,schoolYear,homeroomTeacherId}` | Đang lưu lớp… | 200 `class:{id,name,…}` → đóng form, tải lại | 400: field/tham chiếu; 404: đối tượng không còn khả dụng; 409: trùng mã–năm/GVCN–năm |
| A-04; picker · tìm/lọc học sinh | `GET /api/admin/students` | Query tùy chọn `{classId,search}` | Đang tải bảng/picker | 200 `items:[{id,studentCode,fullName,dateOfBirth,classId}]` → bảng/form có ngày sinh, ghép lớp từ danh mục | 400: query; lookup lỗi khóa liên kết cần học sinh |
| A-04 · Tạo | `POST /api/admin/students` | `{studentCode,fullName,dateOfBirth,classId}`, ngày sinh bắt buộc | Đang tạo hồ sơ… | 201 `student:{id,studentCode,fullName,dateOfBirth,classId}` → đóng form, tải lại | 400: thiếu mã/tên/lớp/ngày sinh, tên ngoài 5–50, ngày sinh sai/tương lai; 409: mã trùng; 404: lớp không còn khả dụng |
| A-04 · Lưu sửa | `PATCH /api/admin/students/:id` | Field thay đổi từ `{fullName,dateOfBirth,classId}` ; không tự thêm xử lý lịch sử chuyển lớp | Đang lưu hồ sơ… | 200 `student:{id,fullName,classId,…}` → tải lại lấy đủ hồ sơ | 400: field/ngày sinh/lớp; 404: bản ghi không còn khả dụng |
| A-05 · mở/lọc liên kết | `GET /api/admin/parent-students` | Query tùy chọn `{parentId,studentId}` | Đang tải liên kết | 200 `items:[{parentId,studentId,relationship}]` → ghép tên; rỗng: “Chưa có liên kết phù hợp” | 400: ID sai; không trả/gọi dữ liệu học tập để bổ sung nhãn |
| A-05 · Tạo | `POST /api/admin/parent-students` | `{parentId,studentId,relationship}`; relationship bắt buộc PARENT/GUARDIAN | Đang tạo liên kết… | 201 `link:{parentId,studentId,relationship}` → đóng form, tải lại; backend ghi audit | 400: quan hệ thiếu/sai, parent sai role, ID sai; 404: đối tượng không còn khả dụng; 409: học sinh đã có phụ huynh |
| A-05 · Xác nhận gỡ | `DELETE /api/admin/parent-students/:parentId/:studentId` | Hai ID từ dòng, không body | Đang gỡ… | 200 `{deleted:true}` → đóng xác nhận, tải lại; backend ghi audit | 404: liên kết không còn khả dụng → tải bảng; timeout: đối chiếu trước retry |
| A-06 · mở/lọc phân công | `GET /api/admin/teaching-assignments` | Query tùy chọn `{classId,teacherId}` | Đang tải phân công | 200 `items:[{teacherId,classId,subjectId}]` → ghép tên; rỗng: “Chưa có phân công phù hợp” | 400: ID sai; 403: sai role |
| A-06 · Tạo | `POST /api/admin/teaching-assignments` | `{teacherId,classId,subjectId}`, đủ ba ID; subjectId không null | Đang phân công… | 201 `assignment:{teacherId,classId,subjectId}` → đóng form, tải lại; backend ghi audit | 400: teacher sai role/field sai; 404: đối tượng không còn khả dụng; 409: lớp–môn đã có giáo viên |
| A-06 · Xác nhận gỡ | `DELETE /api/admin/teaching-assignments/:teacherId/:classId/:subjectId` | Ba ID từ dòng, không body | Đang gỡ phân công… | 200 `{deleted:true}` → tải lại; backend ghi audit | 404: phân công không còn khả dụng; 409 nếu server báo ràng buộc tham chiếu thì nêu lỗi, không tự xóa dây chuyền |
| A-06 · nạp môn | **`GET /api/subjects`** | Không query/body. TEACHER cũng được gọi, PARENT không được | Đang tải môn; khóa gửi phân công | 200 `items:[{id,name,subjectCode}]` → picker chỉ đọc, không CRUD môn | 403: sai role; rỗng/lỗi tải: chưa cho tạo phân công |
| A-07 · Áp dụng lọc / Xóa lọc / Thử lại | `GET /api/admin/audit-logs` | Query tùy chọn **`{studentId,action,entityType,entityId}`**; bỏ field rỗng | Đang tải nhật ký | 200 `items:[{id,actorId,action,entityType,entityId,oldValue,newValue,createdAt}]` → bảng/chi tiết local chỉ đọc; rỗng: “Không có nhật ký phù hợp” | 400: studentId/action/entityType/query sai; 403: sai role; không sửa/xóa/xuất log |

**Lookup phụ thuộc:** lớp cần users(role=TEACHER); hồ sơ cần classes; liên kết cần users(role=PARENT) + students, thêm classes nếu hiện tên lớp; phân công cần users(role=TEACHER) + classes + subjects. Không tải mọi danh mục khi mở mỗi trang. Sau tạo/gỡ, làm mới log khi mở lại A-07.

## 6. Validation và tương tác local

### 6.1. Giá trị thống nhất

| Nhóm | Giá trị / giới hạn |
| --- | --- |
| Tài khoản | fullName trim 5–50; email trim + lowercase, 5–50, đúng định dạng; password ít nhất 8, không trim. Tên tài khoản cho chữ Việt, dấu cách, gạch nối. Role PARENT/TEACHER/ADMIN. |
| Lớp | name 2–20; classCode bắt buộc 2–10; schoolYear YYYY-YYYY; GVCN TEACHER bắt buộc, duy nhất theo năm; mã lớp duy nhất theo năm. |
| Học sinh | studentCode TEXT bắt buộc/duy nhất, mẫu `12-10A1-An-01` theo Niên khóa-lớp-firstName-stt; fullName trim 5–50; dateOfBirth bắt buộc, ngày thật không tương lai; classId bắt buộc. |
| Liên kết | relationship **PARENT / GUARDIAN**, NOT NULL; mỗi học sinh chỉ một tài khoản phụ huynh liên kết. |
| Điểm | scoreType **TX1 / TX2 / GK / CK**; term **HK1 / HK2**; score số 0–10, tối đa 2 chữ số thập phân. |
| Điểm danh | **PRESENT** = Có mặt; **EXCUSED** = Nghỉ có phép; **UNEXCUSED** = Nghỉ không phép; **LATE** = Đi muộn. Chỉ T2–T6, không tương lai; được bù quá khứ. Không note. |
| Đơn nghỉ | PENDING/APPROVED/REJECTED. Ngày hợp lệ, toDate >= fromDate; tạo mới fromDate không quá khứ; reason trim 10–500. Parent sửa đơn của mình PENDING; GVCN xử lý PENDING không có deadline tự đặt. |
| Thông báo | title trim 5–50, content trim 10–1000; class bắt buộc, author GVCN. Form chỉ gửi title/content. |
| Audit | action CREATE/UPDATE/DELETE. entityType: `users`, `classes`, `students`, `parent_students`, `subjects`, `teaching_assignments`, `timetable`, `grades`, `attendance`, `leave_requests`, `announcements`. entityId TEXT. studentId nullable theo nghiệp vụ, không loại log không có học sinh. |
| Ngày / lịch | Ngày nghiệp vụ YYYY-MM-DD, không đổi múi giờ; timestamp ISO 8601 UTC hiển thị giờ Việt Nam. Timetable weekday 2–6, period 1–12, lọc schoolYear + term; room nullable, nếu có 2–20 ký tự; không UI quản lý lịch. |

Frontend hỗ trợ nhập đúng; backend vẫn validate toàn bộ và kiểm tra quyền/đối tượng. Error chỉ có code/message thì hiện lỗi form; không giả định có fieldErrors chưa ghi trong contract. Form có giới hạn/bộ đếm tương ứng; không tự làm tròn điểm quá 2 chữ số để vượt validation.

### 6.2. Nút không gọi API ghi

| Tương tác | API / hành vi |
| --- | --- |
| Hiện/ẩn password; menu; Hủy form; mở/đóng chi tiết đơn, thông báo, log | Không request ghi; chi tiết từ dòng đã tải. Không tự thêm API đọc chi tiết/đánh dấu đã đọc. |
| Menu, chọn con/lớp/môn/năm/kỳ/ngày/tháng/trạng thái | Điều hướng/đổi lựa chọn rồi GET đúng bảng nếu cần; không gọi khi thiếu ID hợp lệ; bỏ query rỗng, bỏ response cũ. |
| Nhập/sửa điểm; chọn trạng thái điểm danh; Chọn tất cả có mặt | Chỉ sửa nháp; Lưu mới PUT. Hủy thay đổi khôi phục dữ liệu đã tải, không gọi API xóa. |
| Tìm tên/mã roster T-02 | Lọc local; không thêm query search vào `/api/classes/:id/students`. |
| Mở Tạo/Sửa/Đặt lại password hoặc mở xác nhận Gỡ/Mở khóa/Duyệt/Từ chối | Chỉ mở form/xác nhận; nút xác nhận cuối gọi method/body tương ứng. |
| Thử lại | Lặp GET lỗi với bộ lọc hiện tại; ghi timeout thì đối chiếu trước khi người dùng gửi lại. |


Đây là đặc tả Markdown để rà soát/tích hợp, chưa phải API test, seed data hoặc tính năng đã chạy. Không sinh lại từ `ui-spec.mjs` cũ vì sẽ ghi đè sửa đổi lần này.
