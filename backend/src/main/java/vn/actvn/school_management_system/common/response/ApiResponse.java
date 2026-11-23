package vn.actvn.school_management_system.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import vn.actvn.school_management_system.common.exception.ErrorCode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, T data, ApiError error) {
    public static <T> ApiResponse<T> ok(T data) { return new ApiResponse<>(true, data, null); }
    public static ApiResponse<Void> error(ErrorCode code, String message, List<FieldIssue> fields) {
        return new ApiResponse<>(false, null, new ApiError(code.name(), message, fields));
    }
}