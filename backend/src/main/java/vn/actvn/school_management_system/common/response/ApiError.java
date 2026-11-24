package vn.actvn.school_management_system.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Phần {@code error} trong envelope lỗi:
 * {@code {"code":"VALIDATION_ERROR","message":"...","fields":[...]}}.
 *
 * <p>Được {@link ApiResponse#error(vn.actvn.school_management_system.common.exception.ErrorCode, String, List)}
 * tạo ra; code bên ngoài không cần dựng trực tiếp.
 *
 * <p><b>Ghi chú:</b>
 * <ul>
 *   <li>{@code @JsonInclude(NON_NULL)}: khi {@code fields} là {@code null} thì
 *       trường này <i>biến mất</i> khỏi JSON, không phải {@code "fields":null}.</li>
 *   <li>Frontend xử lý theo {@code code}, không so sánh chuỗi {@code message}.</li>
 * </ul>
 *
 * @param code    tên mã lỗi, lấy từ {@code ErrorCode.name()}, ví dụ {@code "NOT_FOUND"}
 * @param message nội dung hiển thị cho người dùng
 * @param fields  lỗi theo từng trường, hoặc {@code null} nếu không có
 * @see ApiResponse
 * @see FieldIssue
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(String code, String message, List<FieldIssue> fields) {}