package vn.actvn.school_management_system.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Đăng ký bean {@link Clock} của ứng dụng, đặt ở múi giờ Việt Nam.
 *
 * <p>Mọi nơi cần "bây giờ" hoặc "hôm nay" phải inject {@link Clock} này thay vì
 * gọi {@code LocalDate.now()} trực tiếp, để "hôm nay" luôn tính theo giờ Việt
 * Nam và có thể cố định khi test.
 *
 * <p><b>Cách dùng:</b>
 * <pre>{@code
 * public class LeaveService {
 *     private final Clock clock;
 *     public LeaveService(Clock clock) { this.clock = clock; }
 *
 *     void create(...) {
 *         LocalDate today = Times.today(clock);
 *     }
 * }
 * }</pre>
 *
 * <p><b>Ghi chú:</b>
 * <ul>
 *   <li>Khi viết test, thay bằng {@code Clock.fixed(...)} để cố định ngày cho
 *       các ca "không ngày tương lai", "{@code fromDate} không ở quá khứ".</li>
 *   <li>Múi giờ chỉ ảnh hưởng ngày nghiệp vụ; timestamp lưu DB vẫn là UTC
 *       (xem {@link vn.actvn.school_management_system.common.util.Times}).</li>
 * </ul>
 *
 * @see vn.actvn.school_management_system.common.util.Times
 */
@Configuration
public class ClockConfig {

    /**
     * Tạo đồng hồ hệ thống ở múi giờ {@code Asia/Ho_Chi_Minh} (UTC+7).
     *
     * @return bean {@link Clock} dùng chung toàn ứng dụng
     */
    @Bean
    Clock clock() { return Clock.system(ZoneId.of("Asia/Ho_Chi_Minh")); }
}