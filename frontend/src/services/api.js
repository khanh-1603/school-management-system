/**
 * Lớp lỗi chuẩn hóa từ API backend hoặc lỗi mạng.
 * Frontend xử lý phân nhánh dựa trên thuộc tính `code`, không so sánh chuỗi `message`.
 */
export class ApiError extends Error {
  /**
   * @param {string} code Mã lỗi thống nhất (VALIDATION_ERROR, UNAUTHENTICATED, FORBIDDEN, NOT_FOUND, CONFLICT, INTERNAL_ERROR, NETWORK_ERROR)
   * @param {string} message Thông báo lỗi hiển thị cho người dùng
   * @param {number} status HTTP status code (0 nếu là lỗi mạng)
   * @param {Array<{field: string, message: string}> | null} [fields=null] Danh sách lỗi theo từng trường (tùy chọn)
   */
  constructor(code, message, status, fields = null) {
    super(message);
    this.name = 'ApiError';
    this.code = code;
    this.status = status;
    this.fields = fields;
  }
}

/**
 * Ghép query string từ object, tự động bỏ qua các giá trị null, undefined hoặc chuỗi rỗng.
 *
 * @param {Record<string, unknown> | undefined} params
 * @returns {string} Chuỗi query bắt đầu bằng '?' hoặc chuỗi rỗng
 */
function buildQueryString(params) {
  if (!params) return '';
  const searchParams = new URLSearchParams();

  Object.entries(params).forEach(([key, value]) => {
    if (value === null || value === undefined) return;
    const str = String(value).trim();
    if (str !== '') {
      searchParams.append(key, str);
    }
  });

  const query = searchParams.toString();
  return query ? `?${query}` : '';
}

/**
 * Hàm gọi API trung tâm cho toàn bộ ứng dụng.
 *
 * @param {string} path Đường dẫn sau tiền tố '/api', ví dụ '/auth/me'
 * @param {object} [options]
 * @param {'GET'|'POST'|'PUT'|'PATCH'|'DELETE'} [options.method='GET']
 * @param {Record<string, unknown>} [options.params] Query parameters (tùy chọn)
 * @param {object} [options.body] Request body dạng object (tự serialize sang JSON)
 * @param {AbortSignal} [options.signal] Signal của AbortController để hủy request cũ
 * @returns {Promise<any>} Trường `data` trong envelope thành công
 * @throws {ApiError} Khi server trả lỗi hoặc mất kết nối mạng
 */
export async function request(path, { method = 'GET', params, body, signal } = {}) {
  const url = `/api${path}${buildQueryString(params)}`;

  const headers = {
    Accept: 'application/json',
  };

  const fetchOptions = {
    method,
    credentials: 'include',
    headers,
    signal,
  };

  if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
    fetchOptions.body = JSON.stringify(body);
  }

  let response;
  try {
    response = await fetch(url, fetchOptions);
  } catch (err) {
    // Giữ nguyên AbortError để component biết request bị hủy chủ động, không báo lỗi UI
    if (err.name === 'AbortError') {
      throw err;
    }
    throw new ApiError('NETWORK_ERROR', 'Không thể kết nối đến máy chủ. Vui lòng thử lại.', 0);
  }

  let payload = null;
  try {
    payload = await response.json();
  } catch {
    throw new ApiError('INTERNAL_ERROR', 'Phản hồi từ máy chủ không hợp lệ.', response.status);
  }

  if (!response.ok || !payload?.success) {
    const err = payload?.error;
    throw new ApiError(
      err?.code || 'INTERNAL_ERROR',
      err?.message || 'Đã xảy ra lỗi hệ thống. Vui lòng thử lại.',
      response.status,
      err?.fields || null
    );
  }

  return payload.data;
}

/** Các phương thức viết tắt cho từng HTTP method. */
export const api = {
  get: (path, options = {}) => request(path, { ...options, method: 'GET' }),
  post: (path, body, options = {}) => request(path, { ...options, method: 'POST', body }),
  put: (path, body, options = {}) => request(path, { ...options, method: 'PUT', body }),
  patch: (path, body, options = {}) => request(path, { ...options, method: 'PATCH', body }),
  delete: (path, options = {}) => request(path, { ...options, method: 'DELETE' }),
};