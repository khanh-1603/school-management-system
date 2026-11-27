import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../app/AuthContext';
import { getHomePathByRole } from '../constants/roles';

/**
 * Trang báo lỗi 403 khi tài khoản đã đăng nhập nhưng truy cập nhánh không đúng vai trò.
 */
export function ForbiddenPage() {
  const { user } = useAuth();
  const homePath = user ? getHomePathByRole(user.role) : '/login';

  return (
    <main>
      <h1>403 — Bạn không có quyền truy cập</h1>
      <p>Tài khoản hiện tại không được phép truy cập trang này.</p>
      <Link to={homePath}>Quay về trang chính</Link>
    </main>
  );
}

/**
 * Trang báo lỗi 404 khi đường dẫn không tồn tại.
 */
export function NotFoundPage() {
  const { user } = useAuth();
  const homePath = user ? getHomePathByRole(user.role) : '/login';

  return (
    <main>
      <h1>404 — Không tìm thấy trang</h1>
      <p>Đường dẫn bạn yêu cầu không tồn tại.</p>
      <Link to={homePath}>Quay về trang chính</Link>
    </main>
  );
}

/**
 * Trang giữ chỗ tạm thời cho màn hình chính của từng vai trò trước khi dựng giao diện thật.
 * Hỗ trợ kiểm thử thông tin phiên, nút đăng xuất và kiểm tra chặn quyền chéo vai trò.
 *
 * @param {object} props
 * @param {string} props.title Tiêu đề màn hình giữ chỗ
 */
export function RoleHomePlaceholder({ title }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [loggingOut, setLoggingOut] = useState(false);
  const [logoutError, setLogoutError] = useState('');

  async function handleLogout() {
    setLogoutError('');
    setLoggingOut(true);
    try {
      await logout();
      navigate('/login', { replace: true });
    } catch {
      setLogoutError('Chưa xác nhận được đăng xuất. Vui lòng thử lại.');
    } finally {
      setLoggingOut(false);
    }
  }

  return (
    <main>
      <h1>{title}</h1>
      <p>
        Đang đăng nhập: <strong>{user?.fullName}</strong> ({user?.role}) — ID: {user?.id}
      </p>

      <button type="button" onClick={handleLogout} disabled={loggingOut}>
        {loggingOut ? 'Đang đăng xuất...' : 'Đăng xuất'}
      </button>
      {logoutError && <p role="alert">{logoutError}</p>}

      <hr />
      <p>Liên kết kiểm thử phân quyền route:</p>
      <ul>
        <li><Link to="/parent/students">Vào khu Phụ huynh (/parent/students)</Link></li>
        <li><Link to="/teacher/classes">Vào khu Giáo viên (/teacher/classes)</Link></li>
        <li><Link to="/admin/users">Vào khu Admin (/admin/users)</Link></li>
        <li><Link to="/duong-dan-khong-ton-tai">Thử trang 404</Link></li>
      </ul>
    </main>
  );
}