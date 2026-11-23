-- SEED DEMO - đồ án "Kết nối phụ huynh - nhà trường"
-- Chạy trên DB rỗng, sau ddl.sql. Mật khẩu mọi tài khoản: Demo@2026 (bcrypt $2b$, cost 10).
-- Quy ước:
--  * Mã học sinh: {AT|CT|DT}{khóa 2 số}{mã lớp 2 số, A=01..D=04}{stt 2 số}, ví dụ AT200101.
--  * classes.school_year là KHÓA HỌC (ví dụ 2023-2028). grades/timetable.school_year là NĂM HỌC (2024-2025).
--  * Ngày điểm danh, đơn nghỉ, thông báo, audit tính theo ngày chạy seed, giờ Việt Nam (UTC+7).
--  * Buổi học = 3 tiết (1-3, 4-6 sáng; 7-9, 10-12 chiều). Tiếng Anh 3 buổi/tuần, môn khác 2 buổi.
-- Cố ý để dành cho demo thêm/sửa/xóa:
--  * HS 5, 9, 10, 14 chưa liên kết phụ huynh; ph11 chưa có con; gv.moi chưa được phân công.
--  * DT21C chưa phân công Tư tưởng Hồ Chí Minh (tạo rồi xóa phân công).
--  * HS 5, 10, 15 thiếu điểm cuối kỳ; hôm nay chưa có điểm danh.
-- Tài khoản (@school.test): admin, gv.lyluan (GVCN AT20A), gv.vatly (GVCN DT21C),
--  gv.chunhiem (GVCN CT19B, không dạy môn), gv.tienganh, gv.toan, gv.moi,
--  ph01..ph11, ph.vohieuhoa (vô hiệu hóa), ph.bikhoa (đang khóa).

-- Mật khẩu demo của MỌI tài khoản: Demo@2026 (bcrypt). Chỉ dùng cho demo.
INSERT INTO users (id, full_name, email, password_hash, role, is_active, failed_logins, locked_until, created_at) VALUES
(1,  'Quản trị hệ thống',  'admin@actvn.edu.vn',         '$2b$10$DXEvPRcwAB9LtjNsDb6YvOHDKqkKkZo4y0JS0o.AB4Kq1B0HpdVja', 'ADMIN',   1, 0, NULL, '2026-09-01T00:00:00Z'),
(2,  'Lê Thị Thu Hương',   'gv.lyluan@actvn.edu.vn',     '$2b$10$DXEvPRcwAB9LtjNsDb6YvOHDKqkKkZo4y0JS0o.AB4Kq1B0HpdVja', 'TEACHER', 1, 0, NULL, '2026-09-01T00:00:00Z'),
(3,  'Phạm Quang Huy',     'gv.vatly@actvn.edu.vn',      '$2b$10$DXEvPRcwAB9LtjNsDb6YvOHDKqkKkZo4y0JS0o.AB4Kq1B0HpdVja', 'TEACHER', 1, 0, NULL, '2026-09-01T00:00:00Z'),
(4,  'Hoàng Ngọc Diệp',    'gv.tienganh@actvn.edu.vn',   '$2b$10$DXEvPRcwAB9LtjNsDb6YvOHDKqkKkZo4y0JS0o.AB4Kq1B0HpdVja', 'TEACHER', 1, 0, NULL, '2026-09-01T00:00:00Z'),
(5,  'Đỗ Văn Khoa',        'gv.toan@actvn.edu.vn',       '$2b$10$DXEvPRcwAB9LtjNsDb6YvOHDKqkKkZo4y0JS0o.AB4Kq1B0HpdVja', 'TEACHER', 1, 0, NULL, '2026-09-01T00:00:00Z'),
(6,  'Vũ Thị Hồng Nhung',  'gv.chunhiem@actvn.edu.vn',   '$2b$10$DXEvPRcwAB9LtjNsDb6YvOHDKqkKkZo4y0JS0o.AB4Kq1B0HpdVja', 'TEACHER', 1, 0, NULL, '2026-09-01T00:00:00Z'),
(7,  'Bùi Anh Tuấn',       'gv.moi@actvn.edu.vn',        '$2b$10$DXEvPRcwAB9LtjNsDb6YvOHDKqkKkZo4y0JS0o.AB4Kq1B0HpdVja', 'TEACHER', 1, 0, NULL, '2026-09-01T00:00:00Z'),
(8,  'Nguyễn Văn Hùng',    'ph01@actvn.edu.vn',          '$2b$10$DXEvPRcwAB9LtjNsDb6YvOHDKqkKkZo4y0JS0o.AB4Kq1B0HpdVja', 'PARENT',  1, 0, NULL, '2026-09-01T00:00:00Z'),
(9,  'Trần Thị Lan',       'ph02@actvn.edu.vn',          '$2b$10$DXEvPRcwAB9LtjNsDb6YvOHDKqkKkZo4y0JS0o.AB4Kq1B0HpdVja', 'PARENT',  1, 0, NULL, '2026-09-01T00:00:00Z'),
(10, 'Lê Văn Nam',         'ph03@actvn.edu.vn',          '$2b$10$DXEvPRcwAB9LtjNsDb6YvOHDKqkKkZo4y0JS0o.AB4Kq1B0HpdVja', 'PARENT',  1, 0, NULL, '2026-09-01T00:00:00Z'),
(11, 'Phạm Thị Hoa',       'ph04@actvn.edu.vn',          '$2b$10$DXEvPRcwAB9LtjNsDb6YvOHDKqkKkZo4y0JS0o.AB4Kq1B0HpdVja', 'PARENT',  1, 0, NULL, '2026-09-01T00:00:00Z'),
(12, 'Vũ Văn Thắng',       'ph05@actvn.edu.vn',          '$2b$10$DXEvPRcwAB9LtjNsDb6YvOHDKqkKkZo4y0JS0o.AB4Kq1B0HpdVja', 'PARENT',  1, 0, NULL, '2026-09-01T00:00:00Z'),
(13, 'Đặng Thị Mai',       'ph06@actvn.edu.vn',          '$2b$10$DXEvPRcwAB9LtjNsDb6YvOHDKqkKkZo4y0JS0o.AB4Kq1B0HpdVja', 'PARENT',  1, 0, NULL, '2026-09-01T00:00:00Z'),
(14, 'Bùi Văn Long',       'ph07@actvn.edu.vn',          '$2b$10$DXEvPRcwAB9LtjNsDb6YvOHDKqkKkZo4y0JS0o.AB4Kq1B0HpdVja', 'PARENT',  1, 0, NULL, '2026-09-01T00:00:00Z'),
(15, 'Phan Thị Hạnh',      'ph08@actvn.edu.vn',          '$2b$10$DXEvPRcwAB9LtjNsDb6YvOHDKqkKkZo4y0JS0o.AB4Kq1B0HpdVja', 'PARENT',  1, 0, NULL, '2026-09-01T00:00:00Z'),
(16, 'Lương Văn Tâm',      'ph09@actvn.edu.vn',          '$2b$10$DXEvPRcwAB9LtjNsDb6YvOHDKqkKkZo4y0JS0o.AB4Kq1B0HpdVja', 'PARENT',  1, 0, NULL, '2026-09-01T00:00:00Z'),
(17, 'Trịnh Thị Bình',     'ph10@actvn.edu.vn',          '$2b$10$DXEvPRcwAB9LtjNsDb6YvOHDKqkKkZo4y0JS0o.AB4Kq1B0HpdVja', 'PARENT',  1, 0, NULL, '2026-09-01T00:00:00Z'),
(18, 'Hoàng Văn Sơn',      'ph11@actvn.edu.vn',          '$2b$10$DXEvPRcwAB9LtjNsDb6YvOHDKqkKkZo4y0JS0o.AB4Kq1B0HpdVja', 'PARENT',  1, 0, NULL, '2026-09-01T00:00:00Z'),
(19, 'Lê Văn Đức',         'ph.vohieuhoa@actvn.edu.vn',  '$2b$10$DXEvPRcwAB9LtjNsDb6YvOHDKqkKkZo4y0JS0o.AB4Kq1B0HpdVja', 'PARENT',  0, 0, NULL, '2026-09-01T00:00:00Z'),
(20, 'Mai Thị Thủy',       'ph.bikhoa@actvn.edu.vn',     '$2b$10$DXEvPRcwAB9LtjNsDb6YvOHDKqkKkZo4y0JS0o.AB4Kq1B0HpdVja', 'PARENT',  1, 5, '2099-12-31T00:00:00Z', '2026-09-01T00:00:00Z');

INSERT INTO classes (id, name, class_code, school_year, homeroom_teacher_id) VALUES
(1, 'An toàn K20-1',     'AT20A', '2023-2028', 2),
(2, 'Công nghệ K19-2',   'CT19B', '2022-2027', 6),
(3, 'Điện tử VT K21-3',  'DT21C', '2024-2029', 3);

INSERT INTO students (id, student_code, full_name, date_of_birth, class_id) VALUES
(1,  'AT200101', 'Nguyễn Minh Anh',   '2005-03-12', 1),
(2,  'AT200102', 'Trần Quốc Bảo',     '2005-07-25', 1),
(3,  'AT200103', 'Lê Thị Cẩm Linh',   '2005-11-02', 1),
(4,  'AT200104', 'Phạm Đức Dũng',     '2005-01-18', 1),
(5,  'AT200105', 'Hoàng Thu Hà',      '2005-09-30', 1),
(6,  'CT190201', 'Vũ Hải Đăng',       '2004-05-14', 2),
(7,  'CT190202', 'Đặng Phương Thảo',  '2004-08-21', 2),
(8,  'CT190203', 'Bùi Gia Huy',       '2004-12-09', 2),
(9,  'CT190204', 'Ngô Thùy Vy',       '2004-02-27', 2),
(10, 'CT190205', 'Đỗ Tuấn Kiệt',      '2004-10-06', 2),
(11, 'DT210301', 'Phan Thanh Tùng',   '2006-04-16', 3),
(12, 'DT210302', 'Lương Mai Chi',     '2006-06-28', 3),
(13, 'DT210303', 'Trịnh Bảo Ngọc',    '2006-01-09', 3),
(14, 'DT210304', 'Đinh Công Phúc',    '2006-11-23', 3),
(15, 'DT210305', 'Nguyễn Hải Yến',    '2006-08-04', 3);

-- Học sinh 5, 9, 10, 14 chưa liên kết (demo tạo liên kết); ph11 (id 18) chưa có con.
INSERT INTO parent_students (parent_id, student_id, relationship) VALUES
(8,  1,  'PARENT'),
(8,  15, 'PARENT'),
(9,  2,  'PARENT'),
(10, 3,  'PARENT'),
(11, 4,  'PARENT'),
(12, 6,  'PARENT'),
(13, 7,  'PARENT'),
(14, 8,  'PARENT'),
(15, 11, 'PARENT'),
(16, 12, 'GUARDIAN'),
(17, 13, 'PARENT');

INSERT INTO subjects (id, name, subject_code) VALUES
(1,  'Triết học Mác - Lê nin',             'AT1LLLM1'),
(2,  'Vật lý đại cương 1',                 'ATCBLH1'),
(3,  'Vật lý đại cương 2',                 'ATCBLH2'),
(4,  'Tiếng Anh 1',                        'ATCBNN1'),
(5,  'Tiếng Anh 2',                        'ATCBNN2'),
(6,  'Tiếng Anh 3',                        'ATCBNN6'),
(7,  'Toán rời rạc',                       'ATCBTT6'),
(8,  'Toán xác suất thống kê',             'ATCBTT8'),
(9,  'Lịch sử Đảng cộng sản Việt Nam',     'AT1LLDL1'),
(10, 'Kinh tế chính trị Mác - Lênin',      'AT1LLLM2'),
(11, 'Chủ nghĩa xã hội khoa học',          'AT1LLLM3'),
(12, 'Tư tưởng Hồ Chí Minh',               'ATLLTH1');

-- Mỗi môn một giáo viên dạy cả 3 lớp. DT21C chưa phân công Tư tưởng Hồ Chí Minh (để demo tạo/xóa).
WITH m(subject_code, teacher_id) AS (VALUES
  ('AT1LLLM1', 2), ('AT1LLLM2', 2), ('AT1LLLM3', 2), ('AT1LLDL1', 2), ('ATLLTH1', 2),
  ('ATCBLH1', 3), ('ATCBLH2', 3),
  ('ATCBNN1', 4), ('ATCBNN2', 4), ('ATCBNN6', 4),
  ('ATCBTT6', 5), ('ATCBTT8', 5))
INSERT INTO teaching_assignments (teacher_id, class_id, subject_id)
SELECT m.teacher_id, c.id, s.id
FROM m
JOIN subjects s ON s.subject_code = m.subject_code
CROSS JOIN classes c
WHERE NOT (c.id = 3 AND m.subject_code = 'ATLLTH1');

-- Buổi = 3 tiết: slot 1 = tiết 1-3, 2 = 4-6 (sáng), 3 = 7-9, 4 = 10-12 (chiều).
-- Phòng: tầng 3/4/5 theo lớp, số phòng 1-4.
WITH sess(term, subject_code, weekday, slot) AS (VALUES
  ('HK1','ATCBNN1',2,1), ('HK1','ATCBNN1',4,1), ('HK1','ATCBNN1',6,1),
  ('HK1','ATCBNN2',3,1), ('HK1','ATCBNN2',5,1), ('HK1','ATCBNN2',6,2),
  ('HK1','AT1LLLM1',2,2), ('HK1','AT1LLLM1',4,2),
  ('HK1','AT1LLLM2',3,2), ('HK1','AT1LLLM2',5,2),
  ('HK1','ATCBLH1',2,3),  ('HK1','ATCBLH1',4,3),
  ('HK1','ATCBTT6',3,3),  ('HK1','ATCBTT6',5,3),
  ('HK2','ATCBNN6',2,1),  ('HK2','ATCBNN6',4,1),  ('HK2','ATCBNN6',6,1),
  ('HK2','AT1LLLM3',2,2), ('HK2','AT1LLLM3',4,2),
  ('HK2','AT1LLDL1',3,1), ('HK2','AT1LLDL1',5,1),
  ('HK2','ATLLTH1',3,2),  ('HK2','ATLLTH1',5,2),
  ('HK2','ATCBLH2',2,3),  ('HK2','ATCBLH2',4,3),
  ('HK2','ATCBTT8',3,3),  ('HK2','ATCBTT8',5,3)),
p(off) AS (VALUES (0), (1), (2))
INSERT INTO timetable (class_id, subject_id, weekday, period, room, school_year, term)
SELECT c.id, s.id, sess.weekday, (sess.slot - 1) * 3 + 1 + p.off,
       CAST((c.id + 2) * 100 + (s.id % 4) + 1 AS TEXT), '2024-2025', sess.term
FROM sess
CROSS JOIN p
JOIN subjects s ON s.subject_code = sess.subject_code
CROSS JOIN classes c
WHERE NOT (c.id = 3 AND sess.subject_code = 'ATLLTH1');

-- Điểm sinh xác định (5.0-9.0). Học sinh 5, 10, 15 chưa có điểm CK (để thử ô trống).
WITH subject_term(subject_code, term) AS (VALUES
  ('AT1LLLM1','HK1'), ('AT1LLLM2','HK1'), ('ATCBLH1','HK1'), ('ATCBNN1','HK1'), ('ATCBNN2','HK1'), ('ATCBTT6','HK1'),
  ('AT1LLLM3','HK2'), ('AT1LLDL1','HK2'), ('ATLLTH1','HK2'), ('ATCBLH2','HK2'), ('ATCBNN6','HK2'), ('ATCBTT8','HK2')),
ty(score_type, n) AS (VALUES ('TX1', 1), ('TX2', 2), ('GK', 3), ('CK', 4))
INSERT INTO grades (student_id, subject_id, school_year, term, score_type, score, updated_by, updated_at)
SELECT s.id, sb.id, '2024-2025', subject_term.term, ty.score_type,
       ROUND(5.0 + ((s.id * 7 + sb.id * 13 + ty.n * 5) % 41) / 10.0, 1),
       ta.teacher_id, '2025-01-15T03:00:00Z'
FROM students s
JOIN teaching_assignments ta ON ta.class_id = s.class_id
JOIN subjects sb ON sb.id = ta.subject_id
JOIN subject_term ON subject_term.subject_code = sb.subject_code
CROSS JOIN ty
WHERE NOT (ty.score_type = 'CK' AND s.id % 5 = 0);

-- Điểm danh 14 ngày gần nhất, chỉ T2-T6, KHÔNG gồm hôm nay (để GVCN demo điểm danh hôm nay).
WITH RECURSIVE n(i) AS (SELECT 1 UNION ALL SELECT i + 1 FROM n WHERE i < 14),
days(dt) AS (SELECT date('now', '+7 hours', '-' || i || ' days') FROM n)
INSERT INTO attendance (student_id, date, status, recorded_by, created_at)
SELECT s.id, days.dt,
       CASE
         WHEN (s.id * 3 + CAST(strftime('%j', days.dt) AS INTEGER)) % 17 = 0 THEN 'UNEXCUSED'
         WHEN (s.id * 3 + CAST(strftime('%j', days.dt) AS INTEGER)) % 11 = 0 THEN 'EXCUSED'
         WHEN (s.id * 3 + CAST(strftime('%j', days.dt) AS INTEGER)) % 7  = 0 THEN 'LATE'
         ELSE 'PRESENT'
       END,
       c.homeroom_teacher_id,
       strftime('%Y-%m-%dT%H:%M:%SZ', days.dt || ' 02:00:00')
FROM students s
JOIN classes c ON c.id = s.class_id
CROSS JOIN days
WHERE CAST(strftime('%w', days.dt) AS INTEGER) BETWEEN 1 AND 5;

-- Đơn nghỉ đủ 3 trạng thái (PENDING có from_date từ hôm nay trở đi).
INSERT INTO leave_requests (student_id, parent_id, from_date, to_date, reason, status, reviewed_by, reviewed_at, created_at) VALUES
(1,  8,  date('now','+7 hours','+3 days'), date('now','+7 hours','+4 days'),
 'Con bị sốt, xin nghỉ học để đi khám bệnh', 'PENDING', NULL, NULL, strftime('%Y-%m-%dT%H:%M:%SZ','now','-1 days')),
(15, 8,  date('now','+7 hours','+7 days'), date('now','+7 hours','+7 days'),
 'Gia đình có việc quan trọng, xin phép cho con nghỉ một buổi', 'PENDING', NULL, NULL, strftime('%Y-%m-%dT%H:%M:%SZ','now','-1 days')),
(3,  10, date('now','+7 hours','+2 days'), date('now','+7 hours','+2 days'),
 'Con có lịch khám sức khỏe định kỳ tại bệnh viện', 'PENDING', NULL, NULL, strftime('%Y-%m-%dT%H:%M:%SZ','now','-2 days')),
(7,  13, date('now','+7 hours','+5 days'), date('now','+7 hours','+6 days'),
 'Con tham dự lễ cưới của người thân ở tỉnh khác', 'PENDING', NULL, NULL, strftime('%Y-%m-%dT%H:%M:%SZ','now','-1 days')),
(2,  9,  date('now','+7 hours','-6 days'), date('now','+7 hours','-5 days'),
 'Con bị cảm cúm, bác sĩ dặn nghỉ ngơi ở nhà', 'APPROVED', 2, strftime('%Y-%m-%dT%H:%M:%SZ','now','-8 days'), strftime('%Y-%m-%dT%H:%M:%SZ','now','-9 days')),
(11, 15, date('now','+7 hours','-4 days'), date('now','+7 hours','-3 days'),
 'Gia đình đưa con về quê giải quyết việc gia đình', 'APPROVED', 3, strftime('%Y-%m-%dT%H:%M:%SZ','now','-6 days'), strftime('%Y-%m-%dT%H:%M:%SZ','now','-7 days')),
(6,  12, date('now','+7 hours','-10 days'), date('now','+7 hours','-9 days'),
 'Con xin nghỉ để đi du lịch cùng gia đình', 'REJECTED', 6, strftime('%Y-%m-%dT%H:%M:%SZ','now','-12 days'), strftime('%Y-%m-%dT%H:%M:%SZ','now','-13 days'));

-- Đơn đã duyệt thì các ngày đó ghi Nghỉ có phép.
UPDATE attendance SET status = 'EXCUSED'
WHERE (student_id = 2  AND date BETWEEN date('now','+7 hours','-6 days') AND date('now','+7 hours','-5 days'))
   OR (student_id = 11 AND date BETWEEN date('now','+7 hours','-4 days') AND date('now','+7 hours','-3 days'));

INSERT INTO announcements (class_id, author_id, title, content, created_at) VALUES
(1, 2, 'Lịch kiểm tra giữa kỳ',   'Lớp chuẩn bị kiểm tra giữa kỳ vào tuần sau, các bạn ôn tập theo đề cương đã gửi.', strftime('%Y-%m-%dT%H:%M:%SZ','now','-5 days')),
(1, 2, 'Họp phụ huynh đầu kỳ',    'Nhà trường tổ chức họp phụ huynh vào sáng thứ Bảy tuần này tại phòng 301.',        strftime('%Y-%m-%dT%H:%M:%SZ','now','-2 days')),
(2, 6, 'Nộp bài tập lớn',         'Các nhóm nộp bài tập lớn trước thứ Sáu tuần này, nộp muộn sẽ bị trừ điểm.',        strftime('%Y-%m-%dT%H:%M:%SZ','now','-4 days')),
(3, 3, 'Thay đổi phòng học',      'Tuần này lớp học môn Vật lý đại cương chuyển sang phòng 502, giờ học không đổi.',   strftime('%Y-%m-%dT%H:%M:%SZ','now','-3 days'));

-- Nhật ký kiểm toán mẫu (append-only).
INSERT INTO audit_logs (student_id, actor_user_id, action, entity_type, entity_id, old_value, new_value, created_at) VALUES
(1,    1, 'CREATE', 'parent_students', '8-1',   NULL, '{"parentId":"8","studentId":"1","relationship":"PARENT"}',    strftime('%Y-%m-%dT%H:%M:%SZ','now','-30 days')),
(12,   1, 'CREATE', 'parent_students', '16-12', NULL, '{"parentId":"16","studentId":"12","relationship":"GUARDIAN"}', strftime('%Y-%m-%dT%H:%M:%SZ','now','-30 days')),
(NULL, 1, 'CREATE', 'teaching_assignments',
   (SELECT CAST(id AS TEXT) FROM teaching_assignments WHERE class_id = 1 AND subject_id = 1),
   NULL, '{"teacherId":"2","classId":"1","subjectId":"1"}', strftime('%Y-%m-%dT%H:%M:%SZ','now','-30 days')),
(2,    2, 'UPDATE', 'leave_requests',
   (SELECT CAST(id AS TEXT) FROM leave_requests WHERE student_id = 2),
   '{"status":"PENDING"}', '{"status":"APPROVED"}', strftime('%Y-%m-%dT%H:%M:%SZ','now','-8 days')),
(6,    6, 'UPDATE', 'leave_requests',
   (SELECT CAST(id AS TEXT) FROM leave_requests WHERE student_id = 6),
   '{"status":"PENDING"}', '{"status":"REJECTED"}', strftime('%Y-%m-%dT%H:%M:%SZ','now','-12 days'));

-- Sửa điểm TX1 Triết của học sinh 1: ghi audit (giá trị cũ) rồi mới cập nhật.
INSERT INTO audit_logs (student_id, actor_user_id, action, entity_type, entity_id, old_value, new_value, created_at)
SELECT 1, 2, 'UPDATE', 'grades', CAST(g.id AS TEXT),
       json_object('score', g.score), json_object('score', 8.5), strftime('%Y-%m-%dT%H:%M:%SZ','now','-3 days')
FROM grades g
WHERE g.student_id = 1 AND g.subject_id = 1 AND g.term = 'HK1' AND g.score_type = 'TX1';

UPDATE grades SET score = 8.5, updated_at = strftime('%Y-%m-%dT%H:%M:%SZ','now','-3 days')
WHERE student_id = 1 AND subject_id = 1 AND term = 'HK1' AND score_type = 'TX1';
