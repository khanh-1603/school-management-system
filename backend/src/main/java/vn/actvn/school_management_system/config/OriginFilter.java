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

public class OriginFilter extends OncePerRequestFilter {
    private static final Set<String> WRITE = Set.of("POST", "PUT", "PATCH", "DELETE");
    private final Set<String> allowed;
    private final ApiErrorWriter writer;

    public OriginFilter(Set<String> allowed, ApiErrorWriter writer) {
        this.allowed = allowed;
        this.writer = writer;
    }

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