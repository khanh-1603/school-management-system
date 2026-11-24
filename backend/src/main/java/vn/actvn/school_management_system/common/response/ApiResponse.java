package vn.actvn.school_management_system.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import vn.actvn.school_management_system.common.exception.ErrorCode;


/**
 * "Vỏ ngoài" chung của <b>mọi</b> response từ API.
 *
 * <p>Thành công: {@code {"success":true,"data":{...}}}<br>
 * Thất bại: {@code {"success":false,"error":{"code":"...","message":"..."}}}
 *
 * <p><b>Cách dùng:</b>
 * <pre>{@code
 * // Trong Controller: trả dữ liệu thành công
 * return ApiResponse.ok(new ItemsDto(list));
 *
 * // Lỗi thường không tạo thủ công: ném AppException,
 * // GlobalExceptionHandler sẽ gọi ApiResponse.error(...)
 * }</pre>
 *
 * <p><b>Ghi chú:</b>
 * <ul>
 *   <li>{@code @JsonInclude(NON_NULL)}: response thành công không có trường
 *       {@code error}, response lỗi không có trường {@code data}.</li>
 *   <li>Danh sách rỗng phải trả {@code {"items":[]}}, không phải {@code null};
 *       việc đó do DTO của từng chức năng bảo đảm, không do lớp này.</li>
 *   <li>Response lỗi dùng kiểu {@code ApiResponse<Void>} vì không có {@code data}.</li>
 * </ul>
 *
 * @param <T>     kiểu của phần dữ liệu khi thành công
 * @param success {@code true} nếu thành công, {@code false} nếu lỗi
 * @param data    dữ liệu trả về khi thành công; {@code null} khi lỗi
 * @param error   thông tin lỗi khi thất bại; {@code null} khi thành công
 * @see ApiError
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, T data, ApiError error) {

    /**
     * Tạo response thành công.
     *
     * @param data dữ liệu trả cho client, thường là một DTO
     * @param <T>  kiểu của dữ liệu
     * @return envelope {@code {success:true, data}}
     */
    public static <T> ApiResponse<T> ok(T data) { return new ApiResponse<>(true, data, null); }
    
    /**
     * Tạo response lỗi từ một {@link ErrorCode}.
     *
     * <p>Chỉ lấy <i>tên</i> mã lỗi để đưa vào JSON; HTTP status do nơi gọi đặt
     * riêng (xem {@code GlobalExceptionHandler} và {@code ApiErrorWriter}).
     *
     * @param code    mã lỗi, tên của nó trở thành {@code error.code}
     * @param message nội dung hiển thị cho người dùng
     * @param fields  lỗi theo từng trường, hoặc {@code null} nếu không có
     * @return envelope {@code {success:false, error}}
     */
    public static ApiResponse<Void> error(ErrorCode code, String message, List<FieldIssue> fields) {
        return new ApiResponse<>(false, null, new ApiError(code.name(), message, fields));
    }
}