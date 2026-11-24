package vn.actvn.school_management_system.common.util;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Nơi <b>duy nhất</b> lấy "bây giờ" và "hôm nay", để định dạng thời gian đồng
 * nhất toàn hệ thống.
 *
 * <p>Có hai khái niệm cần tách bạch:
 * <ul>
 *   <li><b>Timestamp</b> (thời điểm xảy ra sự kiện): lưu ở dạng UTC,
 *       ví dụ {@code 2026-11-24T03:00:00Z}.</li>
 *   <li><b>Ngày nghiệp vụ</b> ("hôm nay" để kiểm tra điểm danh, đơn nghỉ): tính
 *       theo <i>giờ Việt Nam</i>, lấy từ múi giờ của {@link Clock}.</li>
 * </ul>
 *
 * <p><b>Cách dùng:</b>
 * <pre>{@code
 * String createdAt = Times.nowIso(clock);      // ghi vào cột created_at
 * LocalDate today  = Times.today(clock);       // so sánh với fromDate
 * }</pre>
 *
 * <p><b>Ghi chú:</b>
 * <ul>
 *   <li>Định dạng trùng với seed ({@code strftime('%Y-%m-%dT%H:%M:%SZ')}),
 *       không có phần lẻ giây. Nhờ đó so sánh chuỗi (như {@code locked_until})
 *       luôn đúng thứ tự thời gian.</li>
 *   <li>Luôn nhận {@link Clock} thay vì gọi thẳng {@code Instant.now()}, để khi
 *       test có thể thay bằng {@code Clock.fixed(...)} và cố định ngày.</li>
 *   <li>Lớp tiện ích: chỉ có hàm {@code static}.</li>
 * </ul>
 *
 * @see vn.actvn.school_management_system.config.ClockConfig
 */
public final class Times {
    
    /**
     * Định dạng thời điểm chuẩn của DB: năm-tháng-ngày, chữ T, giờ:phút:giây, chữ Z.
     * Ép múi giờ UTC để có thể format trực tiếp một {@link Instant}.
     */
    private static final DateTimeFormatter ISO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);
    
    /** Chặn việc tạo object; lớp này chỉ chứa hàm {@code static}. */
    private Times() {}

    /**
     * Lấy thời điểm hiện tại dưới dạng chuỗi để lưu vào DB.
     *
     * <p>Kết quả luôn là UTC, bất kể múi giờ của {@code clock}.
     *
     * @param clock đồng hồ của ứng dụng (bean {@code Clock} múi giờ Việt Nam)
     * @return chuỗi dạng {@code 2026-11-24T03:00:00Z}, làm tròn xuống giây
     */
    public static String nowIso(Clock clock) { return ISO.format(Instant.now(clock)); }

    /**
     * Đổi một {@link Instant} có sẵn sang chuỗi lưu DB.
     *
     * <p>Dùng khi cần ghi một mốc thời gian đã tính trước, ví dụ
     * {@code locked_until} = bây giờ + 5 phút.
     *
     * @param i thời điểm cần đổi
     * @return chuỗi cùng định dạng với {@link #nowIso(Clock)}
     */
    public static String toIso(Instant i) { return ISO.format(i); }

     /**
     * Lấy "hôm nay" theo múi giờ của {@code clock}.
     *
     * <p>Với bean {@code Clock} của dự án đó là giờ Việt Nam
     * ({@code Asia/Ho_Chi_Minh}). Khác ngày UTC trong khoảng từ 0 giờ đến 7 giờ
     * sáng, nên đừng thay bằng {@code LocalDate.now(ZoneOffset.UTC)}.
     *
     * @param clock đồng hồ của ứng dụng
     * @return ngày hiện tại theo giờ Việt Nam, dùng cho các quy tắc như
     *         "không ngày tương lai", "{@code fromDate} không ở quá khứ"
     */
    public static LocalDate today(Clock clock) { return LocalDate.now(clock); } // giờ Việt Nam
}