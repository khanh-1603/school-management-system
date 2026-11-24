package vn.actvn.school_management_system.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Danh sách <b>6 mã lỗi duy nhất</b> của API, mỗi mã gắn sẵn HTTP status và
 * message mặc định.
 *
 * <p>Mọi lỗi trả về client đều quy về một trong các mã này. Frontend xử lý theo
 * tên mã ({@link #name()}), <i>không</i> so sánh chuỗi message.
 *
 * <p><b>Ghi chú:</b>
 * <ul>
 *   <li>Không thêm mã mới: thiết kế cố định đúng 6 mã (405/415 được gộp vào
 *       {@link #VALIDATION_ERROR}).</li>
 *   <li>Tên hằng chính là giá trị {@code error.code} trong JSON.</li>
 * </ul>
 *
 * @see AppException
 * @see GlobalExceptionHandler
 */

public enum ErrorCode {
    /**
     * 401 - chưa đăng nhập, hết phiên, hoặc đăng nhập thất bại.
     *
     * <p>Mọi nguyên nhân đăng nhập thất bại (sai mật khẩu, email không tồn tại,
     * tài khoản bị vô hiệu hóa hoặc đang bị khóa) phải dùng <b>cùng một
     * message</b> để không lộ thông tin tài khoản.
     */
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập."),
    
    /**
     * 403 - đã đăng nhập nhưng <b>sai vai trò</b>, hoặc header {@code Origin}
     * không hợp lệ.
     *
     * <p>Không dùng cho trường hợp thiếu quan hệ sở hữu, liên kết hay phân
     * công; các trường hợp đó trả {@link #NOT_FOUND}.
     */
    FORBIDDEN(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập tài nguyên này."),
    
    /**
     * 404 - tài nguyên không tồn tại <b>hoặc</b> người gọi không có quan hệ với nó.
     *
     * <p>Hai trường hợp này phải cho ra phản hồi <i>y hệt nhau</i> để người ngoài
     * không dò được ID nào có thật. Luôn tạo qua {@link AppException#notFound()}.
     */
    NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy tài nguyên."),
    
    /**
     * 400 - dữ liệu gửi lên sai: body, path, query, quy tắc nghiệp vụ trên dữ
     * liệu gửi lên, ID tham chiếu trong body sai.
     *
     * <p>Cũng được dùng cho lỗi method/Content-Type không hỗ trợ (405/415) để
     * giữ đúng 6 mã.
     */
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ."),
    
    /**
     * 409 - dữ liệu đúng nhưng <b>trạng thái hiện tại</b> không cho phép: đơn đã
     * xử lý, dữ liệu trùng, bản ghi đang được tham chiếu.
     *
     * <p>Thường kèm message cụ thể do Service đặt, ví dụ "Email đã tồn tại.".
     */
    CONFLICT(HttpStatus.CONFLICT, "Dữ liệu xung đột với trạng thái hiện tại."),
    
    /**
     * 500 - lỗi không lường trước (NPE, lỗi SQL, {@code database is locked}).
     *
     * <p>Chi tiết chỉ ghi log ở server; response không được chứa stack trace
     * hay câu SQL.
     */
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Đã xảy ra lỗi hệ thống. Vui lòng thử lại.");

    /** HTTP status tương ứng với mã lỗi. */
    private final HttpStatus status;

    /** Message mặc định hiển thị khi nơi ném lỗi không chỉ định message riêng. */
    private final String defaultMessage;

    /**
     * Tạo một mã lỗi. Chỉ được Java gọi khi khởi tạo các hằng ở trên.
     *
     * @param status         HTTP status của mã lỗi
     * @param defaultMessage message mặc định gửi cho client
     */
    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    /**
     * Lấy HTTP status của mã lỗi.
     *
     * @return status, ví dụ {@link HttpStatus#NOT_FOUND} cho {@link #NOT_FOUND}
     */
    public HttpStatus status() { return status; }
    
    /**
     * Lấy message mặc định của mã lỗi.
     *
     * @return message tiếng Việt, dùng khi không có message riêng
     */
    public String defaultMessage() { return defaultMessage; }
}