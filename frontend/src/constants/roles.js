/** Các vai trò tài khoản hợp lệ từ server. */
export const ROLES = Object.freeze({
  PARENT: 'PARENT',
  TEACHER: 'TEACHER',
  ADMIN: 'ADMIN',
});

/** Màn hình vào mặc định tương ứng với từng vai trò sau khi đăng nhập. */
export const ROLE_HOME_PATHS = Object.freeze({
  [ROLES.PARENT]: '/parent/students',
  [ROLES.TEACHER]: '/teacher/classes',
  [ROLES.ADMIN]: '/admin/users',
});

/**
 * Lấy đường dẫn trang chủ theo vai trò người dùng.
 *
 * @param {string | null | undefined} role Vai trò trả về từ server
 * @returns {string} Đường dẫn tương ứng hoặc `/login` nếu vai trò không hợp lệ
 */
export function getHomePathByRole(role) {
  return ROLE_HOME_PATHS[role] || '/login';
}