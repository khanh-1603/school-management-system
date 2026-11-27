import { api } from './api';

/**
 * Lấy danh sách thông báo của một lớp học.
 * Áp dụng cho `PARENT` có con học tại lớp hoặc `TEACHER` là GVCN của lớp đó.
 *
 * @param {string} classId ID lớp học
 * @param {object} [options]
 * @param {AbortSignal} [options.signal] Signal để hủy request cũ khi đổi lớp/con
 * @returns {Promise<{items: Array<{id: string, title: string, content: string, createdAt: string}>}>} Danh sách thông báo
 */
export function listClassAnnouncements(classId, { signal } = {}) {
  return api.get(`/classes/${encodeURIComponent(classId)}/announcements`, { signal });
}

/**
 * Đăng thông báo mới cho lớp học (chỉ dành cho GVCN của lớp).
 *
 * @param {string} classId ID lớp học chủ nhiệm
 * @param {object} payload
 * @param {string} payload.title Tiêu đề thông báo (5–50 ký tự sau `trim`)
 * @param {string} payload.content Nội dung thông báo (10–1000 ký tự sau `trim`)
 * @returns {Promise<{announcement: {id: string, classId: string, title: string}}>} Thông báo vừa tạo
 */
export function createClassAnnouncement(classId, { title, content }) {
  return api.post(`/classes/${encodeURIComponent(classId)}/announcements`, {
    title,
    content,
  });
}