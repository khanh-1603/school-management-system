package vn.actvn.school_management_system.auth;

import java.io.Serializable;

/**
 * Đại diện cho thông tin người dùng đã xác thực, được lưu trong {@code HttpSession}.
 *
 * <p><b>Cách dùng:</b>
 * <pre>{@code
 * AuthUser currentUser = (AuthUser) authentication.getPrincipal();
 * long userId = currentUser.id();
 * }</pre>
 *
 * <p><b>Ghi chú:</b>
 * <ul>
 *   <li>Bắt buộc implement {@link Serializable} vì Tomcat bật {@code session.persistent=true}.</li>
 *   <li>Không lưu mật khẩu hay dữ liệu nhạy cảm vào đối tượng này.</li>
 * </ul>
 *
 * @param id       ID người dùng trong bảng {@code users}
 * @param email    email đã chuẩn hóa (chữ thường, đã trim)
 * @param fullName họ tên hiển thị của người dùng
 * @param role     vai trò của người dùng ({@code PARENT}, {@code TEACHER}, {@code ADMIN})
 */
public record AuthUser(
        long id,
        String email,
        String fullName,
        String role
) implements Serializable {}