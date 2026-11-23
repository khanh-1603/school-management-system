# 1. Mã nhóm

|Mã nhóm | Chức năng	| Vai trò chính |
|--|--|--|
|AUTH	| Đăng nhập, đăng xuất, phiên	| Mọi vai trò |
|STU |	Danh sách và thông tin học sinh	| Phụ huynh, giáo viên |
|GR |	Điểm |	Phụ huynh xem, giáo viên nhập |
|ATT |	Điểm danh |	Phụ huynh xem, GVCN ghi |
|TT |	Thời khóa biểu |	Phụ huynh |
|LR |	Đơn nghỉ	| Phụ huynh tạo/sửa, GVCN duyệt |
|ANN |	Thông báo lớp	| GVCN, phụ huynh |
|USR, CLS, STD, LNK, TA	| Admin: tài khoản, lớp, học sinh, liên kết, phân công |	Admin |
|AUD |	Nhật ký kiểm toán |	Admin |

# 2. Phụ huynh

## Quy ước
- Tài khoản: phNN là phNN@actvn.edu.vn, mật khẩu Demo@2026. ph01 là user 8, có con 1 (AT200101, lớp 1) và 15 (DT210305, lớp 3).
- Loại ca: H = happy path, Q = bị từ chối quyền, D = dữ liệu sai, X = xung đột.
- T là ngày chạy test theo giờ Việt Nam (T+1 là ngày mai).
- Thứ tự và dữ liệu: các ca tạo/sửa đơn làm thay đổi DB, nên chạy theo thứ tự trong bảng hoặc nạp lại seed trước mỗi lượt.
- “như GR-08”, nghĩa là phản hồi phải giống hệt (cùng status, cùng error.code, cùng message).

Mình đã kiểm tra các con số kỳ vọng với seed mới, giờ ghi nhận quy ước mã rồi gửi bảng phụ huynh.

Đây là bảng tiêu chí nghiệm thu cho **phụ huynh**, dựa trên §13, §15 và seed bản mới (đuôi `@actvn.edu.vn`, năm +1). Các con số kỳ vọng mình đã đối chiếu bằng cách chạy seed.

## Quy ước

- **Tài khoản:** `phNN` là `phNN@actvn.edu.vn`, mật khẩu `Demo@2026`. `ph01` là user 8, có con 1 (AT200101, lớp 1) và 15 (DT210305, lớp 3).
- **Loại ca:** H = happy path, Q = bị từ chối quyền, D = dữ liệu sai, X = xung đột.
- **`T`** là ngày chạy test theo giờ Việt Nam (`T+1` là ngày mai).
- **Thứ tự và dữ liệu:** các ca tạo/sửa đơn làm thay đổi DB, nên chạy theo thứ tự trong bảng hoặc nạp lại seed trước mỗi lượt.
- Khi mình ghi *"như GR-08"*, nghĩa là phản hồi phải **giống hệt** (cùng status, cùng `error.code`, cùng `message`).

## Năm điểm cần hai bạn chốt trước khi dùng

1. **Ngoài phạm vi trả 404, sai vai trò trả 403** (đề xuất). §15.7 cho phép cả hai. Học sinh chưa liên kết và học sinh không tồn tại phải trả y hệt nhau, để không lộ sự tồn tại.
2. **Client gửi field ngoài allowlist** (`parentId`, `status`…): đề xuất trả 400 `VALIDATION_ERROR`, không âm thầm bỏ qua.
3. **Khóa tài khoản:** context giữ `failed_logins` và `locked_until` nhưng chưa nêu ngưỡng. Đề xuất 5 lần sai liên tiếp, khóa 15 phút, trả 401 cùng thông báo chung (seed `ph.bikhoa` đã có `failed_logins = 5`).
4. **Không có query `schoolYear`/`term`:** đề xuất trả tất cả, không mặc định. Đây là điểm đã nói với bạn làm Figma về năm mặc định.
5. **Origin lạ trên request ghi:** đề xuất 403 `FORBIDDEN`.

(Phản hồi mẫu và quyết định ở năm điểm đầu là đề xuất)

## AUTH: đăng nhập, phiên

| Mã | Loại | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| AUTH-01 | H | ph02 | `POST /auth/login` đúng email + mật khẩu | 200; `data.user = {id:"9", fullName:"Trần Thị Lan", role:"PARENT"}`; cookie `SCSID` có `HttpOnly`, `SameSite=Lax`; body không có hash hay session ID |
| AUTH-02 | H | ph02 | login với email `" PH02@ACTVN.EDU.VN "` | 200 (email trim và chuyển chữ thường) |
| AUTH-03 | H | ph02 | login lần nữa khi đang có cookie cũ | Session ID mới khác ID cũ |
| AUTH-04 | D | ph02 | sai mật khẩu | 401 `UNAUTHENTICATED`, thông báo chung |
| AUTH-05 | D | không tồn tại | email không có trong DB | Y hệt AUTH-04 |
| AUTH-06 | D | ph02 | mật khẩu sai chữ hoa/thường (`demo@2026`) | 401 |
| AUTH-07 | D | — | thiếu `email` hoặc `password` | 400 `VALIDATION_ERROR` |
| AUTH-08 | Q | ph.vohieuhoa | đúng mật khẩu | 401 cùng thông báo chung (không lộ tài khoản bị vô hiệu) |
| AUTH-09 | Q | ph.bikhoa | đúng mật khẩu | 401 cùng thông báo chung |
| AUTH-10 | X | tài khoản test | sai mật khẩu liên tiếp đến ngưỡng, rồi nhập đúng | Từ chối cả khi nhập đúng trong lúc khóa; sau hết khóa đăng nhập đúng thì `failed_logins = 0` *(chờ chốt điểm 3)* |
| AUTH-11 | H | ph02 | `GET /auth/me` sau login | 200, đúng user |
| AUTH-12 | H | ph02 | `POST /auth/logout`, rồi `GET /auth/me` bằng cookie cũ | Logout 200 "Đã đăng xuất"; `/auth/me` sau đó 401 |
| AUTH-13 | Q | — | `GET /auth/me` và `GET /students` không cookie | 401 `UNAUTHENTICATED` |

## STU: danh sách học sinh

| Mã | Loại | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| STU-01 | H | ph01 | `GET /students` | 200, 2 mục: `"1"` AT200101 lớp AT20A (`2023-2028`, sinh 2005-03-12) và `"15"` DT210305 lớp DT21C (`2024-2029`, sinh 2006-08-04); không có học sinh khác |
| STU-02 | H | ph09 | `GET /students` | 1 mục `"12"` DT210302 (quan hệ `GUARDIAN` có quyền như `PARENT`) |
| STU-03 | H | ph11 | `GET /students` | 200 `items: []` (không 404, không `null`) |
| STU-04 | Q | ph02 | `GET /students` | Chỉ `"2"`, không có `"1"` |

## GR: điểm

| Mã | Loại | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| GR-01 | H | ph01 | `GET /students/1/grades` | 200, 48 mục (12 môn × 4 đầu điểm); mỗi mục có `subject{id,name}`, `scoreType`, `score`, `term`, `schoolYear="2024-2025"` |
| GR-02 | H | ph01 | `…/grades?term=HK1` | 24 mục, đều `HK1` |
| GR-03 | H | ph01 | `…/grades?term=HK1&subjectId=1` | 4 mục (Triết): TX1 8.5, TX2 8.0, GK 8.5, CK 9.0 |
| GR-04 | H | ph01 | `GET /students/15/grades` | 33 mục: không có CK, không có Tư tưởng Hồ Chí Minh |
| GR-05 | H | ph01 | `…/grades?schoolYear=2023-2024` | 200 `items: []` |
| GR-06 | D | ph01 | `?term=HK3`; `?schoolYear=2024`; `?subjectId=abc` | Mỗi ca 400 `VALIDATION_ERROR` |
| GR-07 | D | ph01 | `/students/abc/grades`; `/students/1%20OR%201=1/grades` | 400, không 500, không trả dữ liệu |
| GR-08 | Q | ph02 | `GET /students/1/grades` (con người khác) | 404 `NOT_FOUND`, không lộ điểm |
| GR-09 | Q | ph02 | `GET /students/5/grades` (có thật, chưa liên kết) | Như GR-08 |
| GR-10 | Q | ph02 | `GET /students/9999/grades` (không tồn tại) | Như GR-08 |
| GR-11 | Q | ph11 | `GET /students/1/grades` (chưa có con nào) | Như GR-08 |

## ATT: điểm danh

| Mã | Loại | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| ATT-01 | H | ph01 | `GET /students/1/attendance` | 10 mục (T2–T6 của 14 ngày gần nhất), không có hôm nay; `date` dạng `YYYY-MM-DD`, `status` thuộc enum |
| ATT-02 | H | ph01 | `?from=T-7&to=T-1` | Chỉ ngày trong khoảng, gồm cả hai đầu |
| ATT-03 | H | ph02 | `GET /students/2/attendance` | Ngày thường thuộc đơn nghỉ đã duyệt (T-6 đến T-5) là `EXCUSED` |
| ATT-04 | H | ph01 | `?from=T+7` | 200 `items: []` |
| ATT-05 | H | ph01 | khoảng bao gồm cuối tuần | Không có thứ Bảy/Chủ nhật trong kết quả |
| ATT-06 | D | ph01 | `from > to`; `from=10/11/2026`; `from=2026-02-30` | Mỗi ca 400 `VALIDATION_ERROR` |
| ATT-07 | Q | ph02 | `/students/1/attendance`, `/students/5/…`, `/students/9999/…` | Cả ba y hệt GR-08 |

## TT: thời khóa biểu

| Mã | Loại | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| TT-01 | H | ph01 | `/students/1/timetable?schoolYear=2024-2025&term=HK1` | 42 mục (14 buổi × 3 tiết); `weekday` 2–6, `period` 1–12; Tiếng Anh 1 có 9 dòng, Triết 6 dòng; không trùng cặp `(weekday, period)` |
| TT-02 | H | ph01 | `…&term=HK2` | 39 mục |
| TT-03 | H | ph01 | `/students/15/timetable?…&term=HK2` | 33 mục, không có Tư tưởng Hồ Chí Minh |
| TT-04 | H | ph01 | `/students/1/timetable` không query | 81 mục *(chờ chốt điểm 4)* |
| TT-05 | H | ph01 | `?schoolYear=2023-2024&term=HK1` | 200 `items: []` |
| TT-06 | D | ph01 | `?term=HK3`; `?schoolYear=2024` | 400 `VALIDATION_ERROR` |
| TT-07 | H | ph02 | `/students/2/timetable?…&term=HK1` | Giống TT-01 (cùng lớp AT20A) |
| TT-08 | Q | ph02 | `/students/1/…`, `/students/5/…`, `/students/9999/…` (nghĩa là học sinh không phải con) | Y hệt GR-08 |

Với ph02, `/students/1/timetable` cũng bị từ chối dù học sinh 1 cùng lớp với con của ph02, vì quyền kiểm tra theo liên kết, không theo lớp.

## LR: đơn nghỉ

Dữ liệu seed: đơn 1 (ph01, học sinh 1, PENDING), 2 (ph01, học sinh 15, PENDING), 3 (ph03, HS 3, PENDING), 4 (ph06, HS 7, PENDING), 5 (ph02, HS 2, APPROVED), 6 (ph08, HS 11, APPROVED), 7 (ph05, HS 6, REJECTED).

| Mã | Loại | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| LR-01 | H | ph01 | `GET /students/1/leave-requests` | 1 mục PENDING (đơn 1) có `id, fromDate, toDate, reason, status, createdAt` |
| LR-02 | H | ph01 | `?status=PENDING`; `?status=APPROVED` | Lần lượt 1 mục và `items: []` |
| LR-03 | D | ph01 | `?status=ABC` | 400 `VALIDATION_ERROR` |
| LR-04 | Q | ph02 | `GET /students/1/leave-requests` | Như GR-08 |
| LR-05 | H | ph04 | `POST /leave-requests` `{studentId:"4", fromDate:T+1, toDate:T+2, reason:"Con bị sốt, xin nghỉ học"}` | 201, `leaveRequest {id, studentId:"4", status:"PENDING"}`; trong DB `parent_id = 11` (lấy từ session) |
| LR-06 | H | ph04 | `GET /students/4/leave-requests` sau LR-05 | Có đơn mới ở `PENDING` |
| LR-07 | H | ph04 | `POST` với `fromDate = toDate = T` | 201 (hôm nay không phải quá khứ) |
| LR-08 | D | ph04 | `fromDate = T-1` | 400 `VALIDATION_ERROR`, không tạo đơn |
| LR-09 | D | ph04 | `toDate` nhỏ hơn `fromDate` | 400 |
| LR-10 | D | ph04 | `reason` 9 ký tự; toàn khoảng trắng; 501 ký tự | Mỗi ca 400 |
| LR-11 | H | ph04 | `reason` đúng 10 ký tự sau trim; đúng 500 ký tự | Mỗi ca 201 |
| LR-12 | D | ph04 | `fromDate="2026-02-30"`; `"02/11/2026"`; thiếu `fromDate` | Mỗi ca 400 |
| LR-13 | D | ph04 | `POST` kèm `status:"APPROVED"`, hoặc `parentId`, `reviewedBy` | 400 *(điểm 2)*, không tạo đơn |
| LR-14 | Q | ph04 | `POST` với `studentId:"1"` (con người khác) | 404 `NOT_FOUND`, không tạo đơn |
| LR-15 | Q | ph04 | `POST` với `studentId:"5"` (chưa liên kết) hoặc `"9999"` | Như LR-14 |
| LR-16 | Q | ph04 | `POST /leave-requests` với header `Origin: https://evil.example` | 403 `FORBIDDEN`, không tạo đơn *(điểm 5)* |
| LR-17 | H | ph01 | `PATCH /leave-requests/1` `{reason:"Con bị sốt cao, đã có giấy hẹn khám"}` | 200; `reason` đổi; `fromDate`, `toDate` giữ nguyên; `status` vẫn `PENDING` |
| LR-18 | H | ph01 | `PATCH /leave-requests/2` đổi cả hai ngày sang `T+3` | 200 |
| LR-19 | H | ph01 | Đơn có `fromDate` đã qua *(chuẩn bị: sửa trực tiếp DB)*, `PATCH` chỉ đổi `reason`, giữ nguyên `fromDate` | 200 (đúng quy tắc sửa đơn đã chốt) |
| LR-20 | D | ph01 | `PATCH` đổi `fromDate` sang `T-1` (khác giá trị đang lưu) | 400 |
| LR-21 | D | ph01 | `PATCH` với `toDate` nhỏ hơn `fromDate` hiệu lực; `reason` ngoài 10–500 | Mỗi ca 400 |
| LR-22 | D | ph01 | `PATCH` kèm `status`, `reviewedBy` hoặc `reviewedAt` | 400 *(điểm 2)*, đơn không đổi |
| LR-23 | X | ph02 | `PATCH /leave-requests/5` (đã `APPROVED`) | 409 `CONFLICT` |
| LR-24 | X | ph05 | `PATCH /leave-requests/7` (đã `REJECTED`) | 409 `CONFLICT` |
| LR-25 | Q | ph02 | `PATCH /leave-requests/1` (đơn của ph01) | 404 `NOT_FOUND`, đơn không đổi |
| LR-26 | Q | ph01 | `PATCH /leave-requests/9999` | Như LR-25 |
| LR-27 | Q | ph01 | `PATCH /leave-requests/1` với Origin lạ | 403 `FORBIDDEN`, đơn không đổi |

## ANN: thông báo lớp (chỉ nếu làm thông báo)

| Mã | Loại | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| ANN-01 | H | ph01 | `GET /classes/1/announcements` | 2 mục, mỗi mục có `id, title, content, createdAt` |
| ANN-02 | H | ph01 | `GET /classes/3/announcements` | 1 mục (con thứ hai học lớp 3) |
| ANN-03 | Q | ph01 | `GET /classes/2/announcements` (không có con ở lớp 2) | 404 `NOT_FOUND` |
| ANN-04 | Q | ph02 | `GET /classes/3/announcements` | 404 |
| ANN-05 | Q | ph11 | `GET /classes/1/announcements` (chưa có con) | 404 |
| ANN-06 | D | ph01 | `GET /classes/abc/announcements` | 400 `VALIDATION_ERROR` |

## ACL: phụ huynh gọi endpoint của vai trò khác

Dùng ph01, hợp lệ về dữ liệu nhưng sai vai trò: mọi ca đều 403 `FORBIDDEN` và dữ liệu trong DB không đổi.

| Mã | Loại | Request | Mong đợi |
|---|---|---|---|
| ACL-01 | Q | `GET /teacher/classes`; `GET /classes/1/students` | 403 |
| ACL-02 | Q | `PUT /grades` (hợp lệ về dữ liệu) | 403, điểm không đổi |
| ACL-03 | Q | `GET /classes/1/attendance?date=T-1`; `PUT /classes/1/attendance` | 403, không có bản ghi điểm danh mới |
| ACL-04 | Q | `GET /classes/1/leave-requests`; `POST /classes/1/announcements` | 403 |
| ACL-05 | Q | `GET /admin/users`; `GET /admin/audit-logs`; `GET /subjects` | 403 |
| ACL-06 | Q | ph11: `POST /admin/parent-students` `{parentId:"18", studentId:"1"}` (tự liên kết con người khác) | 403, liên kết không được tạo (đây là thao tác nhạy cảm ở §5) |

Tổng cộng 79 ca: AUTH 13, STU 4, GR 11, ATT 7, TT 8, LR 27, ANN 6, ACL 6.

Đúng rồi, mình đọc nhầm backlog. Xin lỗi nhé. Bước 1 là hoàn thiện trực tiếp bảng tiêu chí nghiệm thu cho TEACHER và ADMIN trong `tieu_chi_nghiem_thu.md`, dựa trên context, API mapping và seed data đã có. Không cần làm lại use case hay gửi teammate ở bước này.

Mình sẽ giữ nguyên quy ước của bảng PARENT: `H` = happy path, `Q` = bị từ chối quyền, `D` = dữ liệu sai, `X` = xung đột; dùng tiền tố mã theo nhóm chức năng và `T` là ngày chạy test theo giờ Việt Nam.

Dưới đây là phần Markdown để nối tiếp sau mục `ACL` hiện tại.

## 3. Giáo viên

Tài khoản demo: mật khẩu chung `Demo@2026`.

| Tài khoản                  | ID | Phân công                                  |
| -------------------------- | -- | ------------------------------------------ |
| `gv.lyluan@actvn.edu.vn`   | 2  | GVCN AT20A; dạy 5 môn lý luận              |
| `gv.vatly@actvn.edu.vn`    | 3  | GVCN DT21C; dạy Vật lý đại cương 1 và 2    |
| `gv.tienganh@actvn.edu.vn` | 4  | Dạy Tiếng Anh 1, 2, 3                      |
| `gv.toan@actvn.edu.vn`     | 5  | Dạy Toán rời rạc và Toán xác suất thống kê |
| `gv.chunhiem@actvn.edu.vn` | 6  | GVCN CT19B, không dạy môn                  |
| `gv.moi@actvn.edu.vn`      | 7  | Chưa có lớp chủ nhiệm hoặc phân công môn   |

Quy ước:

- `H`, `Q`, `D`, `X` lần lượt là thành công, sai quyền, dữ liệu sai, xung đột.
- Giáo viên bộ môn chỉ được xem roster của lớp được phân công; chỉ nhập điểm đúng lớp/môn.
- GVCN được điểm danh, xem và duyệt đơn nghỉ, đăng thông báo trong lớp chủ nhiệm.
- Giáo viên vừa là GVCN vừa dạy bộ môn có hai quyền độc lập.
- Các ca thay đổi dữ liệu nên chạy theo thứ tự hoặc nạp lại seed trước mỗi lượt.

### AUTH: đăng nhập và phiên

| Mã         | Loại | Tài khoản     | Request                                                       | Mong đợi                                                                                                                                               |
| ---------- | ---- | ------------- | ------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------ |
| TA-AUTH-01 | H    | `gv.lyluan`   | `POST /auth/login` đúng email và mật khẩu                     | 200; `data.user = {id:"2", fullName:"Lê Thị Thu Hương", role:"TEACHER"}`; cookie `SCSID` có `HttpOnly`, `SameSite=Lax`; không trả hash hoặc session ID |
| TA-AUTH-02 | H    | `gv.vatly`    | Login với email `" GV.VATLY@ACTVN.EDU.VN "`                   | 200; email được trim và chuyển chữ thường                                                                                                              |
| TA-AUTH-03 | H    | `gv.lyluan`   | Login lần nữa khi đang có session cũ                          | Session ID mới khác session ID cũ                                                                                                                      |
| TA-AUTH-04 | D    | `gv.lyluan`   | Sai mật khẩu                                                  | 401 `UNAUTHENTICATED`, message chung                                                                                                                   |
| TA-AUTH-05 | D    | Không tồn tại | Login bằng email không có trong DB                            | Giống hệt TA-AUTH-04                                                                                                                                   |
| TA-AUTH-06 | Q    | `ph02`        | Đăng nhập tài khoản PARENT rồi thử truy cập chức năng TEACHER | Login PARENT thành công; quyền TEACHER bị từ chối theo các ca ACL bên dưới                                                                             |
| TA-AUTH-07 | H    | `gv.lyluan`   | `GET /auth/me` sau login                                      | 200; đúng ID, tên và role                                                                                                                              |
| TA-AUTH-08 | H    | `gv.lyluan`   | `POST /auth/logout`, sau đó `GET /auth/me` bằng cookie cũ     | Logout 200; request sau trả 401                                                                                                                        |
| TA-AUTH-09 | Q    | —             | `GET /teacher/classes` không cookie                           | 401 `UNAUTHENTICATED`                                                                                                                                  |

### TCH: lớp, môn và danh sách học sinh

| Mã     | Loại | Tài khoản     | Request                                                               | Mong đợi                                                                                                                |
| ------ | ---- | ------------- | --------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------- |
| TCH-01 | H    | `gv.lyluan`   | `GET /teacher/classes`                                                | 200; có lớp 1 AT20A với `isHomeroom=true` và các môn được phân công; không có lớp 2, 3 nếu không có phân công tương ứng |
| TCH-02 | H    | `gv.vatly`    | `GET /teacher/classes`                                                | 200; có lớp 3 DT21C với `isHomeroom=true`, đồng thời có các phân công Vật lý                                            |
| TCH-03 | H    | `gv.chunhiem` | `GET /teacher/classes`                                                | 200; có lớp 2 CT19B với `isHomeroom=true`; `subjectId` và `subjectName` null; không có phân công môn                    |
| TCH-04 | H    | `gv.moi`      | `GET /teacher/classes`                                                | 200 `items: []`; không trả lớp hoặc môn không được phân công                                                            |
| TCH-05 | H    | `gv.tienganh` | `GET /teacher/classes`                                                | 200; có các lớp được phân công môn Tiếng Anh; không có `isHomeroom=true`                                                |
| TCH-06 | H    | `gv.lyluan`   | `GET /classes/1/students`                                             | 200; 5 học sinh của AT20A, ID `"1"` đến `"5"`; không có học sinh lớp khác                                               |
| TCH-07 | H    | `gv.toan`     | `GET /classes/2/students`                                             | 200; 5 học sinh của CT19B                                                                                               |
| TCH-08 | H    | `gv.chunhiem` | `GET /classes/2/students`                                             | 200; GVCN được xem roster dù không dạy môn                                                                              |
| TCH-09 | Q    | `gv.toan`     | `GET /classes/2/students` nếu giáo viên chỉ được phân công ở lớp khác | 404 `NOT_FOUND`                                                                                                         |
| TCH-10 | Q    | `gv.moi`      | `GET /classes/1/students`                                             | 404 `NOT_FOUND`                                                                                                         |
| TCH-11 | Q    | `gv.lyluan`   | `GET /classes/9999/students`                                          | 404; giống TCH-10 về status, `error.code`, message                                                                      |
| TCH-12 | D    | `gv.lyluan`   | `GET /classes/abc/students`                                           | 400 `VALIDATION_ERROR`                                                                                                  |

### SUB: danh mục môn học

| Mã     | Loại | Tài khoản   | Request         | Mong đợi                                              |
| ------ | ---- | ----------- | --------------- | ----------------------------------------------------- |
| SUB-01 | H    | `gv.lyluan` | `GET /subjects` | 200; 12 môn, mỗi mục có `id`, `name`, `subjectCode`   |
| SUB-02 | H    | `admin`     | `GET /subjects` | 200; cùng danh mục 12 môn                             |
| SUB-03 | Q    | `ph01`      | `GET /subjects` | 403 `FORBIDDEN`                                       |
| SUB-04 | Q    | `gv.moi`    | `GET /subjects` | 200; vẫn được xem danh mục môn dù chưa được phân công |

### GR: xem và nhập điểm

Quy ước seed:

- Năm học `2024-2025`; mỗi lớp có 5 học sinh.
- Lớp 1 và 2 có 12 môn; lớp 3 có 11 môn do chưa phân công Tư tưởng Hồ Chí Minh.
- Mỗi môn có 4 đầu điểm `TX1`, `TX2`, `GK`, `CK`; học sinh 5, 10, 15 thiếu `CK`.
- Với môn đã phân công, lớp 1 và lớp 2 có 19 điểm mỗi môn; lớp 3 cũng có 19 điểm mỗi môn do học sinh 15 thiếu CK.

| Mã      | Loại | Tài khoản     | Request                                                                                        | Mong đợi                                                                                                                      |
| ------- | ---- | ------------- | ---------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------- |
| GR-T-01 | H    | `gv.lyluan`   | `GET /classes/1/grades?subjectId=1&schoolYear=2024-2025&term=HK1`                              | 200; `classId:"1"`, `subjectId:"1"`; 5 học sinh, mỗi học sinh có các đầu điểm đã tồn tại; tổng 19 điểm do học sinh 5 thiếu CK |
| GR-T-02 | H    | `gv.vatly`    | `GET /classes/3/grades?subjectId=2&schoolYear=2024-2025&term=HK1`                              | 200; 5 học sinh; tổng 19 điểm do học sinh 15 thiếu CK                                                                         |
| GR-T-03 | H    | `gv.tienganh` | `GET /classes/2/grades?subjectId=4&schoolYear=2024-2025&term=HK1`                              | 200; trả đúng lớp, môn và học kỳ được lọc                                                                                     |
| GR-T-04 | H    | `gv.lyluan`   | `GET /classes/1/grades?subjectId=1`                                                            | 200; trả dữ liệu của môn 1 trong lớp 1, không lấy điểm lớp khác                                                               |
| GR-T-05 | H    | `gv.lyluan`   | `GET /classes/1/grades?subjectId=1&term=HK2`                                                   | 200; trả dữ liệu HK2 nếu có; không lẫn HK1                                                                                    |
| GR-T-06 | H    | `gv.vatly`    | `GET /classes/3/grades?subjectId=12&schoolYear=2024-2025&term=HK2`                             | 404 `NOT_FOUND` vì chưa có phân công môn 12 tại lớp 3                                                                         |
| GR-T-07 | Q    | `gv.toan`     | `GET /classes/1/grades?subjectId=1`                                                            | 404 vì không được phân công môn 1 ở lớp 1                                                                                     |
| GR-T-08 | Q    | `gv.lyluan`   | `GET /classes/2/grades?subjectId=1`                                                            | 404 nếu không có phân công môn 1 tại lớp 2 cho giáo viên này                                                                  |
| GR-T-09 | Q    | `gv.moi`      | `GET /classes/1/grades?subjectId=1`                                                            | 404 `NOT_FOUND`                                                                                                               |
| GR-T-10 | D    | `gv.lyluan`   | `GET /classes/1/grades?subjectId=abc`                                                          | 400 `VALIDATION_ERROR`                                                                                                        |
| GR-T-11 | D    | `gv.lyluan`   | `GET /classes/1/grades?subjectId=1&term=HK3`                                                   | 400 `VALIDATION_ERROR`                                                                                                        |
| GR-T-12 | D    | `gv.lyluan`   | `GET /classes/1/grades` không có `subjectId`                                                   | 400 `VALIDATION_ERROR`                                                                                                        |
| GR-T-13 | H    | `gv.lyluan`   | `PUT /grades` cập nhật điểm TX1 của học sinh 1, môn 1, lớp 1, HK1, năm 2024-2025, `score:9.25` | 200; điểm được cập nhật; có audit `UPDATE` ghi `oldValue.score` và `newValue.score`; audit và điểm cùng transaction           |
| GR-T-14 | H    | `gv.lyluan`   | `PUT /grades` nhập CK cho học sinh 5, môn 1, lớp 1, HK1, `score:8`                             | 200; tạo điểm mới; audit `CREATE`                                                                                             |
| GR-T-15 | H    | `gv.vatly`    | `PUT /grades` nhập điểm cho học sinh 15, môn 2, lớp 3, HK1, `score:7.5`                        | 200; tạo điểm mới; audit `CREATE`                                                                                             |
| GR-T-16 | D    | `gv.lyluan`   | `PUT /grades` với `score:-1`                                                                   | 400 `VALIDATION_ERROR`; điểm không đổi                                                                                        |
| GR-T-17 | D    | `gv.lyluan`   | `PUT /grades` với `score:10.01`                                                                | 400; điểm không đổi                                                                                                           |
| GR-T-18 | D    | `gv.lyluan`   | `PUT /grades` với `score:8.123`                                                                | 400; điểm không đổi                                                                                                           |
| GR-T-19 | D    | `gv.lyluan`   | `PUT /grades` với `score:"8.5"`                                                                | 400 nếu API yêu cầu kiểu số; không tự chuyển chuỗi tùy tiện                                                                   |
| GR-T-20 | D    | `gv.lyluan`   | `PUT /grades` thiếu `scoreType`, `term` hoặc `schoolYear`                                      | 400; không ghi                                                                                                                |
| GR-T-21 | Q    | `gv.toan`     | `PUT /grades` dùng lớp 1, môn 1, học sinh 1 với dữ liệu điểm hợp lệ                            | 404 `NOT_FOUND`; điểm không đổi                                                                                               |
| GR-T-22 | Q    | `gv.lyluan`   | `PUT /grades` dùng `classId:"1"`, `subjectId:"1"` nhưng `studentId:"6"` thuộc lớp 2            | 400 `VALIDATION_ERROR` theo quy tắc ID tham chiếu trong body                                                                  |
| GR-T-23 | Q    | `gv.vatly`    | `PUT /grades` dùng lớp 3, môn 12 chưa được phân công                                           | 404 `NOT_FOUND`; không tạo điểm                                                                                               |
| GR-T-24 | X    | `gv.lyluan`   | Gửi lại cùng một điểm sau khi điểm đã tồn tại                                                  | 200; cập nhật điểm hiện tại, không tạo bản ghi trùng; audit `UPDATE`                                                          |
| GR-T-25 | Q    | `admin`       | `PUT /grades` với dữ liệu hợp lệ                                                               | 403 `FORBIDDEN`; điểm không đổi                                                                                               |

### ATT: điểm danh của GVCN

| Mã       | Loại | Tài khoản     | Request                                                                | Mong đợi                                                                                                                                     |
| -------- | ---- | ------------- | ---------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------- |
| ATT-T-01 | H    | `gv.lyluan`   | `GET /classes/1/attendance?date=T-1`                                   | 200; 5 học sinh lớp 1; ngày đúng; trạng thái thuộc enum                                                                                      |
| ATT-T-02 | H    | `gv.lyluan`   | `GET /classes/1/attendance?date=T`                                     | 200; 5 học sinh; các học sinh chưa có bản ghi hôm nay chưa có status hoặc được biểu diễn theo response contract; không tự suy diễn `PRESENT` |
| ATT-T-03 | H    | `gv.chunhiem` | `GET /classes/2/attendance?date=T-1`                                   | 200; chỉ 5 học sinh CT19B                                                                                                                    |
| ATT-T-04 | H    | `gv.lyluan`   | `PUT /classes/1/attendance` với ngày T, đủ 5 học sinh và status hợp lệ | 200; `updatedCount:5`; dữ liệu được lưu cho đúng lớp/ngày                                                                                    |
| ATT-T-05 | H    | `gv.lyluan`   | Gửi lại PUT cùng lớp/ngày với trạng thái đã thay đổi                   | 200; cập nhật bản ghi hiện tại, không tạo bản ghi trùng                                                                                      |
| ATT-T-06 | D    | `gv.lyluan`   | `GET /classes/1/attendance?date=2026-02-30`                            | 400 `VALIDATION_ERROR`                                                                                                                       |
| ATT-T-07 | D    | `gv.lyluan`   | `GET /classes/1/attendance` thiếu `date`                               | 400                                                                                                                                          |
| ATT-T-08 | D    | `gv.lyluan`   | PUT điểm danh ngày T+1                                                 | 400; không ghi                                                                                                                               |
| ATT-T-09 | D    | `gv.lyluan`   | PUT điểm danh vào thứ Bảy hoặc Chủ nhật                                | 400; không ghi                                                                                                                               |
| ATT-T-10 | D    | `gv.lyluan`   | PUT batch có một `status` không thuộc enum                             | 400; toàn bộ batch không được ghi                                                                                                            |
| ATT-T-11 | D    | `gv.lyluan`   | PUT batch có một `studentId` thuộc lớp khác                            | 400; toàn bộ batch không được ghi                                                                                                            |
| ATT-T-12 | D    | `gv.lyluan`   | PUT batch có một học sinh lặp hai lần                                  | 400; toàn bộ batch không được ghi                                                                                                            |
| ATT-T-13 | Q    | `gv.vatly`    | `GET /classes/1/attendance?date=T-1`                                   | 404 vì không phải GVCN lớp 1                                                                                                                 |
| ATT-T-14 | Q    | `gv.toan`     | `PUT /classes/1/attendance` với dữ liệu hợp lệ                         | 404; không ghi                                                                                                                               |
| ATT-T-15 | Q    | `gv.moi`      | `GET /classes/1/attendance?date=T-1`                                   | 404                                                                                                                                          |
| ATT-T-16 | D    | `gv.lyluan`   | `PUT /classes/1/attendance` có `note` ngoài contract                   | 400 `VALIDATION_ERROR`                                                                                                                       |

### LR-T: GVCN xem và xử lý đơn nghỉ

| Mã      | Loại | Tài khoản     | Request                                                | Mong đợi                                                                                       |
| ------- | ---- | ------------- | ------------------------------------------------------ | ---------------------------------------------------------------------------------------------- |
| LR-T-01 | H    | `gv.lyluan`   | `GET /classes/1/leave-requests`                        | 200; trả các đơn của học sinh lớp 1; có đơn 3 PENDING của học sinh 3                           |
| LR-T-02 | H    | `gv.chunhiem` | `GET /classes/2/leave-requests`                        | 200; có đơn 7 REJECTED của học sinh 6; không có đơn lớp khác                                   |
| LR-T-03 | H    | `gv.vatly`    | `GET /classes/3/leave-requests`                        | 200; có đơn 2 PENDING của học sinh 15                                                          |
| LR-T-04 | H    | `gv.lyluan`   | `GET /classes/1/leave-requests?status=PENDING`         | 200; chỉ trả đơn PENDING của lớp 1                                                             |
| LR-T-05 | D    | `gv.lyluan`   | `GET /classes/1/leave-requests?status=ABC`             | 400 `VALIDATION_ERROR`                                                                         |
| LR-T-06 | H    | `gv.lyluan`   | `PATCH /leave-requests/3` body `{status:"APPROVED"}`   | 200; đơn chuyển APPROVED; có `reviewedBy` là user 2 và `reviewedAt`; audit cập nhật trạng thái |
| LR-T-07 | H    | `gv.vatly`    | `PATCH /leave-requests/2` body `{status:"REJECTED"}`   | 200; đơn chuyển REJECTED; reviewer là user 3                                                   |
| LR-T-08 | D    | `gv.lyluan`   | PATCH với `status:"PENDING"` hoặc giá trị không hợp lệ | 400; đơn không đổi                                                                             |
| LR-T-09 | D    | `gv.lyluan`   | PATCH kèm `reviewedBy` hoặc `reviewedAt` từ client     | 400 `VALIDATION_ERROR`; không cho client tự chỉ định người duyệt/thời điểm                     |
| LR-T-10 | Q    | `gv.vatly`    | `PATCH /leave-requests/3` (đơn thuộc lớp 1)            | 404 `NOT_FOUND`; đơn không đổi                                                                 |
| LR-T-11 | Q    | `gv.moi`      | `PATCH /leave-requests/3`                              | 404                                                                                            |
| LR-T-12 | X    | `gv.lyluan`   | Duyệt đơn 3 lần thứ hai sau khi đã APPROVED            | 409 `CONFLICT`; message “Đơn đã được xử lý, không thể thay đổi.”                               |
| LR-T-13 | X    | `gv.vatly`    | Duyệt đơn 2 lần thứ hai sau khi đã REJECTED            | 409; đơn không đổi                                                                             |
| LR-T-14 | Q    | `admin`       | `PATCH /leave-requests/3` với status hợp lệ            | 403 `FORBIDDEN`                                                                                |

### ANN-T: thông báo lớp (chỉ nghiệm thu nếu triển khai)

| Mã       | Loại | Tài khoản     | Request                                                                                                                          | Mong đợi                                                                           |
| -------- | ---- | ------------- | -------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------- |
| ANN-T-01 | H    | `gv.lyluan`   | `GET /classes/1/announcements`                                                                                                   | 200; 2 thông báo seed, có `id,title,content,createdAt`                             |
| ANN-T-02 | H    | `gv.chunhiem` | `GET /classes/2/announcements`                                                                                                   | 200; 1 thông báo seed                                                              |
| ANN-T-03 | H    | `gv.lyluan`   | `POST /classes/1/announcements` `{title:"Thông báo kiểm tra",content:"Lớp chuẩn bị nội dung ôn tập cho buổi kiểm tra sắp tới."}` | 201; `announcement.classId:"1"`, author lấy từ session; tải lại GET thấy thông báo |
| ANN-T-04 | D    | `gv.lyluan`   | POST title dưới 5 ký tự hoặc trên 50 ký tự                                                                                       | 400; không tạo                                                                     |
| ANN-T-05 | D    | `gv.lyluan`   | POST content dưới 10 ký tự hoặc trên 1000 ký tự                                                                                  | 400; không tạo                                                                     |
| ANN-T-06 | D    | `gv.lyluan`   | POST kèm `authorId` hoặc `classId` trong body                                                                                    | 400 nếu field ngoài allowlist; lớp lấy từ path, author từ session                  |
| ANN-T-07 | Q    | `gv.vatly`    | `POST /classes/1/announcements`                                                                                                  | 404 vì không phải GVCN lớp 1                                                       |
| ANN-T-08 | Q    | `gv.moi`      | `GET /classes/1/announcements`                                                                                                   | 404                                                                                |

### ACL-T: giáo viên gọi API ngoài quyền

Dùng tài khoản TEACHER, dữ liệu request hợp lệ nhưng sai role; mọi ca trả `403 FORBIDDEN`, dữ liệu không đổi.

| Mã       | Loại | Tài khoản   | Request                            | Mong đợi |
| -------- | ---- | ----------- | ---------------------------------- | -------- |
| ACL-T-01 | Q    | `gv.lyluan` | `GET /students`                    | 403      |
| ACL-T-02 | Q    | `gv.lyluan` | `GET /students/1/grades`           | 403      |
| ACL-T-03 | Q    | `gv.lyluan` | `GET /students/1/attendance`       | 403      |
| ACL-T-04 | Q    | `gv.lyluan` | `GET /students/1/timetable`        | 403      |
| ACL-T-05 | Q    | `gv.lyluan` | `GET /admin/users`                 | 403      |
| ACL-T-06 | Q    | `gv.lyluan` | `GET /admin/audit-logs`            | 403      |
| ACL-T-07 | Q    | `gv.lyluan` | `POST /admin/parent-students`      | 403      |
| ACL-T-08 | Q    | `gv.lyluan` | `POST /admin/teaching-assignments` | 403      |

## 4. Admin

Tài khoản demo: `admin@actvn.edu.vn`, mật khẩu `Demo@2026`, ID `1`.

Quy ước:

- Admin được quản lý tài khoản, lớp, hồ sơ học sinh, liên kết phụ huynh–học sinh và phân công giáo viên.
- Admin chỉ được xem audit log; không được xem điểm, điểm danh, thời khóa biểu hoặc đơn nghỉ.
- Không có chức năng xóa tài khoản, lớp, học sinh hoặc môn học. Tài khoản có thể vô hiệu hóa; lớp và học sinh có thể sửa.
- Chỉ được gỡ liên kết phụ huynh–học sinh và phân công giáo viên khi không bị ràng buộc dữ liệu.
- Các thao tác tạo, sửa, gỡ liên kết và gỡ phân công cần kiểm tra dữ liệu sau thao tác và audit log tương ứng.

### USR: quản lý tài khoản

Seed ban đầu có 20 tài khoản: 1 ADMIN, 6 TEACHER và 13 PARENT. Trong đó có một PARENT bị vô hiệu hóa (`ph.vohieuhoa`) và một PARENT đang bị khóa (`ph.bikhoa`).

| Mã     | Loại | Tài khoản | Request                                                                          | Mong đợi                                                                                                 |
| ------ | ---- | --------- | -------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------- |
| USR-01 | H    | admin     | `GET /admin/users`                                                               | 200; 20 tài khoản; mỗi mục có `id,email,fullName,role,isActive`; không trả password/hash                 |
| USR-02 | H    | admin     | `GET /admin/users?role=TEACHER`                                                  | 200; 6 tài khoản TEACHER                                                                                 |
| USR-03 | H    | admin     | `GET /admin/users?role=PARENT`                                                   | 200; 13 tài khoản PARENT, bao gồm tài khoản bị vô hiệu hóa và đang khóa                                  |
| USR-04 | H    | admin     | `GET /admin/users?search=gv.lyluan`                                              | 200; tìm được tài khoản `gv.lyluan@actvn.edu.vn`                                                         |
| USR-05 | H    | admin     | `GET /admin/users?role=TEACHER&search=gv`                                        | 200; chỉ trả các tài khoản TEACHER khớp điều kiện                                                        |
| USR-06 | D    | admin     | `GET /admin/users?role=ABC`                                                      | 400 `VALIDATION_ERROR`                                                                                   |
| USR-07 | H    | admin     | `POST /admin/users` tạo PARENT với email mới, tên hợp lệ và password `Demo@2026` | 201; trả `user:{id,email,fullName,role}`; không trả password/hash; đăng nhập được bằng tài khoản vừa tạo |
| USR-08 | H    | admin     | `POST /admin/users` tạo TEACHER với email mới                                    | 201; role là `TEACHER`; đăng nhập được                                                                   |
| USR-09 | D    | admin     | POST thiếu `email`, `password`, `fullName` hoặc `role`                           | 400 `VALIDATION_ERROR`; không tạo tài khoản                                                              |
| USR-10 | D    | admin     | POST email sai định dạng hoặc độ dài ngoài 5–50                                  | 400; không tạo                                                                                           |
| USR-11 | D    | admin     | POST password dưới 8 ký tự                                                       | 400; không tạo                                                                                           |
| USR-12 | D    | admin     | POST role `ADMIN` hoặc role không hợp lệ                                         | 400 nếu phạm vi API chỉ cho tạo PARENT/TEACHER                                                           |
| USR-13 | X    | admin     | POST email đã tồn tại `ph01@actvn.edu.vn`                                        | 409 `CONFLICT`; không tạo tài khoản trùng                                                                |
| USR-14 | H    | admin     | `PATCH /admin/users/:id` body `{fullName:"Tên mới hợp lệ"}`                      | 200; cập nhật tên; email và role không đổi                                                               |
| USR-15 | H    | admin     | PATCH `{isActive:false}` với tài khoản PARENT đang hoạt động                     | 200; tài khoản bị vô hiệu hóa; login sau đó trả 401 với message chung                                    |
| USR-16 | H    | admin     | PATCH `{isActive:true}` với user 19 (`ph.vohieuhoa`)                             | 200; kích hoạt lại; tài khoản đăng nhập được nếu không bị khóa                                           |
| USR-17 | H    | admin     | PATCH `{password:"NewPass@2026"}`                                                | 200; mật khẩu mới đăng nhập được, mật khẩu cũ không còn dùng được; response không trả mật khẩu/hash      |
| USR-18 | H    | admin     | PATCH `{unlock:true}` với user 20 (`ph.bikhoa`)                                  | 200; `failed_logins=0`, `locked_until=NULL`; không tự thay đổi `isActive`                                |
| USR-19 | D    | admin     | PATCH tên rỗng, dưới 5 hoặc trên 50 ký tự                                        | 400; dữ liệu không đổi                                                                                   |
| USR-20 | D    | admin     | PATCH password dưới 8 ký tự                                                      | 400; mật khẩu cũ vẫn dùng được                                                                           |
| USR-21 | D    | admin     | PATCH `{email:"new@example.com"}` hoặc `{role:"ADMIN"}`                          | 400 `VALIDATION_ERROR`; không cho sửa email/role                                                         |
| USR-22 | D    | admin     | PATCH có field ngoài allowlist hoặc sai kiểu dữ liệu                             | 400; không thay đổi dữ liệu                                                                              |
| USR-23 | Q    | admin     | `PATCH /admin/users/9999`                                                        | 404 `NOT_FOUND`                                                                                          |
| USR-24 | Q    | admin     | Gọi `DELETE /admin/users/:id`                                                    | Không có endpoint xóa tài khoản; không được xóa tài khoản bằng API                                       |
| USR-25 | H    | admin     | `GET /admin/users?role=PARENT&search=ph.vohieuhoa`                               | 200; vẫn tìm thấy tài khoản disabled để quản trị                                                         |
| USR-26 | H    | admin     | Sau khi khóa hoặc vô hiệu hóa tài khoản, thử login bằng đúng password            | 401 `UNAUTHENTICATED`, message giống nhau giữa sai mật khẩu, tài khoản không tồn tại, disabled và locked |

### CLS: quản lý lớp học

Seed ban đầu có 3 lớp: AT20A (ID 1), CT19B (ID 2), DT21C (ID 3).

| Mã     | Loại | Tài khoản | Request                                                                            | Mong đợi                                                                |
| ------ | ---- | --------- | ---------------------------------------------------------------------------------- | ----------------------------------------------------------------------- |
| CLS-01 | H    | admin     | `GET /admin/classes`                                                               | 200; 3 lớp; mỗi mục có `id,name,classCode,schoolYear,homeroomTeacherId` |
| CLS-02 | H    | admin     | `GET /admin/classes?search=AT20A`                                                  | 200; chỉ lớp AT20A                                                      |
| CLS-03 | H    | admin     | `POST /admin/classes` tạo lớp mới, GVCN là user 7 (`gv.moi`)                       | 201; trả `class:{id,name}`; lớp được tạo với đúng mã, niên khóa và GVCN |
| CLS-04 | D    | admin     | POST thiếu `name`, `classCode`, `schoolYear` hoặc `homeroomTeacherId`              | 400; không tạo lớp                                                      |
| CLS-05 | D    | admin     | POST `name` dưới 2 hoặc trên 20 ký tự                                              | 400                                                                     |
| CLS-06 | D    | admin     | POST `classCode` dưới 2 hoặc trên 10 ký tự                                         | 400                                                                     |
| CLS-07 | D    | admin     | POST `schoolYear` sai định dạng `YYYY-YYYY`                                        | 400                                                                     |
| CLS-08 | D    | admin     | POST `homeroomTeacherId` là tài khoản PARENT hoặc ADMIN                            | 400 `VALIDATION_ERROR`                                                  |
| CLS-09 | Q    | admin     | POST dùng `homeroomTeacherId` không tồn tại                                        | 400 theo quy ước ID tham chiếu trong body                               |
| CLS-10 | X    | admin     | Tạo lớp trùng `classCode` và `schoolYear` với lớp hiện có                          | 409 `CONFLICT`                                                          |
| CLS-11 | X    | admin     | Gán một GVCN đã chủ nhiệm lớp khác trong cùng năm học                              | 409 `CONFLICT`                                                          |
| CLS-12 | H    | admin     | `PATCH /admin/classes/1` đổi `name`                                                | 200; tên lớp thay đổi; các trường còn lại giữ nguyên                    |
| CLS-13 | H    | admin     | PATCH đổi `classCode`                                                              | 200 nếu mã mới hợp lệ và không trùng                                    |
| CLS-14 | H    | admin     | PATCH đổi `schoolYear`                                                             | 200; được phép sửa niên khóa lớp                                        |
| CLS-15 | H    | admin     | PATCH đổi `homeroomTeacherId` sang TEACHER hợp lệ chưa chủ nhiệm lớp khác cùng năm | 200                                                                     |
| CLS-16 | D    | admin     | PATCH field lớp sai định dạng hoặc ngoài allowlist                                 | 400; lớp không đổi                                                      |
| CLS-17 | X    | admin     | PATCH làm trùng mã lớp–năm học hoặc GVCN–năm học                                   | 409                                                                     |
| CLS-18 | Q    | admin     | `PATCH /admin/classes/9999`                                                        | 404 `NOT_FOUND`                                                         |
| CLS-19 | Q    | admin     | Gọi `DELETE /admin/classes/1`                                                      | Không có endpoint xóa lớp; không được xóa lớp bằng API                  |

### STD: quản lý hồ sơ học sinh

Seed ban đầu có 15 học sinh, mỗi lớp 5 học sinh. Mã học sinh theo định dạng đã chốt, ví dụ `AT200101`, `CT190201`, `DT210301`.

| Mã     | Loại | Tài khoản | Request                                                                                                  | Mong đợi                                                                   |
| ------ | ---- | --------- | -------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------- |
| STD-01 | H    | admin     | `GET /admin/students`                                                                                    | 200; 15 học sinh; mỗi mục có `id,studentCode,fullName,dateOfBirth,classId` |
| STD-02 | H    | admin     | `GET /admin/students?classId=1`                                                                          | 200; 5 học sinh lớp AT20A                                                  |
| STD-03 | H    | admin     | `GET /admin/students?search=AT200101`                                                                    | 200; tìm được học sinh ID 1                                                |
| STD-04 | H    | admin     | `GET /admin/students?classId=3&search=DT2103`                                                            | 200; chỉ học sinh khớp cả lớp và từ khóa                                   |
| STD-05 | D    | admin     | `GET /admin/students?classId=abc`                                                                        | 400 `VALIDATION_ERROR`                                                     |
| STD-06 | H    | admin     | `POST /admin/students` tạo học sinh mới với mã chưa dùng, tên hợp lệ, ngày sinh hợp lệ và classId hợp lệ | 201; trả đúng `student:{id,studentCode,fullName,dateOfBirth,classId}`      |
| STD-07 | D    | admin     | POST thiếu một trong các field bắt buộc                                                                  | 400; không tạo                                                             |
| STD-08 | D    | admin     | POST `fullName` dưới 5 hoặc trên 50 ký tự                                                                | 400                                                                        |
| STD-09 | D    | admin     | POST ngày sinh sai định dạng hoặc ngày không tồn tại                                                     | 400                                                                        |
| STD-10 | D    | admin     | POST ngày sinh trong tương lai                                                                           | 400                                                                        |
| STD-11 | D    | admin     | POST `classId` sai kiểu hoặc không hợp lệ                                                                | 400                                                                        |
| STD-12 | X    | admin     | POST `studentCode` đã tồn tại, ví dụ `AT200101`                                                          | 409 `CONFLICT`                                                             |
| STD-13 | H    | admin     | `PATCH /admin/students/5` đổi `fullName`                                                                 | 200; tên mới được lưu                                                      |
| STD-14 | H    | admin     | PATCH đổi `dateOfBirth` sang ngày hợp lệ không ở tương lai                                               | 200                                                                        |
| STD-15 | H    | admin     | PATCH đổi `classId` sang lớp khác tồn tại                                                                | 200; học sinh thuộc lớp mới                                                |
| STD-16 | D    | admin     | PATCH đổi `studentCode`                                                                                  | 400 `VALIDATION_ERROR`; mã học sinh không được sửa                         |
| STD-17 | D    | admin     | PATCH có field ngoài allowlist hoặc sai kiểu dữ liệu                                                     | 400; hồ sơ không đổi                                                       |
| STD-18 | D    | admin     | PATCH ngày sinh sai hoặc trong tương lai                                                                 | 400                                                                        |
| STD-19 | Q    | admin     | `PATCH /admin/students/9999`                                                                             | 404 `NOT_FOUND`                                                            |
| STD-20 | Q    | admin     | Gọi `DELETE /admin/students/1`                                                                           | Không có endpoint xóa học sinh; không được xóa bằng API                    |

### LNK: liên kết phụ huynh – học sinh

Seed ban đầu có 11 liên kết. Học sinh 5, 9, 10, 14 chưa liên kết; `ph11` (user 18) chưa có con. `ph04` (user 11) liên kết học sinh 4 và không có đơn nghỉ, phù hợp để demo gỡ liên kết. Liên kết `ph01` – học sinh 1 có đơn nghỉ nên dùng để thử lỗi ràng buộc.

| Mã     | Loại | Tài khoản | Request                                                                             | Mong đợi                                                                               |
| ------ | ---- | --------- | ----------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------- |
| LNK-01 | H    | admin     | `GET /admin/parent-students`                                                        | 200; 11 liên kết; mỗi mục có `parentId,studentId,relationship`                         |
| LNK-02 | H    | admin     | `GET /admin/parent-students?parentId=8`                                             | 200; 2 liên kết của ph01 với học sinh 1 và 15                                          |
| LNK-03 | H    | admin     | `GET /admin/parent-students?studentId=12`                                           | 200; một liên kết của user 16 với học sinh 12, relationship `GUARDIAN`                 |
| LNK-04 | H    | admin     | `GET /admin/parent-students?parentId=18`                                            | 200 `items: []`                                                                        |
| LNK-05 | D    | admin     | Query `parentId=abc` hoặc `studentId=abc`                                           | 400 `VALIDATION_ERROR`                                                                 |
| LNK-06 | H    | admin     | `POST /admin/parent-students` `{parentId:"18",studentId:"5",relationship:"PARENT"}` | 201; tạo liên kết; `ph11` có học sinh 5; audit `CREATE`                                |
| LNK-07 | H    | admin     | POST liên kết với `relationship:"GUARDIAN"`                                         | 201; relationship được lưu đúng                                                        |
| LNK-08 | D    | admin     | POST thiếu `relationship` hoặc relationship không thuộc `PARENT/GUARDIAN`           | 400; không tạo                                                                         |
| LNK-09 | D    | admin     | POST `parentId` trỏ tới TEACHER hoặc ADMIN                                          | 400 `VALIDATION_ERROR`                                                                 |
| LNK-10 | D    | admin     | POST `studentId` không tồn tại hoặc ID tham chiếu sai                               | 400 theo quy ước ID tham chiếu trong body                                              |
| LNK-11 | X    | admin     | Tạo liên kết cho học sinh đã có phụ huynh, ví dụ học sinh 1                         | 409 `CONFLICT`                                                                         |
| LNK-12 | X    | admin     | Tạo lại liên kết ph11–học sinh 5 sau LNK-06                                         | 409 `CONFLICT`                                                                         |
| LNK-13 | H    | admin     | `DELETE /admin/parent-students/11/4`                                                | 200 `{deleted:true}`; liên kết ph04–học sinh 4 bị gỡ; audit `DELETE`                   |
| LNK-14 | X    | admin     | `DELETE /admin/parent-students/8/1`                                                 | 409 `CONFLICT` vì học sinh/quan hệ đang được đơn nghỉ tham chiếu; liên kết không bị gỡ |
| LNK-15 | Q    | admin     | DELETE liên kết không tồn tại `/admin/parent-students/18/5` trước khi tạo           | 404 `NOT_FOUND`                                                                        |
| LNK-16 | Q    | admin     | DELETE liên kết đã gỡ ở LNK-13 lần nữa                                              | 404 `NOT_FOUND`                                                                        |

### TA: phân công giáo viên bộ môn

Seed ban đầu có 35 phân công. Mỗi môn được phân công cho cả ba lớp, ngoại trừ Tư tưởng Hồ Chí Minh tại lớp DT21C.

| Mã    | Loại | Tài khoản | Request                                                                         | Mong đợi                                                                 |
| ----- | ---- | --------- | ------------------------------------------------------------------------------- | ------------------------------------------------------------------------ |
| TA-01 | H    | admin     | `GET /admin/teaching-assignments`                                               | 200; 35 phân công; mỗi mục có `teacherId,classId,subjectId`              |
| TA-02 | H    | admin     | `GET /admin/teaching-assignments?classId=3`                                     | 200; 11 phân công tại DT21C                                              |
| TA-03 | H    | admin     | `GET /admin/teaching-assignments?teacherId=2`                                   | 200; các phân công môn lý luận của user 2                                |
| TA-04 | H    | admin     | `GET /admin/teaching-assignments?classId=3&teacherId=3`                         | 200; chỉ các môn Vật lý được phân công cho user 3 tại lớp 3              |
| TA-05 | D    | admin     | Query `classId=abc` hoặc `teacherId=abc`                                        | 400 `VALIDATION_ERROR`                                                   |
| TA-06 | H    | admin     | `POST /admin/teaching-assignments` `{teacherId:"7",classId:"3",subjectId:"12"}` | 201; phân công gv.moi môn Tư tưởng Hồ Chí Minh tại DT21C; audit `CREATE` |
| TA-07 | D    | admin     | POST thiếu một trong ba ID                                                      | 400; không tạo                                                           |
| TA-08 | D    | admin     | POST `teacherId` là PARENT hoặc ADMIN                                           | 400 `VALIDATION_ERROR`                                                   |
| TA-09 | D    | admin     | POST teacher/class/subject ID không hợp lệ trong body                           | 400 theo quy ước ID tham chiếu trong body                                |
| TA-10 | X    | admin     | POST lại phân công lớp 3 – môn 12 sau TA-06                                     | 409 `CONFLICT`                                                           |
| TA-11 | X    | admin     | POST phân công lớp–môn đã có giáo viên khác                                     | 409 `CONFLICT`                                                           |
| TA-12 | H    | admin     | `DELETE /admin/teaching-assignments/7/3/12` sau TA-06                           | 200 `{deleted:true}`; phân công bị gỡ; audit `DELETE`                    |
| TA-13 | X    | admin     | DELETE phân công đang được thời khóa biểu tham chiếu                            | 409 `CONFLICT`; không xóa dây chuyền và không làm mất thời khóa biểu     |
| TA-14 | Q    | admin     | DELETE phân công không tồn tại                                                  | 404 `NOT_FOUND`                                                          |
| TA-15 | Q    | admin     | DELETE lại phân công đã gỡ ở TA-12                                              | 404 `NOT_FOUND`                                                          |

### AUD: nhật ký kiểm toán

Seed ban đầu có 6 audit log mẫu: liên kết phụ huynh, phân công, xử lý đơn nghỉ và cập nhật điểm.

| Mã     | Loại | Tài khoản | Request                                                           | Mong đợi                                                                                            |
| ------ | ---- | --------- | ----------------------------------------------------------------- | --------------------------------------------------------------------------------------------------- |
| AUD-01 | H    | admin     | `GET /admin/audit-logs`                                           | 200; 6 bản ghi seed; mỗi mục có `id,actorId,action,entityType,entityId,oldValue,newValue,createdAt` |
| AUD-02 | H    | admin     | `GET /admin/audit-logs?studentId=1`                               | 200; chỉ log liên quan học sinh 1                                                                   |
| AUD-03 | H    | admin     | `GET /admin/audit-logs?action=UPDATE`                             | 200; chỉ log có action UPDATE                                                                       |
| AUD-04 | H    | admin     | `GET /admin/audit-logs?entityType=grades`                         | 200; log cập nhật điểm                                                                              |
| AUD-05 | H    | admin     | `GET /admin/audit-logs?entityId=1`                                | 200; chỉ log có entityId tương ứng                                                                  |
| AUD-06 | H    | admin     | Kết hợp `studentId`, `action`, `entityType`, `entityId`           | 200; kết quả thỏa mãn đồng thời các bộ lọc                                                          |
| AUD-07 | H    | admin     | Lọc với điều kiện không có log phù hợp                            | 200 `items: []`, không trả null hoặc 404                                                            |
| AUD-08 | D    | admin     | `studentId=abc`                                                   | 400 `VALIDATION_ERROR`                                                                              |
| AUD-09 | D    | admin     | `action=INVALID` hoặc `entityType` không hợp lệ                   | 400 `VALIDATION_ERROR`                                                                              |
| AUD-10 | D    | admin     | `entityId` sai định dạng nếu contract quy định giới hạn/định dạng | 400; không trả dữ liệu ngoài bộ lọc                                                                 |
| AUD-11 | H    | admin     | Sau thao tác LNK-06 hoặc TA-06, tải lại audit log                 | Có bản ghi CREATE tương ứng với actor là admin                                                      |
| AUD-12 | H    | admin     | Sau thao tác LNK-13 hoặc TA-12, tải lại audit log                 | Có bản ghi DELETE tương ứng                                                                         |
| AUD-13 | H    | admin     | Mở chi tiết audit log                                             | Hiển thị `oldValue`, `newValue` và thời điểm; không có thao tác sửa/xóa log                         |
| AUD-14 | Q    | admin     | Thử gọi API ghi hoặc xóa audit log không được định nghĩa          | Không có endpoint ghi/sửa/xóa audit log; log chỉ đọc                                                |

### ACL-A: admin truy cập chức năng ngoài quyền

Dùng tài khoản `admin@actvn.edu.vn`, request hợp lệ về dữ liệu nhưng sai vai trò. Các ca dưới phải trả `403 FORBIDDEN`; không trả dữ liệu học tập.

| Mã       | Loại | Request                                        | Mong đợi                 |
| -------- | ---- | ---------------------------------------------- | ------------------------ |
| ACL-A-01 | Q    | `GET /students`                                | 403                      |
| ACL-A-02 | Q    | `GET /students/1/grades`                       | 403                      |
| ACL-A-03 | Q    | `GET /students/1/attendance`                   | 403                      |
| ACL-A-04 | Q    | `GET /students/1/timetable`                    | 403                      |
| ACL-A-05 | Q    | `GET /students/1/leave-requests`               | 403                      |
| ACL-A-06 | Q    | `GET /classes/1/grades?subjectId=1`            | 403                      |
| ACL-A-07 | Q    | `PUT /grades` với dữ liệu hợp lệ               | 403; điểm không đổi      |
| ACL-A-08 | Q    | `GET /classes/1/attendance?date=T-1`           | 403                      |
| ACL-A-09 | Q    | `PUT /classes/1/attendance` với dữ liệu hợp lệ | 403; điểm danh không đổi |
| ACL-A-10 | Q    | `GET /classes/1/leave-requests`                | 403                      |
| ACL-A-11 | Q    | `PATCH /leave-requests/3` với status hợp lệ    | 403; đơn không đổi       |
| ACL-A-12 | Q    | `GET /classes/1/announcements`                 | 403                      |
| ACL-A-13 | Q    | `POST /classes/1/announcements`                | 403; không tạo thông báo |
| ACL-A-14 | Q    | `GET /teacher/classes`                         | 403                      |

## 5. Quy tắc chung khi nghiệm thu

Áp dụng cho cả ba vai trò:

1. Request không có session hoặc session hết hạn: `401 UNAUTHENTICATED`.
2. Sai role: `403 FORBIDDEN`.
3. ID không tồn tại hoặc tài nguyên nằm ngoài phạm vi quan hệ/quyền truy cập: `404 NOT_FOUND`; hai trường hợp phải có cùng `status`, `error.code` và `message`.
4. Body, query, path sai hoặc field ngoài allowlist: `400 VALIDATION_ERROR`.
5. Trùng dữ liệu, trạng thái không cho phép hoặc tài nguyên đang bị tham chiếu: `409 CONFLICT`.
6. Các request ghi phải kiểm tra Origin; Origin thiếu hoặc ngoài allowlist trả `403 FORBIDDEN`.
7. Các thao tác điểm và audit log phải cùng transaction. Điểm danh theo lô phải validate toàn bộ trước khi ghi; một dòng sai thì không ghi bất kỳ dòng nào.
8. Admin không được truy cập dữ liệu học tập thông qua API quản trị hoặc bằng cách thay ID trên URL.
9. Không có endpoint xóa tài khoản, lớp, học sinh, điểm, điểm danh, môn học hoặc audit log trong phạm vi hiện tại.

Lưu ý về các ca cần chuẩn bị dữ liệu: những ca có trạng thái thay đổi (tạo tài khoản, sửa điểm, duyệt đơn, điểm danh, tạo/gỡ liên kết và phân công) nên chạy theo thứ tự hoặc reset DB về seed trước mỗi lượt. Đặc biệt, TA-06 phải chạy trước TA-12; LNK-06 phải chạy trước LNK-13 nếu muốn kiểm tra cả tạo và xóa trong cùng lượt.

- ANN là chức năng có thể cắt nếu thiếu thời gian; có thể bỏ toàn bộ nhóm ANN-T và ANN-A liên quan nếu không triển khai thông báo.

