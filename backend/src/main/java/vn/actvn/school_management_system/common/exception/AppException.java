// common/exception/AppException.java
package vn.actvn.school_management_system.common.exception;

import java.util.List;
import vn.actvn.school_management_system.common.response.FieldIssue;

public class AppException extends RuntimeException {
    private final ErrorCode code;
    private final List<FieldIssue> fields;

    public AppException(ErrorCode code, String message, List<FieldIssue> fields) {
        super(message);
        this.code = code;
        this.fields = fields;
    }
    public AppException(ErrorCode code, String message) { this(code, message, null); }
    public AppException(ErrorCode code) { this(code, code.defaultMessage(), null); }

    // Một chỗ duy nhất tạo 404: "không tồn tại" và "không có quan hệ" phải y hệt nhau.
    public static AppException notFound() { return new AppException(ErrorCode.NOT_FOUND); }
    public static AppException validation(String msg) { return new AppException(ErrorCode.VALIDATION_ERROR, msg); }
    public static AppException conflict(String msg) { return new AppException(ErrorCode.CONFLICT, msg); }

    public ErrorCode code() { return code; }
    public List<FieldIssue> fields() { return fields; }
}