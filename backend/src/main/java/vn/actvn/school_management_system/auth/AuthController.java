package vn.actvn.school_management_system.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.actvn.school_management_system.auth.dto.LoginRequest;
import vn.actvn.school_management_system.common.response.ApiResponse;

/**
 * Controller cung cấp các endpoint xác thực: đăng nhập, đăng xuất và lấy thông tin phiên hiện tại.
 *
 * <p><b>Cách dùng:</b>
 * <pre>{@code
 * POST /api/auth/login   (công khai, cần header Origin hợp lệ)
 * POST /api/auth/logout  (yêu cầu đã đăng nhập, cần header Origin hợp lệ)
 * GET  /api/auth/me      (yêu cầu đã đăng nhập)
 * }</pre>
 *
 * <p><b>Ghi chú:</b>
 * <ul>
 *   <li>Sử dụng session cookie ({@code SCSID}), không trả session ID trong body JSON.</li>
 *   <li>Tự đổi session ID sau khi đăng nhập thành công để chống tấn công Session Fixation.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /**
     * DTO hiển thị thông tin người dùng trả về cho frontend theo contract API.
     *
     * <p><b>Ghi chú:</b>
     * <ul>
     *   <li>Trường {@code id} được serialize thành {@link String} để đồng nhất toàn hệ thống.</li>
     * </ul>
     *
     * @param id       ID người dùng dạng chuỗi
     * @param fullName họ tên hiển thị
     * @param role     vai trò ({@code PARENT}, {@code TEACHER}, {@code ADMIN})
     */
    public record UserView(String id, String fullName, String role) {

        /**
         * Chuyển đổi từ {@link AuthUser} sang {@link UserView}.
         *
         * @param u đối tượng {@link AuthUser} nguồn
         * @return đối tượng {@link UserView} tương ứng
         */
        static UserView from(AuthUser u) {
            return new UserView(String.valueOf(u.id()), u.fullName(), u.role());
        }
    }

    /** Service xử lý nghiệp vụ đăng nhập và truy vấn thông tin tài khoản. */
    private final AuthService authService;

    /** Repository lưu trữ {@link SecurityContext} vào {@link HttpSession}. */
    private final SecurityContextRepository securityContextRepo = new HttpSessionSecurityContextRepository();

    /**
     * Khởi tạo {@link AuthController}.
     *
     * @param authService service xử lý xác thực
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Xử lý đăng nhập bằng email và mật khẩu, tạo session cookie và đổi session ID.
     *
     * <p><b>Cách dùng:</b>
     * <pre>{@code
     * POST /api/auth/login
     * Content-Type: application/json
     * Origin: http://localhost:5173
     *
     * { "email": "p01@actvn.edu.vn", "password": "..." }
     * }</pre>
     *
     * @param body     thông tin đăng nhập đã qua Bean Validation
     * @param request  HTTP request hiện tại dùng để tạo và đổi session ID
     * @param response HTTP response dùng để gắn cookie phiên
     * @return envelope thành công chứa {@code user}
     */
    @PostMapping("/login")
    public ApiResponse<?> login(
            @Valid @RequestBody LoginRequest body,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        AuthUser user = authService.login(body.email(), body.password());

        request.getSession(true);
        request.changeSessionId();

        var auth = UsernamePasswordAuthenticationToken.authenticated(
                user,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.role()))
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        securityContextRepo.saveContext(context, request, response);

        return ApiResponse.ok(Map.of("user", UserView.from(user)));
    }

    /**
     * Đăng xuất người dùng hiện tại bằng cách xóa {@link SecurityContext} và hủy {@link HttpSession}.
     *
     * <p><b>Cách dùng:</b>
     * <pre>{@code
     * POST /api/auth/logout
     * Origin: http://localhost:5173
     * }</pre>
     *
     * @param request HTTP request chứa phiên cần hủy
     * @return envelope thành công kèm thông báo {@code "Đã đăng xuất"}
     */
    @PostMapping("/logout")
    public ApiResponse<?> logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ApiResponse.ok(Map.of("message", "Đã đăng xuất"));
    }

    /**
     * Trả về thông tin tài khoản của người dùng đang đăng nhập.
     *
     * <p><b>Cách dùng:</b>
     * <pre>{@code
     * GET /api/auth/me
     * }</pre>
     *
     * <p><b>Ghi chú:</b>
     * <ul>
     *   <li>Truy vấn lại DB thông qua {@link AuthService#getCurrentUser(long)} để đảm bảo tài khoản vẫn còn hoạt động.</li>
     * </ul>
     *
     * @param authentication thông tin xác thực hiện tại do Spring Security cung cấp
     * @return envelope thành công chứa {@code user}
     */
    @GetMapping("/me")
    public ApiResponse<?> me(Authentication authentication) {
        AuthUser principal = (AuthUser) authentication.getPrincipal();
        AuthUser fresh = authService.getCurrentUser(principal.id());
        return ApiResponse.ok(Map.of("user", UserView.from(fresh)));
    }
}