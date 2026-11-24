// common/exception/AppException.java
package vn.actvn.school_management_system.common.exception;

import java.util.List;
import vn.actvn.school_management_system.common.response.FieldIssue;

/**
 * Exception nghiệp vụ <b>duy nhất</b> mà Service được phép ném để trả lỗi cho
 * client.
 *
 * <p>Mang theo {@link ErrorCode}, message và danh sách lỗi theo trường (tùy
 * chọn). {@link GlobalExceptionHandler} bắt nó và đổi thành envelope JSON
 * {@code {success:false, error:{...}}}.
 *
 * <p><b>Cách dùng:</b>
 * <pre>{@code
 * var leave = repo.findOwned(id, parentId)
 *         .orElseThrow(AppException::notFound);
 * if (!leave.isPending()) {
 *     throw AppException.conflict("Đơn đã được xử lý, không thể thay đổi.");
 * }
 * }</pre>
 *
 * <p><b>Ghi chú:</b>
 * <ul>
 *   <li>Là {@link RuntimeException} nên không cần khai báo {@code throws}.</li>
 *   <li>Mọi chỗ ném 404 phải đi qua {@link #notFound()} để "không tồn tại" và
 *       "không có quyền" ra phản hồi y hệt nhau.</li>
 *   <li>Lỗi xảy ra trong filter không đi qua lớp này; dùng
 *       {@code ApiErrorWriter}.</li>
 * </ul>
 *
 * @see ErrorCode
 * @see GlobalExceptionHandler
 */
public class AppException extends RuntimeException {

    /** Mã lỗi, quyết định HTTP status và giá trị {@code error.code}. */
    private final ErrorCode code;

    /** Lỗi theo từng trường của form; {@code null} nếu không cần chỉ rõ trường. */
    private final List<FieldIssue> fields;


    /**
     * Tạo exception đầy đủ: mã lỗi, message và danh sách lỗi theo trường.
     *
     * @param code    mã lỗi, không null
     * @param message nội dung gửi cho client
     * @param fields  lỗi theo trường, hoặc {@code null} nếu không có
     */
    public AppException(ErrorCode code, String message, List<FieldIssue> fields) {
        super(message);
        this.code = code;
        this.fields = fields;
    }

    /**
     * Tạo exception với message riêng, không có lỗi theo trường.
     *
     * @param code    mã lỗi, không null
     * @param message nội dung gửi cho client
     */
    public AppException(ErrorCode code, String message) { this(code, message, null); }
    
    /**
     * Tạo exception với message mặc định của mã lỗi.
     *
     * @param code mã lỗi, không null; message lấy từ {@link ErrorCode#defaultMessage()}
     */
    public AppException(ErrorCode code) { this(code, code.defaultMessage(), null); }

    /**
     * Tạo lỗi 404 chuẩn. Đây là <b>nơi duy nhất</b> nên dùng để ném 404.
     *
     * <p>Dùng cả khi ID không tồn tại lẫn khi ID có thật nhưng người gọi không
     * có quan hệ, nhờ đó hai trường hợp ra phản hồi giống hệt nhau.
     *
     * @return exception mang {@link ErrorCode#NOT_FOUND} và message mặc định
     */
    public static AppException notFound() { return new AppException(ErrorCode.NOT_FOUND); }
    
    /**
     * Tạo lỗi 400 với message cụ thể về quy tắc bị vi phạm.
     *
     * <p>Ví dụ: "Ngày bắt đầu không được ở quá khứ.". Message nên ngắn và nêu
     * đúng quy tắc, không lộ chi tiết nội bộ.
     *
     * @param msg nội dung gửi cho client
     * @return exception mang {@link ErrorCode#VALIDATION_ERROR}
     */
    public static AppException validation(String msg) { return new AppException(ErrorCode.VALIDATION_ERROR, msg); }
    
    /**
     * Tạo lỗi 409 với message cụ thể về xung đột.
     *
     * <p>Ví dụ: "Email đã tồn tại.", "Đơn đã được xử lý, không thể thay đổi.".
     *
     * @param msg nội dung gửi cho client
     * @return exception mang {@link ErrorCode#CONFLICT}
     */
    public static AppException conflict(String msg) { return new AppException(ErrorCode.CONFLICT, msg); }

     /**
     * Lấy mã lỗi của exception.
     *
     * @return mã lỗi, không null
     */
    public ErrorCode code() { return code; }
    
    /**
     * Lấy danh sách lỗi theo trường.
     *
     * @return danh sách lỗi, hoặc {@code null} nếu không có (khi đó trường
     *  {@code fields} không xuất hiện trong JSON)
     */
    public List<FieldIssue> fields() { return fields; }
}