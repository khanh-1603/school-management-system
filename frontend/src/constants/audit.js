/**
 * Danh sách hành động hợp lệ của bảng `audit_logs` (khớp enum backend).
 *
 * @readonly
 * @type {ReadonlyArray<{ value: 'CREATE' | 'UPDATE' | 'DELETE', label: string }>}
 */
export const AUDIT_ACTION_OPTIONS = Object.freeze([
  { value: 'CREATE', label: 'Tạo mới (CREATE)' },
  { value: 'UPDATE', label: 'Cập nhật (UPDATE)' },
  { value: 'DELETE', label: 'Xóa / Gỡ (DELETE)' },
]);

/**
 * Tập giá trị `action` hợp lệ dùng cho bước kiểm tra bộ lọc phía client.
 *
 * @readonly
 * @type {ReadonlyArray<string>}
 */
export const AUDIT_ACTION_VALUES = Object.freeze(
  AUDIT_ACTION_OPTIONS.map((option) => option.value),
);

/**
 * Danh sách 11 bảng hợp lệ cho bộ lọc `entityType` của `GET /api/admin/audit-logs`.
 *
 * @readonly
 * @type {ReadonlyArray<{ value: string, label: string }>}
 */
export const AUDIT_ENTITY_TYPE_OPTIONS = Object.freeze([
  { value: 'users', label: 'Tài khoản (users)' },
  { value: 'classes', label: 'Lớp học (classes)' },
  { value: 'students', label: 'Học sinh (students)' },
  { value: 'parent_students', label: 'Liên kết phụ huynh (parent_students)' },
  { value: 'subjects', label: 'Môn học (subjects)' },
  { value: 'teaching_assignments', label: 'Phân công giảng dạy (teaching_assignments)' },
  { value: 'timetable', label: 'Thời khóa biểu (timetable)' },
  { value: 'grades', label: 'Điểm số (grades)' },
  { value: 'attendance', label: 'Điểm danh (attendance)' },
  { value: 'leave_requests', label: 'Đơn xin nghỉ (leave_requests)' },
  { value: 'announcements', label: 'Thông báo lớp (announcements)' },
]);

/**
 * Tập giá trị `entityType` hợp lệ dùng cho bước kiểm tra bộ lọc phía client.
 *
 * @readonly
 * @type {ReadonlyArray<string>}
 */
export const AUDIT_ENTITY_TYPE_VALUES = Object.freeze(
  AUDIT_ENTITY_TYPE_OPTIONS.map((option) => option.value),
);