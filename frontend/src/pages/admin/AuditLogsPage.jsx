import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../app/AuthContext';
import {
  AUDIT_ACTION_OPTIONS,
  AUDIT_ACTION_VALUES,
  AUDIT_ENTITY_TYPE_OPTIONS,
  AUDIT_ENTITY_TYPE_VALUES,
} from '../../constants/audit';
import { listAuditLogs } from '../../services/auditService';
import { formatUtcToVietnamDateTime } from '../../utils/dateTime';

/**
 * Bản ghi nhật ký kiểm toán trả về từ `GET /api/admin/audit-logs`.
 *
 * @typedef {object} AuditLogItem
 * @property {string} id Mã định danh bản ghi nhật ký.
 * @property {string} actorId ID người dùng thực hiện thao tác.
 * @property {string | null} [studentId] ID học sinh liên quan nếu bản ghi thuộc nghiệp vụ học sinh.
 * @property {'CREATE' | 'UPDATE' | 'DELETE'} action Hành động ghi nhận.
 * @property {string} entityType Tên bảng bị tác động.
 * @property {string} entityId ID bản ghi bị tác động (kiểu TEXT, không phải enum).
 * @property {unknown} oldValue Giá trị trước khi thay đổi (JSON object, chuỗi JSON hoặc `null`).
 * @property {unknown} newValue Giá trị sau khi thay đổi (JSON object, chuỗi JSON hoặc `null`).
 * @property {string} createdAt Thời điểm tạo dạng ISO 8601 UTC.
 */

/**
 * Bộ lọc 4 trường hỗ trợ bởi `GET /api/admin/audit-logs`.
 *
 * @typedef {object} AuditLogFilters
 * @property {string} studentId Lọc theo ID học sinh (chuỗi số nguyên dương nếu nhập).
 * @property {string} action Lọc theo hành động (`CREATE`, `UPDATE`, `DELETE` hoặc rỗng).
 * @property {string} entityType Lọc theo tên bảng trong danh sách 11 bảng (hoặc rỗng).
 * @property {string} entityId Lọc theo ID đối tượng (TEXT tự nhập).
 */

/**
 * Trạng thái lỗi chuẩn hóa cho vùng hiển thị của trang A-07.
 *
 * @typedef {object} AuditPageError
 * @property {'validation' | 'forbidden' | 'not_found' | 'server'} kind Phân loại lỗi để quyết định nút hành động.
 * @property {string} message Thông báo lỗi hiển thị cho người dùng.
 * @property {Array<{ field: string, message: string }>} fields Danh sách lỗi chi tiết từng trường nếu backend trả về.
 */

/**
 * Giá trị mặc định (rỗng) của bộ lọc A-07.
 *
 * @readonly
 * @type {Readonly<AuditLogFilters>}
 */
const EMPTY_FILTERS = Object.freeze({
  studentId: '',
  action: '',
  entityType: '',
  entityId: '',
});

/**
 * Biểu thức kiểm tra chuỗi số nguyên dương cho `studentId`.
 *
 * @type {RegExp}
 */
const POSITIVE_INTEGER_REGEX = /^[1-9]\d*$/;

/**
 * Định dạng `oldValue` hoặc `newValue` để hiển thị chỉ đọc trong khung chi tiết.
 * Hỗ trợ cả trường hợp backend trả JSON object đã parse, chuỗi JSON thô hoặc `null`.
 *
 * @param {unknown} value Giá trị `oldValue` hoặc `newValue` từ bản ghi nhật ký.
 * @returns {string} Chuỗi JSON đã thụt dòng hoặc `Không có (null)`.
 */
function formatAuditPayload(value) {
  if (value === null || value === undefined || value === '') {
    return 'Không có (null)';
  }

  if (typeof value === 'string') {
    try {
      const parsed = JSON.parse(value);
      return JSON.stringify(parsed, null, 2);
    } catch {
      return value;
    }
  }

  try {
    return JSON.stringify(value, null, 2);
  } catch {
    return String(value);
  }
}

/**
 * Chuẩn hóa và kiểm tra hợp lệ bộ lọc phía client trước khi gọi API.
 * Backend vẫn là nguồn validate cuối cùng cho mọi tham số.
 *
 * @param {AuditLogFilters} rawFilters Giá trị đang nhập trong form lọc.
 * @returns {{ filters: AuditLogFilters | null, errorMessage: string | null }} Kết quả đã chuẩn hóa hoặc thông báo lỗi.
 */
function normalizeAndValidateFilters(rawFilters) {
  const normalized = {
    studentId: rawFilters.studentId.trim(),
    action: rawFilters.action.trim(),
    entityType: rawFilters.entityType.trim(),
    entityId: rawFilters.entityId.trim(),
  };

  if (normalized.studentId !== '' && !POSITIVE_INTEGER_REGEX.test(normalized.studentId)) {
    return {
      filters: null,
      errorMessage: 'ID học sinh (studentId) phải là số nguyên dương.',
    };
  }

  if (normalized.action !== '' && !AUDIT_ACTION_VALUES.includes(normalized.action)) {
    return {
      filters: null,
      errorMessage: 'Hành động (action) không hợp lệ.',
    };
  }

  if (normalized.entityType !== '' && !AUDIT_ENTITY_TYPE_VALUES.includes(normalized.entityType)) {
    return {
      filters: null,
      errorMessage: 'Loại đối tượng (entityType) không hợp lệ.',
    };
  }

  return {
    filters: normalized,
    errorMessage: null,
  };
}

/**
 * Màn hình `A-07 · Audit log` (`/admin/audit-logs`) dành riêng cho vai trò `ADMIN`.
 *
 * Tính năng:
 * - Lọc theo 4 trường: `studentId`, `action`, `entityType`, `entityId` (tự bỏ trường rỗng).
 * - Hiển thị bảng nhật ký chỉ đọc, chuyển `createdAt` từ ISO 8601 UTC sang giờ Việt Nam.
 * - Xem chi tiết `oldValue` và `newValue` hoàn toàn local từ dòng đã tải.
 * - Dùng `AbortController` để hủy request cũ khi đổi bộ lọc, tải lại hoặc rời trang.
 * - Xử lý lỗi theo `error.code`: `400`, `401` (gọi `clearSession()`), `403`, `404`, `500`/lỗi mạng (có nút Thử lại).
 *
 * @returns {import('react').JSX.Element} Giao diện khung HTML thô của trang nhật ký kiểm toán.
 */
export default function AuditLogsPage() {
  const { user, logout, clearSession } = useAuth();

  /** @type {[AuditLogFilters, import('react').Dispatch<import('react').SetStateAction<AuditLogFilters>>]} */
  const [draftFilters, setDraftFilters] = useState({ ...EMPTY_FILTERS });

  /** @type {[AuditLogFilters, import('react').Dispatch<import('react').SetStateAction<AuditLogFilters>>]} */
  const [appliedFilters, setAppliedFilters] = useState({ ...EMPTY_FILTERS });

  /** Bộ đếm ép chạy lại effect khi bấm Thử lại hoặc Áp dụng lại cùng bộ lọc. */
  const [reloadTick, setReloadTick] = useState(0);

  /** @type {[AuditLogItem[], import('react').Dispatch<import('react').SetStateAction<AuditLogItem[]>>]} */
  const [items, setItems] = useState([]);

  const [isLoading, setIsLoading] = useState(true);
  const [isLoggingOut, setIsLoggingOut] = useState(false);

  /** @type {[AuditPageError | null, import('react').Dispatch<import('react').SetStateAction<AuditPageError | null>>]} */
  const [error, setError] = useState(null);

  /** @type {[AuditLogItem | null, import('react').Dispatch<import('react').SetStateAction<AuditLogItem | null>>]} */
  const [selectedLog, setSelectedLog] = useState(null);

  useEffect(() => {
    const controller = new AbortController();

    async function fetchAuditLogs() {
      setIsLoading(true);
      setError(null);
      setSelectedLog(null);

      try {
        const data = await listAuditLogs(appliedFilters, {
          signal: controller.signal,
        });

        if (controller.signal.aborted) {
          return;
        }

        const nextItems = Array.isArray(data?.items) ? data.items : [];
        setItems(nextItems);
      } catch (err) {
        if (controller.signal.aborted || err?.name === 'AbortError') {
          return;
        }

        const code = err?.code;
        const status = err?.status;

        if (code === 'UNAUTHENTICATED' || status === 401) {
          setItems([]);
          setSelectedLog(null);
          clearSession();
          return;
        }

        if (code === 'FORBIDDEN' || status === 403) {
          setItems([]);
          setSelectedLog(null);
          setError({
            kind: 'forbidden',
            message: err?.message || 'Bạn không có quyền xem nhật ký kiểm toán.',
            fields: [],
          });
          return;
        }

        if (code === 'NOT_FOUND' || status === 404) {
          setItems([]);
          setSelectedLog(null);
          setError({
            kind: 'not_found',
            message: err?.message || 'Không tìm thấy tài nguyên nhật ký.',
            fields: [],
          });
          return;
        }

        if (code === 'VALIDATION_ERROR' || status === 400) {
          setError({
            kind: 'validation',
            message: err?.message || 'Tham số bộ lọc không hợp lệ.',
            fields: Array.isArray(err?.fields) ? err.fields : [],
          });
          return;
        }

        setItems([]);
        setSelectedLog(null);
        setError({
          kind: 'server',
          message: 
          status === 502 || status === 503 || status === 504
            ? 'Không kết nối được tới máy chủ backend. Vui lòng thử lại.'
            : err?.message || 'Không tải được nhật ký kiểm toán. Vui lòng thử lại.',
          fields: [],
        });
      } finally {
        if (!controller.signal.aborted) {
          setIsLoading(false);
        }
      }
    }

    fetchAuditLogs();

    return () => {
      controller.abort();
    };
  }, [appliedFilters, reloadTick, clearSession]);

  /**
   * Cập nhật một trường trong state nháp của bộ lọc.
   *
   * @param {import('react').ChangeEvent<HTMLInputElement | HTMLSelectElement>} event Sự kiện thay đổi input/select.
   * @returns {void}
   */
  function handleFilterChange(event) {
    const { name, value } = event.target;
    setDraftFilters((prev) => ({
      ...prev,
      [name]: value,
    }));
  }

  /**
   * Xử lý submit form bộ lọc: validate nhẹ phía client rồi kích hoạt gọi API.
   *
   * @param {import('react').SyntheticEvent<HTMLFormElement>} event Sự kiện submit form.
   * @returns {void}
   */
  function handleApplyFilters(event) {
    event.preventDefault();

    const { filters, errorMessage } = normalizeAndValidateFilters(draftFilters);
    if (errorMessage || !filters) {
      setError({
        kind: 'validation',
        message: errorMessage || 'Tham số bộ lọc không hợp lệ.',
        fields: [],
      });
      return;
    }

    setError(null);
    setDraftFilters(filters);
    setAppliedFilters(filters);
    setReloadTick((prev) => prev + 1);
  }

  /**
   * Xóa toàn bộ bộ lọc đang nhập và tải lại danh sách mặc định không có query.
   *
   * @returns {void}
   */
  function handleResetFilters() {
    setError(null);
    setSelectedLog(null);
    setDraftFilters({ ...EMPTY_FILTERS });
    setAppliedFilters({ ...EMPTY_FILTERS });
    setReloadTick((prev) => prev + 1);
  }

  /**
   * Gọi lại yêu cầu `GET /api/admin/audit-logs` với bộ lọc đang áp dụng hiện tại.
   *
   * @returns {void}
   */
  function handleRetry() {
    setReloadTick((prev) => prev + 1);
  }

  /**
   * Đăng xuất khỏi phiên làm việc hiện tại.
   *
   * @returns {Promise<void>}
   */
  async function handleLogout() {
    if (isLoggingOut) {
      return;
    }

    setIsLoggingOut(true);
    try {
      await logout();
    } catch (err) {
      setError({
        kind: 'server',
        message: err?.message || 'Chưa xác nhận được đăng xuất. Vui lòng thử lại.',
        fields: [],
      });
    } finally {
      setIsLoggingOut(false);
    }
  }

  const hasActiveFilter = Object.values(appliedFilters).some((value) => value !== '');

  return (
    <main>
      {/* Thanh điều hướng tạm (sẽ tách sang AdminLayout khi nhóm chốt giao diện) */}
      <header>
        <nav aria-label="Điều hướng quản trị tạm thời">
          <Link to="/admin/users">Trang chủ Admin (placeholder)</Link>
          {' | '}
          <strong>Nhật ký kiểm toán (A-07)</strong>
          {' | '}
          <span>
            {user?.fullName || 'Quản trị viên'} ({user?.role})
          </span>
          {' '}
          <button type="button" onClick={handleLogout} disabled={isLoggingOut}>
            {isLoggingOut ? 'Đang đăng xuất...' : 'Đăng xuất'}
          </button>
        </nav>
        <hr />
      </header>

      <h1>A-07 · Nhật ký kiểm toán (Audit log)</h1>

      {/* Bộ lọc 4 trường theo đặc tả A-07 */}
      <section aria-label="Bộ lọc nhật ký">
        <h2>Bộ lọc</h2>
        <form onSubmit={handleApplyFilters} noValidate>
          <div>
            <label htmlFor="audit-filter-studentId">ID học sinh (studentId): </label>
            <input
              id="audit-filter-studentId"
              name="studentId"
              type="text"
              inputMode="numeric"
              value={draftFilters.studentId}
              onChange={handleFilterChange}
              placeholder="Ví dụ: 1"
            />
          </div>

          <div>
            <label htmlFor="audit-filter-action">Hành động (action): </label>
            <select
              id="audit-filter-action"
              name="action"
              value={draftFilters.action}
              onChange={handleFilterChange}
            >
              <option value="">-- Tất cả --</option>
              {AUDIT_ACTION_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label htmlFor="audit-filter-entityType">Bảng đối tượng (entityType): </label>
            <select
              id="audit-filter-entityType"
              name="entityType"
              value={draftFilters.entityType}
              onChange={handleFilterChange}
            >
              <option value="">-- Tất cả --</option>
              {AUDIT_ENTITY_TYPE_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label htmlFor="audit-filter-entityId">ID đối tượng (entityId): </label>
            <input
              id="audit-filter-entityId"
              name="entityId"
              type="text"
              value={draftFilters.entityId}
              onChange={handleFilterChange}
              placeholder="Nhập TEXT, ví dụ: 12 hoặc 2:5"
            />
          </div>

          <div>
            <button type="submit" disabled={isLoading}>
              Áp dụng lọc
            </button>{' '}
            <button type="button" onClick={handleResetFilters} disabled={isLoading}>
              Xóa lọc
            </button>
          </div>
        </form>

        {/* Lỗi validation (400 hoặc kiểm tra bộ lọc phía client) */}
        {error?.kind === 'validation' && (
          <div role="alert">
            <p>
              <strong>Lỗi bộ lọc:</strong> {error.message}
            </p>
            {error.fields.length > 0 && (
              <ul>
                {error.fields.map((fieldError, index) => (
                  <li key={`${fieldError.field}-${index}`}>
                    <code>{fieldError.field}</code>: {fieldError.message}
                  </li>
                ))}
              </ul>
            )}
          </div>
        )}
      </section>

      <hr />

      {/* Vùng kết quả bảng nhật ký */}
      <section aria-label="Danh sách nhật ký">
        <h2>
          Danh sách nhật ký {!isLoading && !error ? `(${items.length})` : ''}
        </h2>

        {hasActiveFilter && (
          <p>
            Đang áp dụng lọc:{' '}
            <code>
              {Object.entries(appliedFilters)
                .filter(([, value]) => value !== '')
                .map(([key, value]) => `${key}=${value}`)
                .join(', ')}
            </code>
          </p>
        )}

        {isLoading && <p role="status">Đang tải nhật ký...</p>}

        {!isLoading && error && error.kind !== 'validation' && (
          <div role="alert">
            <p>{error.message}</p>
            {error.kind === 'server' && (
              <button type="button" onClick={handleRetry}>
                Thử lại
              </button>
            )}
          </div>
        )}

        {!isLoading && (!error || error.kind === 'validation') && items.length === 0 && (
          <p>Không có nhật ký phù hợp</p>
        )}

        {!isLoading && items.length > 0 && (
          <table border="1" cellPadding="6" cellSpacing="0">
            <thead>
              <tr>
                <th>ID</th>
                <th>Actor ID</th>
                <th>Hành động</th>
                <th>Đối tượng (entityType)</th>
                <th>ID đối tượng (entityId)</th>
                <th>Thời điểm (giờ VN)</th>
                <th>Chi tiết</th>
              </tr>
            </thead>
            <tbody>
              {items.map((item) => {
                const isSelected = selectedLog?.id === item.id;
                return (
                  <tr key={item.id}>
                    <td>{item.id}</td>
                    <td>{item.actorId}</td>
                    <td>{item.action}</td>
                    <td>{item.entityType}</td>
                    <td>{item.entityId}</td>
                    <td title={item.createdAt || ''}>
                      {formatUtcToVietnamDateTime(item.createdAt)}
                    </td>
                    <td>
                      <button
                        type="button"
                        onClick={() => setSelectedLog(isSelected ? null : item)}
                      >
                        {isSelected ? 'Đóng chi tiết' : 'Xem chi tiết'}
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </section>

      {/* Khung chi tiết chỉ đọc lấy hoàn toàn từ dòng đã tải */}
      {selectedLog && (
        <>
          <hr />
          <section aria-label="Chi tiết bản ghi nhật ký">
            <h2>Chi tiết nhật ký #{selectedLog.id}</h2>
            <p>
              <button type="button" onClick={() => setSelectedLog(null)}>
                Đóng chi tiết
              </button>
            </p>

            <ul>
              <li>
                <strong>ID:</strong> {selectedLog.id}
              </li>
              <li>
                <strong>Actor ID:</strong> {selectedLog.actorId}
              </li>
              {selectedLog.studentId !== undefined && (
                <li>
                  <strong>Student ID:</strong> {selectedLog.studentId ?? '—'}
                </li>
              )}
              <li>
                <strong>Action:</strong> {selectedLog.action}
              </li>
              <li>
                <strong>Entity Type:</strong> {selectedLog.entityType}
              </li>
              <li>
                <strong>Entity ID:</strong> {selectedLog.entityId}
              </li>
              <li>
                <strong>Thời điểm (giờ VN):</strong>{' '}
                {formatUtcToVietnamDateTime(selectedLog.createdAt)}{' '}
                {selectedLog.createdAt ? `(UTC: ${selectedLog.createdAt})` : ''}
              </li>
            </ul>

            <div>
              <h3>Giá trị cũ (oldValue)</h3>
              <pre>{formatAuditPayload(selectedLog.oldValue)}</pre>
            </div>

            <div>
              <h3>Giá trị mới (newValue)</h3>
              <pre>{formatAuditPayload(selectedLog.newValue)}</pre>
            </div>
          </section>
        </>
      )}
    </main>
  );
}