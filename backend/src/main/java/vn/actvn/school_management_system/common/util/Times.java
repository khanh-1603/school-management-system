package vn.actvn.school_management_system.common.util;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public final class Times {
    // Cùng định dạng seed (strftime '%Y-%m-%dT%H:%M:%SZ'), không có phần lẻ giây,
    // để so sánh chuỗi (ví dụ locked_until) luôn đúng.
    private static final DateTimeFormatter ISO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);
    private Times() {}

    public static String nowIso(Clock clock) { return ISO.format(Instant.now(clock)); }
    public static String toIso(Instant i) { return ISO.format(i); }
    public static LocalDate today(Clock clock) { return LocalDate.now(clock); } // giờ Việt Nam
}