import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../app/AuthContext';
import { getHomePathByRole } from '../constants/roles';

/** Biểu thức kiểm tra định dạng email cơ bản. */
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/**
 * Trang đăng nhập (`AUTH-01`).
 * Dùng giao diện khung tối thiểu để kiểm thử luồng xác thực trước khi dựng UI chính thức.
 */
export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  /**
   * Kiểm tra nhanh dữ liệu đầu vào trước khi gửi request đăng nhập.
   *
   * @returns {string} Chuỗi lỗi nếu không hợp lệ, hoặc chuỗi rỗng nếu hợp lệ
   */
  function validate() {
    const normalizedEmail = email.trim().toLowerCase();
    if (normalizedEmail.length < 5 || normalizedEmail.length > 50 || !EMAIL_REGEX.test(normalizedEmail)) {
      return 'Email phải từ 5 đến 50 ký tự và đúng định dạng.';
    }
    if (password.length < 8) {
      return 'Mật khẩu phải có ít nhất 8 ký tự.';
    }
    return '';
  }

  /**
   * Xử lý submit form đăng nhập.
   *
   * @param {import('react').SyntheticEvent} event Sự kiện submit form
   */
  async function handleSubmit(event) {
    event.preventDefault();
    setErrorMessage('');

    const validationError = validate();
    if (validationError) {
      setErrorMessage(validationError);
      return;
    }

    setSubmitting(true);
    try {
      const loggedInUser = await login({ email, password });
      setPassword('');
      navigate(getHomePathByRole(loggedInUser.role), { replace: true });
    } catch (err) {
      setErrorMessage(err.message || 'Đăng nhập thất bại. Vui lòng thử lại.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main>
      <h1>Ứng dụng kết nối phụ huynh và nhà trường</h1>
      <h2>Đăng nhập</h2>

      <form onSubmit={handleSubmit} noValidate>
        <div>
          <label htmlFor="email">Email</label>
          <input
            id="email"
            type="email"
            autoComplete="username"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            disabled={submitting}
          />
        </div>

        <div>
          <label htmlFor="password">Mật khẩu</label>
          <input
            id="password"
            type={showPassword ? 'text' : 'password'}
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            disabled={submitting}
          />
          <button
            type="button"
            onClick={() => setShowPassword((prev) => !prev)}
            disabled={submitting}
          >
            {showPassword ? 'Ẩn' : 'Hiện'}
          </button>
        </div>

        {errorMessage && <p role="alert">{errorMessage}</p>}

        <button type="submit" disabled={submitting}>
          {submitting ? 'Đang đăng nhập...' : 'Đăng nhập'}
        </button>
      </form>
    </main>
  );
}