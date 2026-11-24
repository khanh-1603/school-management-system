// db/DatabaseInitializer.java
package vn.actvn.school_management_system.db;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import vn.actvn.school_management_system.config.AppProperties;

/**
 * Tạo cơ sở dữ liệu <b>lần đầu</b> khi ứng dụng khởi động: chạy
 * {@code schema.sql}, rồi {@code seed.sql} nếu cấu hình cho phép.
 *
 * <p>Thay cho {@code spring.sql.init} của Spring. Cơ chế mặc định chạy lại mỗi
 * lần khởi động, trong khi seed có ID cố định và ngày tính theo ngày chạy, nên
 * lần chạy thứ hai sẽ lỗi "table already exists".
 *
 * <p><b>Luồng chạy:</b>
 * <ol>
 *   <li>Tạo thư mục chứa file DB nếu chưa có.</li>
 *   <li>Nếu bảng {@code users} đã tồn tại: coi như DB đã khởi tạo, bỏ qua.</li>
 *   <li>Nếu chưa: mở transaction, chạy {@code db/schema.sql}, rồi
 *       {@code db/seed.sql} nếu {@code app.db.seed=true}, sau đó commit.</li>
 *   <li>Có lỗi giữa chừng thì rollback và ném lỗi, ứng dụng không khởi động.</li>
 * </ol>
 *
 * <p><b>Ghi chú:</b>
 * <ul>
 *   <li>Muốn làm lại từ đầu (ví dụ để seed tính lại ngày): dừng ứng dụng, xóa
 *       file {@code data/school.db}, chạy lại.</li>
 *   <li>Seed dùng ngày tương đối ("hôm nay trừ 3 ngày"), nên dữ liệu "cũ đi"
 *       theo ngày chạy lần đầu.</li>
 *   <li>Rollback hủy cả schema lẫn seed vì DDL của SQLite nằm trong transaction;
 *       DB không bị tạo nửa chừng, lần chạy sau thử lại từ đầu.</li>
 *   <li>Pool chỉ có 1 kết nối nên lớp này giữ kết nối duy nhất đến hết hàm. Lớp
 *       khác không được truy vấn DB lúc khởi động.</li>
 *   <li>Chỉ kiểm tra có bảng {@code users} hay chưa, không so khớp phiên bản
 *       schema: sửa {@code schema.sql} sau khi DB đã tạo sẽ không có hiệu lực
 *       cho tới khi xóa DB.</li>
 * </ul>
 *
 * @see SqlScriptRunner
 * @see AppProperties
 */
@Component
public class DatabaseInitializer implements InitializingBean {

    /** Logger ghi kết quả khởi tạo. */
    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);
    
    /** Nguồn kết nối tới SQLite (pool Hikari, kích thước 1). */
    private final DataSource ds;
    
    /** Cấu hình ứng dụng, dùng để lấy thư mục DB và cờ nạp seed. */
    private final AppProperties props;

    /**
     * Tạo bộ khởi tạo. Spring tự truyền các tham số qua constructor.
     *
     * @param ds    nguồn kết nối tới DB
     * @param props cấu hình ứng dụng ({@code app.db.dir}, {@code app.db.seed})
     */
    public DatabaseInitializer(DataSource ds, AppProperties props) {
        this.ds = ds;
        this.props = props;
    }

    /**
     * Khởi tạo DB nếu cần. Spring gọi tự động ngay sau khi bean được tạo xong,
     * trước khi ứng dụng nhận request.
     *
     * @throws Exception nếu tạo thư mục, chạy script hoặc commit thất bại
     *                   (đã rollback trước khi ném lại)
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        Files.createDirectories(Path.of(props.db().dir()));
        try (Connection c = ds.getConnection()) {
            if (hasTable(c, "users")) {
                log.info("DB đã có schema, bỏ qua khởi tạo.");
                return;
            }
            c.setAutoCommit(false);
            try {
                SqlScriptRunner.run(c, "db/schema.sql");
                if (props.db().seed()) SqlScriptRunner.run(c, "db/seed.sql");
                c.commit();
                log.info("Đã khởi tạo DB (seed={}).", props.db().seed());
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }

    /**
     * Kiểm tra một bảng đã tồn tại trong DB chưa, bằng cách tra {@code sqlite_master}.
     *
     * @param c    kết nối đang mở
     * @param name tên bảng cần kiểm tra
     * @return {@code true} nếu bảng đã có
     * @throws Exception nếu truy vấn thất bại
     */
    private static boolean hasTable(Connection c, String name) throws Exception {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }
}