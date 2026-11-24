package vn.actvn.school_management_system.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.actvn.school_management_system.auth.AuthRepository.UserRow;
import vn.actvn.school_management_system.common.exception.AppException;
import vn.actvn.school_management_system.common.exception.ErrorCode;
import vn.actvn.school_management_system.common.util.Times;

/**
 * Tầng xử lý nghiệp vụ xác thực: kiểm tra mật khẩu, chuẩn hóa email và cơ chế khóa tài khoản.
 *
 * <p><b>Cách dùng:</b>
 * <pre>{@code
 * AuthUser user = authService.login(req.email(), req.password());
 * }</pre>
 *
 * <p><b>Ghi chú:</b>
 * <ul>
 *   <li>Mọi nguyên nhân đăng nhập thất bại (sai email, sai mật khẩu, bị vô hiệu hóa, đang bị khóa)
 *       đều trả về cùng một mã {@link ErrorCode#UNAUTHENTICATED} và cùng một thông báo để tránh lộ thông tin.</li>
 *   <li>Sai mật khẩu 5 lần liên tiếp sẽ khóa tài khoản trong 5 phút.</li>
 * </ul>
 */
@Service
public class AuthService {

    /** Thông báo lỗi dùng chung cho mọi trường hợp đăng nhập thất bại. */
    private static final String LOGIN_FAILED_MSG = "Email hoặc mật khẩu không đúng.";

    /** Số lần nhập sai mật khẩu liên tiếp tối đa trước khi khóa tài khoản. */
    private static final int MAX_FAILED_ATTEMPTS = 5;

    /** Thời lượng khóa tài khoản khi vượt quá số lần nhập sai cho phép. */
    private static final Duration LOCK_DURATION = Duration.ofMinutes(5);

    /** Repository truy vấn bảng {@code users}. */
    private final AuthRepository repo;

    /** Bộ đối chiếu mật khẩu băm (BCrypt). */
    private final PasswordEncoder passwordEncoder;

    /** Đồng hồ hệ thống (múi giờ {@code Asia/Ho_Chi_Minh}) dùng để tính thời điểm khóa. */
    private final Clock clock;

    /**
     * Khởi tạo {@link AuthService} với các dependency cần thiết.
     *
     * @param repo            repository truy cập bảng {@code users}
     * @param passwordEncoder bộ kiểm tra mật khẩu băm
     * @param clock           đồng hồ hệ thống dùng chung
     */
    public AuthService(AuthRepository repo, PasswordEncoder passwordEncoder, Clock clock) {
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    /**
     * Xác thực thông tin đăng nhập và cập nhật trạng thái khóa nếu thất bại.
     *
     * <p><b>Cách dùng:</b>
     * <pre>{@code
     * AuthUser user = authService.login(body.email(), body.password());
     * }</pre>
     *
     * <p><b>Ghi chú:</b>
     * <ul>
     *   <li>Email được trim và chuyển về chữ thường trước khi truy vấn; mật khẩu giữ nguyên.</li>
     *   <li>Trong thời gian đang bị khóa, mọi yêu cầu đăng nhập (kể cả đúng mật khẩu) đều bị từ chối.</li>
     * </ul>
     *
     * @param rawEmail    email do người dùng gửi lên
     * @param rawPassword mật khẩu dạng rõ do người dùng gửi lên
     * @return đối tượng {@link AuthUser} hợp lệ để lưu vào session
     * @throws AppException với {@link ErrorCode#UNAUTHENTICATED} nếu xác thực thất bại
     */
    public AuthUser login(String rawEmail, String rawPassword) {
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        UserRow user = repo.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED, LOGIN_FAILED_MSG));

        if (!user.active()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED, LOGIN_FAILED_MSG);
        }

        String nowIso = Times.nowIso(clock);
        if (user.lockedUntil() != null && user.lockedUntil().compareTo(nowIso) > 0) {
            throw new AppException(ErrorCode.UNAUTHENTICATED, LOGIN_FAILED_MSG);
        }

        if (!passwordEncoder.matches(rawPassword, user.passwordHash())) {
            int nextFailures = user.failedLogins() + 1;
            String lockedUntil = null;
            if (nextFailures >= MAX_FAILED_ATTEMPTS) {
                lockedUntil = Times.toIso(Instant.now(clock).plus(LOCK_DURATION));
                nextFailures = 0;
            }
            repo.updateLoginFailure(user.id(), nextFailures, lockedUntil);
            throw new AppException(ErrorCode.UNAUTHENTICATED, LOGIN_FAILED_MSG);
        }

        if (user.failedLogins() > 0 || user.lockedUntil() != null) {
            repo.resetLoginFailures(user.id());
        }

        return new AuthUser(user.id(), user.email(), user.fullName(), user.role());
    }

    /**
     * Lấy lại thông tin mới nhất của người dùng đang đăng nhập từ cơ sở dữ liệu.
     *
     * <p><b>Cách dùng:</b>
     * <pre>{@code
     * AuthUser fresh = authService.getCurrentUser(principal.id());
     * }</pre>
     *
     * <p><b>Ghi chú:</b>
     * <ul>
     *   <li>Kiểm tra lại trạng thái {@code is_active} để kịp thời chặn nếu tài khoản vừa bị Admin vô hiệu hóa.</li>
     * </ul>
     *
     * @param userId ID người dùng lấy từ session hiện tại
     * @return thông tin {@link AuthUser} mới nhất
     * @throws AppException với {@link ErrorCode#UNAUTHENTICATED} nếu tài khoản không tồn tại hoặc đã bị vô hiệu hóa
     */
    public AuthUser getCurrentUser(long userId) {
        UserRow user = repo.findById(userId)
                .filter(UserRow::active)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));
        return new AuthUser(user.id(), user.email(), user.fullName(), user.role());
    }
}