import { api } from './api';

/**
 * Lấy danh sách nhật ký kiểm toán theo các bộ lọc tùy chọn (chỉ dành cho `ADMIN`).
 *
 * @param {object} [filters] Các tiêu chí lọc tùy chọn (giá trị rỗng sẽ tự động bị bỏ qua)
 * @param {string} [filters.studentId] ID học sinh cần lọc
 * @param {'CREATE'|'UPDATE'|'DELETE'|''} [filters.action] Loại hành động
 * @param {string} [filters.entityType] Tên bảng bị tác động (ví dụ: `grades`, `announcements`)
 * @param {string} [filters.entityId] ID bản ghi bị tác động
 * @param {object} [options]
 * @param {AbortSignal} [options.signal] Signal để hủy request cũ khi đổi bộ lọc
 * @returns {Promise<{items: Array<object>}>} Danh sách bản ghi audit log
 */
export function listAuditLogs(filters = {}, { signal } = {}) {
  const { studentId, action, entityType, entityId } = filters;
  return api.get('/admin/audit-logs', {
    params: { studentId, action, entityType, entityId },
    signal,
  });
}