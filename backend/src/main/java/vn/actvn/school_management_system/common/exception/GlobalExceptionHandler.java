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

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static ResponseEntity<ApiResponse<Void>> build(ErrorCode c, String msg, List<FieldIssue> f) {
        return ResponseEntity.status(c.status()).body(ApiResponse.error(c, msg, f));
    }
    private static ResponseEntity<ApiResponse<Void>> build(ErrorCode c, String msg) { return build(c, msg, null); }

    @ExceptionHandler(AppException.class)
    ResponseEntity<ApiResponse<Void>> app(AppException e) { return build(e.code(), e.getMessage(), e.fields()); }

    // @PreAuthorize ném trong controller nên vào đây, không vào AccessDeniedHandler.
    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiResponse<Void>> unauth(AuthenticationException e) {
        return build(ErrorCode.UNAUTHENTICATED, ErrorCode.UNAUTHENTICATED.defaultMessage());
    }
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiResponse<Void>> denied(AccessDeniedException e) {
        return build(ErrorCode.FORBIDDEN, ErrorCode.FORBIDDEN.defaultMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Void>> invalidBody(MethodArgumentNotValidException e) {
        List<FieldIssue> fields = e.getBindingResult().getFieldErrors().stream()
                .map(f -> new FieldIssue(f.getField(), f.getDefaultMessage())).toList();
        return build(ErrorCode.VALIDATION_ERROR, "Dữ liệu không hợp lệ.", fields);
    }
    // gồm JSON sai cú pháp, body rỗng, sai kiểu và field ngoài allowlist
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiResponse<Void>> unreadable(HttpMessageNotReadableException e) {
        return build(ErrorCode.VALIDATION_ERROR, "Dữ liệu không hợp lệ.");
    }
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, ConstraintViolationException.class,
            MissingServletRequestParameterException.class})
    ResponseEntity<ApiResponse<Void>> badParam(Exception e) {
        return build(ErrorCode.VALIDATION_ERROR, "Tham số không hợp lệ.");
    }
    @ExceptionHandler({HttpRequestMethodNotSupportedException.class, HttpMediaTypeNotSupportedException.class,
            HttpMediaTypeNotAcceptableException.class})
    ResponseEntity<ApiResponse<Void>> unsupported(Exception e) {
        return build(ErrorCode.VALIDATION_ERROR, "Yêu cầu không được hỗ trợ.");
    }
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    ResponseEntity<ApiResponse<Void>> noRoute(Exception e) {
        return build(ErrorCode.NOT_FOUND, ErrorCode.NOT_FOUND.defaultMessage());
    }

    // Fallback; service nên bắt trước để trả message theo trường ("Email đã tồn tại.").
    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<ApiResponse<Void>> data(DataAccessException e) {
        if (SqlErrors.isUnique(e)) return build(ErrorCode.CONFLICT, "Dữ liệu đã tồn tại.");
        if (SqlErrors.isForeignKey(e)) return build(ErrorCode.CONFLICT, "Không thể xóa vì đang được sử dụng.");
        log.error("Lỗi truy cập dữ liệu", e);
        return build(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.defaultMessage());
    }
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> other(Exception e) {
        log.error("Lỗi không lường trước", e);
        return build(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.defaultMessage());
    }
}