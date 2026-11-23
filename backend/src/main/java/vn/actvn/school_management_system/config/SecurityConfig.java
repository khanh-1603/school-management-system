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

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

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

    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(10); } // đọc được $2b$ của seed
}