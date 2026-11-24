// common/response/ApiErrorWriter.java
package vn.actvn.school_management_system.common.response;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import vn.actvn.school_management_system.common.exception.ErrorCode;

/**
 * Ghi envelope lỗi <b>thẳng vào {@link HttpServletResponse}</b> cho code chạy
 * trước Spring MVC, nơi {@code GlobalExceptionHandler} không với tới.
 *
 * <p>Dùng ở: {@code OriginFilter}, {@code AuthenticationEntryPoint},
 * {@code AccessDeniedHandler}. Nhờ dùng chung {@link ApiResponse} và
 * {@link ErrorCode}, lỗi từ filter và lỗi từ controller có <i>cùng định dạng
 * JSON</i>.
 *
 * <p><b>Cách dùng:</b>
 * <pre>{@code
 * if (originSai) {
 *     writer.write(res, ErrorCode.FORBIDDEN, "Yêu cầu không hợp lệ.");
 *     return; // KHÔNG gọi chain.doFilter
 * }
 * }</pre>
 *
 * <p><b>Ghi chú:</b>
 * <ul>
 *   <li>Dùng chung {@link ObjectMapper} của Spring, nên cấu hình Jackson
 *       (bao gồm {@code NON_NULL}) áp dụng giống nhau mọi nơi.</li>
 *   <li>Sau khi ghi, response coi như đã hoàn tất; nơi gọi phải {@code return}
 *       và không để request đi tiếp.</li>
 *   <li>Không có logic phân quyền hay ghi log ở đây; chỉ định dạng và ghi.</li>
 * </ul>
 *
 * @see ApiResponse
 * @see ErrorCode
 */@Component
public class ApiErrorWriter {

    /** Bộ chuyển object sang JSON, dùng chung với toàn ứng dụng. */
    private final ObjectMapper mapper;

    /**
     * Tạo writer. Spring tự truyền {@link ObjectMapper} qua constructor.
     *
     * @param mapper bộ chuyển JSON của ứng dụng
     */
    public ApiErrorWriter(ObjectMapper mapper) { this.mapper = mapper; }

    /**
     * Ghi lỗi với message mặc định của mã lỗi.
     *
     * @param res  response cần ghi, chưa được commit
     * @param code mã lỗi, quyết định HTTP status; message lấy từ
     *             {@link ErrorCode#defaultMessage()}
     * @throws IOException nếu ghi body thất bại
     */
    public void write(HttpServletResponse res, ErrorCode code) throws IOException {
        write(res, code, code.defaultMessage());
    }

    /**
     * Ghi lỗi với message riêng.
     *
     * <p>Đặt HTTP status theo {@link ErrorCode#status()}, content-type
     * {@code application/json} UTF-8, rồi ghi body
     * {@code {success:false, error:{code, message}}}. Không có
     * {@code fields} (filter không có thông tin theo trường).
     *
     * @param res     response cần ghi, chưa được commit
     * @param code    mã lỗi, quyết định HTTP status và {@code error.code}
     * @param message nội dung hiển thị cho người dùng
     * @throws IOException nếu ghi body thất bại
     */
    public void write(HttpServletResponse res, ErrorCode code, String message) throws IOException {
        res.setStatus(code.status().value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        mapper.writeValue(res.getOutputStream(), ApiResponse.error(code, message, null));
    }
}