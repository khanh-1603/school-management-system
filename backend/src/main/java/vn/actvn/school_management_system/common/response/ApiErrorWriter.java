// common/response/ApiErrorWriter.java
package vn.actvn.school_management_system.common.response;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import vn.actvn.school_management_system.common.exception.ErrorCode;

/** Ghi envelope lỗi cho code chạy trước @RestControllerAdvice (filter, entry point). */
@Component
public class ApiErrorWriter {
    private final ObjectMapper mapper;
    public ApiErrorWriter(ObjectMapper mapper) { this.mapper = mapper; }

    public void write(HttpServletResponse res, ErrorCode code) throws IOException {
        write(res, code, code.defaultMessage());
    }

    public void write(HttpServletResponse res, ErrorCode code, String message) throws IOException {
        res.setStatus(code.status().value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        mapper.writeValue(res.getOutputStream(), ApiResponse.error(code, message, null));
    }
}