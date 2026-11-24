// config/SecurityConfig.java
package vn.actvn.school_management_system.config;

import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import vn.actvn.school_management_system.common.exception.ErrorCode;
import vn.actvn.school_management_system.common.response.ApiErrorWriter;

/**
 * Cấu hình bảo mật trung tâm: ai được vào đường dẫn nào, lỗi 401/403 ra sao, và
 * cách băm mật khẩu.
 *
 * <p>Gồm hai bean: chuỗi lọc bảo mật {@link #filterChain} và bộ băm mật khẩu
 * {@link #passwordEncoder()}. {@code @EnableMethodSecurity} bật thêm việc kiểm
 * tra quyền bằng annotation trên method (như {@code @PreAuthorize}).
 *
 * <p><b>Phân quyền theo URL (lớp kiểm tra vai trò thô):</b>
 * <table>
 *   <caption>Quy tắc truy cập</caption>
 *   <tr><th>Đường dẫn</th><th>Ai được vào</th></tr>
 *   <tr><td>{@code POST /api/auth/login}</td><td>Mọi người</td></tr>
 *   <tr><td>{@code /api/admin/**}</td><td>ADMIN</td></tr>
 *   <tr><td>{@code /api/teacher/**}</td><td>TEACHER</td></tr>
 *   <tr><td>{@code /api/students/**}, {@code /api/classes/**},
 *       {@code /api/leave-requests/**}, {@code /api/grades/**}</td>
 *       <td>PARENT, TEACHER (Admin không xem dữ liệu học tập)</td></tr>
 *   <tr><td>Còn lại</td><td>Đã đăng nhập</td></tr>
 * </table>
 *
 * <p><b>Ghi chú:</b>
 * <ul>
 *   <li>Đây chỉ kiểm tra <i>vai trò</i>. Quan hệ sở hữu (phụ huynh có con này
 *       không, giáo viên có dạy lớp này không) nằm ở Service và câu SQL.</li>
 *   <li>Mặc định Spring trả 403 cho người chưa đăng nhập; lớp này sửa lại thành
 *       401 bằng entry point tự viết.</li>
 *   <li>Request ghi đi qua {@link OriginFilter} trước mọi bước khác.</li>
 *   <li>Quy tắc có thứ tự: khớp quy tắc đầu tiên thì dùng, nên quy tắc cụ thể
 *       phải đặt trước {@code anyRequest()}.</li>
 * </ul>
 *
 * @see OriginFilter
 * @see ApiErrorWriter
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Dựng chuỗi lọc bảo mật cho toàn ứng dụng.
     *
     * <p>Các bước cấu hình:
     * <ol>
     *   <li>Tắt CSRF token mặc định, CORS, form login, HTTP Basic và logout mặc
     *       định (đăng nhập, đăng xuất sẽ tự viết).</li>
     *   <li>Dùng session, chỉ tạo khi cần.</li>
     *   <li>Chèn {@link OriginFilter} ở đầu chuỗi.</li>
     *   <li>Khai báo quy tắc truy cập theo URL.</li>
     *   <li>Đặt hai bộ xử lý lỗi trả JSON: chưa đăng nhập thì 401, sai vai trò
     *       thì 403.</li>
     * </ol>
     *
     * @param http   đối tượng cấu hình do Spring Security cung cấp
     * @param props  cấu hình ứng dụng, lấy danh sách {@code Origin} cho phép
     * @param writer bộ ghi lỗi JSON dùng chung
     * @return chuỗi lọc bảo mật đã dựng xong
     * @throws Exception nếu cấu hình không hợp lệ
     */
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, AppProperties props, ApiErrorWriter writer) throws Exception {
        // Tạo bằng new, không để thành bean: Boot sẽ tự đăng ký mọi Filter bean vào servlet chain.
        OriginFilter originFilter = new OriginFilter(Set.copyOf(props.security().allowedOrigins()), writer);

        http
            // CSRF token tắt có chủ đích: dùng OriginFilter + SameSite=Lax + chỉ nhận JSON.
            .csrf(c -> c.disable())
            .cors(c -> c.disable())          // frontend cùng origin (proxy dev server)
            .formLogin(f -> f.disable())
            .httpBasic(b -> b.disable())
            .logout(l -> l.disable())        // /auth/logout tự viết
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .addFilterBefore(originFilter, SecurityContextHolderFilter.class)
            .authorizeHttpRequests(a -> a
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/teacher/**").hasRole("TEACHER")
                // Admin không xem dữ liệu học tập; quan hệ sở hữu kiểm tra trong Service.
                .requestMatchers("/api/students/**", "/api/classes/**",
                                 "/api/leave-requests/**", "/api/grades/**").hasAnyRole("PARENT", "TEACHER")
                .anyRequest().authenticated())
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> writer.write(res, ErrorCode.UNAUTHENTICATED))
                .accessDeniedHandler((req, res, ex) -> writer.write(res, ErrorCode.FORBIDDEN)));
        return http.build();
    }

     /**
     * Tạo bộ băm và kiểm tra mật khẩu bằng BCrypt (cost 10).
     *
     * <p>Dùng để băm mật khẩu khi tạo hoặc đặt lại tài khoản, và để so sánh mật
     * khẩu người dùng nhập với {@code password_hash} khi đăng nhập. Đọc được hash
     * dạng {@code $2b$} có sẵn trong seed.
     *
     * @return bộ mã hóa mật khẩu dùng chung toàn ứng dụng
     */
    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(10); } // đọc được $2b$ của seed
}