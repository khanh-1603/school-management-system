// common/exception/GlobalExceptionHandler.java
package vn.actvn.school_management_system.common.exception;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import vn.actvn.school_management_system.common.response.ApiResponse;
import vn.actvn.school_management_system.common.response.FieldIssue;
import vn.actvn.school_management_system.common.util.SqlErrors;

/**
 * Chốt chặn cuối cùng: biến mọi exception thoát ra từ Controller/Service thành
 * envelope lỗi {@code {success:false, error:{code, message[, fields]}}}.
 *
 * <p>Spring tự chọn handler <i>cụ thể nhất</i> khớp với loại exception; không
 * gọi trực tiếp lớp này.
 *
 * <p><b>Ghi chú:</b>
 * <ul>
 *   <li>Chỉ bắt được lỗi phát sinh từ Controller trở vào. Lỗi trong filter
 *       (như {@code OriginFilter}) chạy trước MVC nên phải ghi qua
 *       {@code ApiErrorWriter}.</li>
 *   <li>{@link AccessDeniedException} từ {@code @PreAuthorize} được ném trong
 *       controller nên rơi vào đây, không vào {@code AccessDeniedHandler}.</li>
 *   <li>Message của lỗi 400 là hằng cố định, <b>không</b> chép nội dung exception
 *       để tránh lộ chi tiết nội bộ.</li>
 *   <li>Lỗi 500 được log đầy đủ ở server, response không chứa stack hay SQL.</li>
 * </ul>
 *
 * @see AppException
 * @see ErrorCode
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Logger ghi lại các lỗi 500 để dev xem nguyên nhân thật. */
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Dựng response lỗi đầy đủ.
     *
     * @param c   mã lỗi, quyết định HTTP status
     * @param msg message gửi cho client
     * @param f   lỗi theo trường, hoặc {@code null}
     * @return {@link ResponseEntity} chứa {@link ApiResponse} lỗi
     */
    private static ResponseEntity<ApiResponse<Void>> build(ErrorCode c, String msg, List<FieldIssue> f) {
        return ResponseEntity.status(c.status()).body(ApiResponse.error(c, msg, f));
    }

    /**
     * Dựng response lỗi không có lỗi theo trường.
     *
     * @param c   mã lỗi, quyết định HTTP status
     * @param msg message gửi cho client
     * @return {@link ResponseEntity} chứa {@link ApiResponse} lỗi
     */
    private static ResponseEntity<ApiResponse<Void>> build(ErrorCode c, String msg) { return build(c, msg, null); }

    /**
     * Xử lý lỗi nghiệp vụ do Service chủ động ném.
     *
     * @param e exception mang sẵn mã lỗi, message và lỗi theo trường
     * @return response với status, message và fields lấy nguyên từ {@code e}
     */
    @ExceptionHandler(AppException.class)
    ResponseEntity<ApiResponse<Void>> app(AppException e) { return build(e.code(), e.getMessage(), e.fields()); }

    /**
     * Xử lý lỗi xác thực ném từ bên trong controller.
     *
     * @param e lỗi xác thực của Spring Security (không dùng nội dung)
     * @return 401 {@link ErrorCode#UNAUTHENTICATED} với message mặc định
     */
    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiResponse<Void>> unauth(AuthenticationException e) {
        return build(ErrorCode.UNAUTHENTICATED, ErrorCode.UNAUTHENTICATED.defaultMessage());
    }

    /**
     * Xử lý lỗi thiếu quyền, chủ yếu từ {@code @PreAuthorize}.
     *
     * <p>Nếu thiếu handler này, lỗi sẽ rơi xuống handler cuối và trả nhầm 500.
     *
     * @param e lỗi từ chối truy cập (không dùng nội dung)
     * @return 403 {@link ErrorCode#FORBIDDEN} với message mặc định
     */
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiResponse<Void>> denied(AccessDeniedException e) {
        return build(ErrorCode.FORBIDDEN, ErrorCode.FORBIDDEN.defaultMessage());
    }

    /**
     * Xử lý body sai Bean Validation (thiếu field, sai độ dài, định dạng, khoảng
     * giá trị).
     *
     * @param e exception chứa danh sách lỗi theo từng trường
     * @return 400 {@link ErrorCode#VALIDATION_ERROR} kèm {@code fields} gồm
     *         tên trường và message để form tô đúng ô
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Void>> invalidBody(MethodArgumentNotValidException e) {
        List<FieldIssue> fields = e.getBindingResult().getFieldErrors().stream()
                .map(f -> new FieldIssue(f.getField(), f.getDefaultMessage())).toList();
        return build(ErrorCode.VALIDATION_ERROR, "Dữ liệu không hợp lệ.", fields);
    }

    /**
     * Xử lý body không đọc được: JSON sai cú pháp, body rỗng, sai kiểu dữ liệu,
     * hoặc có field ngoài allowlist.
     *
     * <p>Trường hợp field lạ chỉ xảy ra khi bật
     * {@code spring.jackson.deserialization.fail-on-unknown-properties=true}.
     *
     * @param e exception đọc body (không dùng nội dung để tránh lộ chi tiết)
     * @return 400 {@link ErrorCode#VALIDATION_ERROR} với message cố định
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiResponse<Void>> unreadable(HttpMessageNotReadableException e) {
        return build(ErrorCode.VALIDATION_ERROR, "Dữ liệu không hợp lệ.");
    }

     /**
     * Xử lý tham số path/query sai: sai kiểu ({@code /students/abc}), vi phạm
     * ràng buộc, hoặc thiếu tham số bắt buộc.
     *
     * @param e một trong ba loại exception tham số
     * @return 400 {@link ErrorCode#VALIDATION_ERROR} với message "Tham số không hợp lệ."
     */
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, ConstraintViolationException.class,
            MissingServletRequestParameterException.class})
    ResponseEntity<ApiResponse<Void>> badParam(Exception e) {
        return build(ErrorCode.VALIDATION_ERROR, "Tham số không hợp lệ.");
    }

    /**
     * Xử lý method hoặc Content-Type không được hỗ trợ (vốn là 405/415).
     *
     * <p>Gộp về 400 để API chỉ có đúng 6 mã lỗi.
     *
     * @param e một trong ba loại exception về method/media type
     * @return 400 {@link ErrorCode#VALIDATION_ERROR} với message "Yêu cầu không được hỗ trợ."
     */
    @ExceptionHandler({HttpRequestMethodNotSupportedException.class, HttpMediaTypeNotSupportedException.class,
            HttpMediaTypeNotSupportedException.class,
            HttpMediaTypeNotAcceptableException.class})
    ResponseEntity<ApiResponse<Void>> unsupported(Exception e) {
        return build(ErrorCode.VALIDATION_ERROR, "Yêu cầu không được hỗ trợ.");
    }

    /**
     * Xử lý đường dẫn API không tồn tại.
     *
     * @param e exception không tìm thấy handler hoặc resource
     * @return 404 {@link ErrorCode#NOT_FOUND} với message mặc định, giống hệt
     *         lỗi ID không tồn tại
     */
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    ResponseEntity<ApiResponse<Void>> noRoute(Exception e) {
        return build(ErrorCode.NOT_FOUND, ErrorCode.NOT_FOUND.defaultMessage());
    }

    /**
     * Lưới an toàn cho lỗi truy cập dữ liệu mà Service chưa bắt.
     *
     * <p>Phân loại bằng mã lỗi SQLite qua {@link SqlErrors}, không đọc chuỗi
     * thông báo. Service nên bắt trước để trả message theo trường, ví dụ
     * "Email đã tồn tại.".
     *
     * @param e lỗi truy cập dữ liệu của Spring (SQLite có thể nằm trong chuỗi cause)
     * @return 409 {@link ErrorCode#CONFLICT} nếu vi phạm UNIQUE/PRIMARY KEY
     *         hoặc FOREIGN KEY; ngược lại 500 {@link ErrorCode#INTERNAL_ERROR}
     *         (và ghi log)
     */    
    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<ApiResponse<Void>> data(DataAccessException e) {
        if (SqlErrors.isUnique(e)) return build(ErrorCode.CONFLICT, "Dữ liệu đã tồn tại.");
        if (SqlErrors.isForeignKey(e)) return build(ErrorCode.CONFLICT, "Không thể xóa vì đang được sử dụng.");
        log.error("Lỗi truy cập dữ liệu", e);
        return build(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.defaultMessage());
    }

    /**
     * Handler cuối cùng cho mọi lỗi không lường trước (NPE, lỗi logic, lỗi hệ thống).
     *
     * <p>Ghi log đầy đủ stack ở server, nhưng response chỉ có message chung để
     * không lộ chi tiết nội bộ.
     *
     * @param e exception bất kỳ chưa có handler riêng
     * @return 500 {@link ErrorCode#INTERNAL_ERROR} với message mặc định
     */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> other(Exception e) {
        log.error("Lỗi không lường trước", e);
        return build(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.defaultMessage());
    }
}