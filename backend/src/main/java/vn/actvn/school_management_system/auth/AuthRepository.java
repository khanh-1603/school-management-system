package vn.actvn.school_management_system.auth;

import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Tầng truy xuất dữ liệu bảng {@code users} phục vụ xác thực và quản lý khóa tài khoản.
 *
 * <p><b>Cách dùng:</b>
 * <pre>{@code
 * Optional<AuthRepository.UserRow> user = authRepository.findByEmail("p01@actvn.edu.vn");
 * }</pre>
 *
 * <p><b>Ghi chú:</b>
 * <ul>
 *   <li>Sử dụng {@link JdbcClient} thuần với truy vấn tham số hóa, chống SQL Injection.</li>
 *   <li>Không chứa logic nghiệp vụ hay kiểm tra mật khẩu.</li>
 * </ul>
 */
@Repository
public class AuthRepository {

    /**
     * Bản ghi ánh xạ trực tiếp một dòng trong bảng {@code users} phục vụ luồng xác thực.
     *
     * <p><b>Ghi chú:</b>
     * <ul>
     *   <li>Chỉ dùng nội bộ giữa {@link AuthRepository} và {@link AuthService}.</li>
     *   <li>Tuyệt đối không trả trực tiếp ra API vì có chứa {@code passwordHash}.</li>
     * </ul>
     *
     * @param id           khóa chính của người dùng
     * @param email        email đã chuẩn hóa
     * @param passwordHash chuỗi băm mật khẩu (BCrypt)
     * @param fullName     họ tên hiển thị
     * @param role         vai trò ({@code PARENT}, {@code TEACHER}, {@code ADMIN})
     * @param active       trạng thái hoạt động ({@code true} nếu {@code is_active = 1})
     * @param failedLogins số lần nhập sai mật khẩu liên tiếp hiện tại
     * @param lockedUntil  thời điểm hết khóa dạng ISO 8601 UTC, hoặc {@code null} nếu không bị khóa
     */
    public record UserRow(
            long id,
            String email,
            String passwordHash,
            String fullName,
            String role,
            boolean active,
            int failedLogins,
            String lockedUntil
    ) {}

    /** Công cụ thực thi câu lệnh SQL tham số hóa của Spring JDBC. */
    private final JdbcClient jdbc;

    /**
     * Khởi tạo repository với {@link JdbcClient} do Spring inject.
     *
     * @param jdbc đối tượng {@code JdbcClient} kết nối tới SQLite
     */
    public AuthRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Tìm tài khoản người dùng theo email đã chuẩn hóa.
     *
     * <p><b>Cách dùng:</b>
     * <pre>{@code
     * Optional<UserRow> opt = repo.findByEmail(rawEmail.trim().toLowerCase(Locale.ROOT));
     * }</pre>
     *
     * <p><b>Ghi chú:</b>
     * <ul>
     *   <li>Email đầu vào phải được trim và chuyển về chữ thường trước khi gọi.</li>
     * </ul>
     *
     * @param normalizedEmail email đã chuẩn hóa
     * @return {@link Optional} chứa {@link UserRow} nếu tồn tại, ngược lại là {@link Optional#empty()}
     */
    public Optional<UserRow> findByEmail(String normalizedEmail) {
        return jdbc.sql("""
                SELECT id, email, password_hash, full_name, role, is_active, failed_logins, locked_until
                FROM users
                WHERE email = :email
                """)
                .param("email", normalizedEmail)
                .query((rs, rowNum) -> new UserRow(
                        rs.getLong("id"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getString("full_name"),
                        rs.getString("role"),
                        rs.getInt("is_active") == 1,
                        rs.getInt("failed_logins"),
                        rs.getString("locked_until")
                ))
                .optional();
    }

    /**
     * Tìm tài khoản người dùng theo ID.
     *
     * <p><b>Cách dùng:</b>
     * <pre>{@code
     * Optional<UserRow> opt = repo.findById(principal.id());
     * }</pre>
     *
     * @param id khóa chính của người dùng
     * @return {@link Optional} chứa {@link UserRow} nếu tồn tại, ngược lại là {@link Optional#empty()}
     */
    public Optional<UserRow> findById(long id) {
        return jdbc.sql("""
                SELECT id, email, password_hash, full_name, role, is_active, failed_logins, locked_until
                FROM users
                WHERE id = :id
                """)
                .param("id", id)
                .query((rs, rowNum) -> new UserRow(
                        rs.getLong("id"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getString("full_name"),
                        rs.getString("role"),
                        rs.getInt("is_active") == 1,
                        rs.getInt("failed_logins"),
                        rs.getString("locked_until")
                ))
                .optional();
    }

    /**
     * Cập nhật số lần đăng nhập thất bại và thời điểm hết khóa cho tài khoản.
     *
     * <p><b>Cách dùng:</b>
     * <pre>{@code
     * repo.updateLoginFailure(user.id(), 0, "2026-11-24T11:30:00Z");
     * }</pre>
     *
     * @param userId       ID tài khoản cần cập nhật
     * @param failedLogins số lần sai mới
     * @param lockedUntil  thời điểm hết khóa (ISO 8601 UTC), truyền {@code null} nếu chưa khóa
     */
    public void updateLoginFailure(long userId, int failedLogins, String lockedUntil) {
        jdbc.sql("""
                UPDATE users
                SET failed_logins = :failedLogins, locked_until = :lockedUntil
                WHERE id = :id
                """)
                .param("failedLogins", failedLogins)
                .param("lockedUntil", lockedUntil)
                .param("id", userId)
                .update();
    }

    /**
     * Đặt lại bộ đếm đăng nhập sai về 0 và gỡ trạng thái khóa sau khi đăng nhập thành công.
     *
     * <p><b>Cách dùng:</b>
     * <pre>{@code
     * repo.resetLoginFailures(user.id());
     * }</pre>
     *
     * @param userId ID tài khoản cần đặt lại
     */
    public void resetLoginFailures(long userId) {
        jdbc.sql("""
                UPDATE users
                SET failed_logins = 0, locked_until = NULL
                WHERE id = :id
                """)
                .param("id", userId)
                .update();
    }
}