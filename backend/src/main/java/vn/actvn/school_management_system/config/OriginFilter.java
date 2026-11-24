package vn.actvn.school_management_system.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.actvn.school_management_system.common.exception.ErrorCode;
import vn.actvn.school_management_system.common.response.ApiErrorWriter;

/**
 * Bộ lọc chống CSRF thay cho CSRF token: request <b>ghi dữ liệu</b> phải có
 * header {@code Origin} nằm trong danh sách cho phép.
 *
 * <p>Trình duyệt luôn tự gắn {@code Origin} khi trang web gọi POST/PUT/PATCH/
 * DELETE. Trang của kẻ tấn công không thể giả mạo giá trị đó, nên request đến từ
 * trang lạ sẽ bị chặn dù cookie phiên của người dùng vẫn đi kèm.
 *
 * <p><b>Logic:</b>
 * <ul>
 *   <li>Method không phải ghi (GET...): cho qua.</li>
 *   <li>Method ghi mà {@code Origin} <i>thiếu</i> hoặc <i>không có trong danh
 *       sách</i>: trả 403 và dừng, request không tới được controller.</li>
 *   <li>Method ghi và {@code Origin} hợp lệ: cho đi tiếp.</li>
 * </ul>
 *
 * <p><b>Ghi chú:</b>
 * <ul>
 *   <li><b>Không gắn {@code @Component}.</b> Boot tự đăng ký mọi bean kiểu
 *       Filter vào chuỗi servlet, nằm ngoài Spring Security. Lớp này được tạo
 *       bằng {@code new} trong {@link SecurityConfig}.</li>
 *   <li>Vì filter chạy trước MVC nên lỗi phải ghi qua {@link ApiErrorWriter},
 *       {@code GlobalExceptionHandler} không bắt được.</li>
 *   <li>So khớp nguyên văn từng ký tự: {@code http://localhost:5173} khác
 *       {@code http://127.0.0.1:5173}.</li>
 *   <li>Postman, curl không tự gửi {@code Origin}; phải thêm header thủ công.</li>
 *   <li>Đây chỉ là một lớp phòng thủ, đi kèm cookie {@code SameSite=Lax} và
 *       việc chỉ nhận JSON cho request ghi.</li>
 * </ul>
 *
 * @see SecurityConfig
 * @see ApiErrorWriter
 */
public class OriginFilter extends OncePerRequestFilter {

    /** Các method làm thay đổi dữ liệu, cần kiểm tra {@code Origin}. */
    private static final Set<String> WRITE = Set.of("POST", "PUT", "PATCH", "DELETE");

    /** Danh sách {@code Origin} được phép, lấy từ {@code app.security.allowed-origins}. */
    private final Set<String> allowed;

    /** Dùng để ghi lỗi 403 thẳng vào response. */
    private final ApiErrorWriter writer;

    /**
     * Tạo bộ lọc.
     *
     * @param allowed tập {@code Origin} hợp lệ, so khớp nguyên văn
     * @param writer  bộ ghi lỗi dùng chung của ứng dụng
     */
    public OriginFilter(Set<String> allowed, ApiErrorWriter writer) {
        this.allowed = allowed;
        this.writer = writer;
    }

    /**
     * Kiểm tra từng request. Spring gọi đúng một lần cho mỗi request.
     *
     * <p>Với request ghi có {@code Origin} sai, ghi 403 {@code FORBIDDEN} và
     * {@code return} mà <i>không</i> gọi {@code chain.doFilter}, nên request dừng
     * tại đây.
     *
     * @param req   request đang xử lý
     * @param res   response, dùng để ghi lỗi khi chặn
     * @param chain phần còn lại của chuỗi lọc
     * @throws ServletException nếu bộ lọc phía sau lỗi
     * @throws IOException      nếu ghi response lỗi
     */
    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        if (WRITE.contains(req.getMethod())) {
            String origin = req.getHeader("Origin");
            if (origin == null || !allowed.contains(origin)) {   // thiếu hoặc lạ đều 403
                writer.write(res, ErrorCode.FORBIDDEN, "Yêu cầu không hợp lệ.");
                return;
            }
        }
        chain.doFilter(req, res);
    }
}