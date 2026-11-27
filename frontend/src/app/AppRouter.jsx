import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { ROLES, getHomePathByRole } from '../constants/roles';
import { AuthProvider, useAuth } from './AuthContext';
import { GuestOnly, RequireAuth } from './RouteGuards';
import { LoginPage } from '../pages/LoginPage';
import { NotFoundPage, RoleHomePlaceholder } from '../pages/StatusPages';

/**
 * Xử lý đường dẫn gốc `/`: tự động điều hướng về màn hình theo role hoặc `/login`.
 */
function RootRedirect() {
  const { status, user, sessionError, checkSession } = useAuth();

  if (status === 'checking') {
    return <p>Đang kiểm tra phiên...</p>;
  }
  if (status === 'error') {
    return (
      <div>
        <p>Không kiểm tra được phiên: {sessionError}</p>
        <button type="button" onClick={checkSession}>Thử lại</button>
      </div>
    );
  }
  if (status === 'authenticated' && user) {
    return <Navigate to={getHomePathByRole(user.role)} replace />;
  }
  return <Navigate to="/login" replace />;
}

/**
 * Bộ định tuyến trung tâm của ứng dụng, áp dụng Route Guard theo vai trò từ server.
 */
export function AppRouter() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/" element={<RootRedirect />} />

          {/* Chỉ người chưa đăng nhập */}
          <Route element={<GuestOnly />}>
            <Route path="/login" element={<LoginPage />} />
          </Route>

          {/* Nhánh Phụ huynh */}
          <Route element={<RequireAuth allowedRoles={[ROLES.PARENT]} />}>
            <Route
              path="/parent/students"
              element={<RoleHomePlaceholder title="P-01 · Học sinh liên kết" />}
            />
          </Route>

          {/* Nhánh Giáo viên */}
          <Route element={<RequireAuth allowedRoles={[ROLES.TEACHER]} />}>
            <Route
              path="/teacher/classes"
              element={<RoleHomePlaceholder title="T-01 · Lớp/môn được phân công" />}
            />
          </Route>

          {/* Nhánh Quản trị viên */}
          <Route element={<RequireAuth allowedRoles={[ROLES.ADMIN]} />}>
            <Route
              path="/admin/users"
              element={<RoleHomePlaceholder title="A-02 · Tài khoản" />}
            />
          </Route>

          {/* Mọi đường dẫn không khớp */}
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}