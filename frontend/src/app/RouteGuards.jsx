import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from './AuthContext';
import { getHomePathByRole } from '../constants/roles';
import { ForbiddenPage } from '../pages/StatusPages';

/**
 * Màn hình chờ hoặc báo lỗi kết nối trong lúc kiểm tra phiên ban đầu.
 * Không tự chuyển về `/login` khi gặp lỗi mạng hoặc 500.
 */
function SessionCheckGate({ status, sessionError, onRetry }) {
  if (status === 'checking') {
    return <p>Đang kiểm tra phiên...</p>;
  }

  return (
    <div>
      <p>Không kiểm tra được phiên: {sessionError}</p>
      <button type="button" onClick={onRetry}>
        Thử lại
      </button>
    </div>
  );
}

/**
 * Route guard yêu cầu người dùng đã đăng nhập và (tùy chọn) thuộc danh sách vai trò cho phép.
 *
 * @param {object} props
 * @param {string[]} [props.allowedRoles] Danh sách vai trò được phép truy cập nhánh route này
 */
export function RequireAuth({ allowedRoles }) {
  const { status, user, sessionError, checkSession } = useAuth();

  if (status === 'checking' || status === 'error') {
    return (
      <SessionCheckGate
        status={status}
        sessionError={sessionError}
        onRetry={checkSession}
      />
    );
  }

  if (status === 'unauthenticated' || !user) {
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles && !allowedRoles.includes(user.role)) {
    return <ForbiddenPage />;
  }

  return <Outlet />;
}

/**
 * Route guard cho các trang chỉ dành cho người chưa đăng nhập (như `/login`).
 * Nếu đã có phiên hợp lệ, tự động chuyển hướng sang màn hình mặc định theo vai trò.
 */
export function GuestOnly() {
  const { status, user, sessionError, checkSession } = useAuth();

  if (status === 'checking' || status === 'error') {
    return (
      <SessionCheckGate
        status={status}
        sessionError={sessionError}
        onRetry={checkSession}
      />
    );
  }

  if (status === 'authenticated' && user) {
    return <Navigate to={getHomePathByRole(user.role)} replace />;
  }

  return <Outlet />;
}