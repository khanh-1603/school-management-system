import { api } from './api';

/**
 * Kiểm tra phiên đăng nhập hiện tại từ server.
 *
 * @param {object} [options]
 * @param {AbortSignal} [options.signal] Signal để hủy request khi unmount
 * @returns {Promise<{user: {id: string, fullName: string, role: string}}>} Thông tin người dùng hiện tại
 */
export function getMe({ signal } = {}) {
  return api.get('/auth/me', { signal });
}

/**
 * Đăng nhập bằng email và mật khẩu.
 * Email được tự động `trim()` và chuyển về chữ thường; mật khẩu giữ nguyên không `trim()`.
 *
 * @param {object} credentials
 * @param {string} credentials.email Email đăng nhập
 * @param {string} credentials.password Mật khẩu
 * @returns {Promise<{user: {id: string, fullName: string, role: string}}>} Thông tin người dùng sau khi đăng nhập
 */
export function login({ email, password }) {
  return api.post('/auth/login', {
    email: email.trim().toLowerCase(),
    password,
  });
}

/**
 * Đăng xuất và hủy session hiện tại ở server.
 *
 * @returns {Promise<{message: string}>} Thông báo đăng xuất thành công
 */
export function logout() {
  return api.post('/auth/logout');
}