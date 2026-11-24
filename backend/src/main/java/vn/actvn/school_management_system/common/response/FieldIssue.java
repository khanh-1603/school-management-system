package vn.actvn.school_management_system.common.response;

/**
 * Một lỗi gắn với <b>một trường cụ thể</b> của form, ví dụ
 * {@code {"field":"reason","message":"must not be blank"}}.
 *
 * <p>Chỉ xuất hiện trong {@link ApiError#fields()} khi Bean Validation báo lỗi
 * body, để frontend tô đỏ đúng ô nhập thay vì chỉ hiện một thông báo chung.
 *
 * <p><b>Ghi chú:</b>
 * <ul>
 *   <li>Là danh sách tùy chọn; không cần chỉ rõ trường thì để {@code null}.</li>
 *   <li>Frontend vẫn phải xử lý theo {@code error.code}, không phụ thuộc vào
 *       nội dung {@code message} của từng trường.</li>
 * </ul>
 *
 * @param field   tên trường trong body JSON, ví dụ {@code "reason"}
 * @param message mô tả lỗi của trường đó, hiển thị cạnh ô nhập
 * @see ApiError
 */
public record FieldIssue(String field, String message) {}