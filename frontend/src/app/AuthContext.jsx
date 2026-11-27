import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react';
import { ApiError } from '../services/api';
import * as authService from '../services/authService';

/** Context lưu trạng thái xác thực và các hành động phiên đăng nhập. */
const AuthContext = createContext(null);

/**
 * Provider bọc toàn bộ ứng dụng để kiểm tra phiên từ server và chia sẻ thông tin người dùng.
 *
 * @param {object} props
 * @param {import('react').ReactNode} props.children Cây component con
 */
export function AuthProvider({ children }) {
  /**
   * Trạng thái phiên:
   * - `checking`: đang gọi `/api/auth/me`
   * - `authenticated`: đã có phiên hợp lệ
   * - `unauthenticated`: chưa đăng nhập (401 hoặc đã đăng xuất)
   * - `error`: lỗi mạng hoặc 500 khi kiểm tra phiên
   */
  const [status, setStatus] = useState('checking');
  const [user, setUser] = useState(null);
  const [sessionError, setSessionError] = useState(null);

  /** Giữ AbortController hiện hành để hủy request kiểm tra phiên cũ khi gọi lại hoặc unmount. */
  const abortRef = useRef(null);

  /**
   * Gọi `GET /api/auth/me` để đồng bộ trạng thái phiên từ server.
   */
  const checkSession = useCallback(async () => {
    abortRef.current?.abort();
    const controller = new AbortController();
    abortRef.current = controller;

    setStatus('checking');
    setSessionError(null);

    try {
      const data = await authService.getMe({ signal: controller.signal });
      setUser(data.user);
      setStatus('authenticated');
    } catch (err) {
      if (err.name === 'AbortError') return;

      if (err instanceof ApiError && err.code === 'UNAUTHENTICATED') {
        setUser(null);
        setStatus('unauthenticated');
        return;
      }

      setUser(null);
      setSessionError(err.message || 'Không kiểm tra được phiên.');
      setStatus('error');
    }
  }, []);

  useEffect(() => {
    checkSession();
    return () => {
      abortRef.current?.abort();
    };
  }, [checkSession]);

  /**
   * Xóa sạch dữ liệu người dùng ở client khi phát hiện hết phiên (401) từ bất kỳ API nào.
   */
  const clearSession = useCallback(() => {
    setUser(null);
    setSessionError(null);
    setStatus('unauthenticated');
  }, []);

  /**
   * Thực hiện đăng nhập và cập nhật trạng thái người dùng từ phản hồi server.
   *
   * @param {{ email: string, password: string }} credentials Thông tin đăng nhập
   * @returns {Promise<{ id: string, fullName: string, role: string }>} Người dùng đã đăng nhập
   */
  const login = useCallback(async (credentials) => {
    const data = await authService.login(credentials);
    setUser(data.user);
    setSessionError(null);
    setStatus('authenticated');
    return data.user;
  }, []);

  /**
   * Thực hiện đăng xuất.
   * Nếu server trả 200 hoặc 401 (đã hết phiên sẵn) thì xóa state và coi như đăng xuất xong.
   * Nếu lỗi mạng/500 thì ném lỗi để giao diện báo người dùng thử lại.
   */
  const logout = useCallback(async () => {
    try {
      await authService.logout();
      clearSession();
    } catch (err) {
      if (err instanceof ApiError && err.code === 'UNAUTHENTICATED') {
        clearSession();
        return;
      }
      throw err;
    }
  }, [clearSession]);

  const value = {
    status,
    user,
    sessionError,
    checkSession,
    login,
    logout,
    clearSession,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

/**
 * Hook truy cập trạng thái và hành động xác thực từ {@link AuthProvider}.
 *
 * @returns {{
 *   status: 'checking' | 'authenticated' | 'unauthenticated' | 'error',
 *   user: { id: string, fullName: string, role: string } | null,
 *   sessionError: string | null,
 *   checkSession: () => Promise<void>,
 *   login: (credentials: { email: string, password: string }) => Promise<object>,
 *   logout: () => Promise<void>,
 *   clearSession: () => void
 * }}
 */
export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth phải được gọi bên trong AuthProvider.');
  }
  return ctx;
}