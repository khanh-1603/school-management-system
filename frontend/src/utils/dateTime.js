/**
 * Múi giờ hiển thị chuẩn của ứng dụng.
 *
 * @type {string}
 */
export const VIETNAM_TIME_ZONE = 'Asia/Ho_Chi_Minh';

/**
 * Bộ định dạng timestamp ISO 8601 UTC sang múi giờ Việt Nam (`DD/MM/YYYY HH:mm:ss`).
 * Dùng `formatToParts` để kết quả đồng nhất giữa các trình duyệt.
 *
 * @type {Intl.DateTimeFormat}
 */
const VIETNAM_DATE_TIME_FORMATTER = new Intl.DateTimeFormat('vi-VN', {
  timeZone: VIETNAM_TIME_ZONE,
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  second: '2-digit',
  hour12: false,
});

/**
 * Chuyển chuỗi timestamp ISO 8601 UTC (ví dụ `2026-10-31T09:00:00Z`) sang chuỗi
 * giờ Việt Nam dạng `DD/MM/YYYY HH:mm:ss`.
 *
 * Lưu ý: Chỉ dùng cho timestamp UTC (`createdAt`, `reviewedAt`), không dùng cho
 * ngày nghiệp vụ dạng `YYYY-MM-DD`.
 *
 * @param {string | null | undefined} isoTimestamp Chuỗi thời gian ISO 8601 UTC từ API.
 * @returns {string} Chuỗi giờ Việt Nam hoặc `—` nếu dữ liệu rỗng.
 */
export function formatUtcToVietnamDateTime(isoTimestamp) {
  if (!isoTimestamp) {
    return '—';
  }

  const parsedDate = new Date(isoTimestamp);
  if (Number.isNaN(parsedDate.getTime())) {
    return String(isoTimestamp);
  }

  const parts = VIETNAM_DATE_TIME_FORMATTER.formatToParts(parsedDate);
  const partMap = Object.fromEntries(parts.map((part) => [part.type, part.value]));

  return `${partMap.day}/${partMap.month}/${partMap.year} ${partMap.hour}:${partMap.minute}:${partMap.second}`;
}