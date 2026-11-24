package vn.actvn.school_management_system.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Ánh xạ phần {@code app:} trong {@code application.yml} thành object Java có
 * kiểu, để code đọc cấu hình mà không phải gõ chuỗi tên khóa.
 *
 * <p>Ví dụ với cấu hình:
 * <pre>{@code
 * app:
 *   db:
 *     dir: data
 *     seed: true
 *   security:
 *     allowed-origins:
 *       - http://localhost:5173
 * }</pre>
 * thì {@code props.db().seed()} trả {@code true} và
 * {@code props.security().allowedOrigins()} trả danh sách một phần tử.
 *
 * <p><b>Cách dùng:</b> inject qua constructor vào bất kỳ bean nào cần cấu hình
 * (xem {@link SecurityConfig}, {@code DatabaseInitializer}).
 *
 * <p><b>Ghi chú:</b>
 * <ul>
 *   <li>Cần {@code @ConfigurationPropertiesScan} ở lớp main thì Spring mới tạo
 *       bean này.</li>
 *   <li>Spring tự đổi kiểu tên: khóa {@code allowed-origins} trong YAML ứng với
 *       thuộc tính {@code allowedOrigins} trong Java.</li>
 *   <li>Record bất biến: cấu hình chỉ đọc, không sửa được lúc đang chạy.</li>
 * </ul>
 *
 * @param db       cấu hình cơ sở dữ liệu
 * @param security cấu hình bảo mật
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Db db, Security security) {

    /**
     * Cấu hình khởi tạo cơ sở dữ liệu ({@code app.db}).
     *
     * @param dir  thư mục chứa file SQLite và thư mục session; phải khớp với
     *             đường dẫn trong {@code spring.datasource.url}
     * @param seed {@code true} thì nạp dữ liệu mẫu khi tạo DB lần đầu;
     *             {@code false} thì để DB rỗng
     */
    public record Db(String dir, boolean seed) {}

    /**
     * Cấu hình bảo mật ({@code app.security}).
     *
     * @param allowedOrigins danh sách {@code Origin} được phép gửi request ghi,
     *                       so khớp nguyên văn (gồm cả scheme và cổng), ví dụ
     *                       {@code http://localhost:5173}
     */
    public record Security(List<String> allowedOrigins) {}
}