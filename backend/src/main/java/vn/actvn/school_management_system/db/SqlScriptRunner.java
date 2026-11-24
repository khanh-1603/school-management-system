package vn.actvn.school_management_system.db;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.core.io.ClassPathResource;

/**
 * Đọc một file {@code .sql} trong classpath và thực thi <b>từng câu lệnh</b> một.
 *
 * <p>Tự viết thay vì dùng bộ tách mặc định của Spring, vì trigger có dấu
 * {@code ;} bên trong thân ({@code BEGIN ...; END;}) nên bộ tách mặc định sẽ cắt
 * sai giữa chừng.
 *
 * <p><b>Cách dùng:</b>
 * <pre>{@code
 * c.setAutoCommit(false);
 * SqlScriptRunner.run(c, "db/schema.sql");
 * SqlScriptRunner.run(c, "db/seed.sql");
 * c.commit();
 * }</pre>
 *
 * <p><b>Ghi chú:</b>
 * <ul>
 *   <li>Lớp tiện ích, chỉ trong package {@code db}; không tạo object.</li>
 *   <li>Lớp này <i>không</i> tự quản lý transaction: nơi gọi quyết định commit
 *       hay rollback.</li>
 *   <li>Giới hạn: một câu lệnh kết thúc bằng {@code ;} rồi theo sau là comment
 *       {@code -- ...} cùng dòng sẽ không được nhận ra là đã kết thúc. Một dấu
 *       {@code ;} nằm cuối dòng bên trong chuỗi ký tự cũng bị hiểu nhầm. Hai
 *       trường hợp này hiện không có trong {@code schema.sql} và
 *       {@code seed.sql}.</li>
 * </ul>
 *
 * @see DatabaseInitializer
 */
final class SqlScriptRunner {

    /** Nhận diện đầu câu lệnh tạo trigger, bao gồm cả {@code TEMP}/{@code TEMPORARY}. */
    private static final Pattern TRIGGER =
            Pattern.compile("^\\s*CREATE\\s+(TEMP\\w*\\s+)?TRIGGER", Pattern.CASE_INSENSITIVE);

    /** Nhận diện phần kết thúc của trigger: từ {@code END} đứng riêng rồi {@code ;} ở cuối. */        
    private static final Pattern END = Pattern.compile("\\bEND\\s*;$", Pattern.CASE_INSENSITIVE);

    /** Chặn việc tạo object; lớp này chỉ chứa hàm {@code static}. */
    private SqlScriptRunner() {}

    /**
     * Đọc file trong classpath rồi chạy lần lượt từng câu lệnh.
     *
     * <p>Dừng ngay ở câu lệnh đầu tiên lỗi; các câu trước đó vẫn nằm trong
     * transaction của nơi gọi, nên nơi gọi cần {@code rollback()}.
     *
     * @param c             kết nối đang mở, nên tắt auto-commit trước khi gọi
     * @param classpathFile đường dẫn trong classpath, ví dụ {@code "db/schema.sql"}
     * @throws Exception nếu không đọc được file, file sai cú pháp
     *                   (xem {@link #split(String)}), hoặc một câu lệnh SQL thất bại
     */
    static void run(Connection c, String classpathFile) throws Exception {
        String script;
        try (InputStream in = new ClassPathResource(classpathFile).getInputStream()) {
            script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        try (Statement st = c.createStatement()) {
            for (String sql : split(script)) st.execute(sql);
        }
    }

    /**
     * Tách nội dung file SQL thành danh sách câu lệnh.
     *
     * <p><b>Quy tắc tách:</b>
     * <ol>
     *   <li>Bỏ ký tự {@code \r} (file có thể ở dạng CRLF của Windows).</li>
     *   <li>Bỏ dòng trống và dòng comment bắt đầu bằng {@code --}.</li>
     *   <li>Cộng dồn từng dòng; khi phần đã cộng kết thúc bằng {@code ;} thì coi
     *       là xong một câu.</li>
     *   <li>Ngoại lệ: câu {@code CREATE TRIGGER} chỉ xong khi kết thúc bằng
     *       {@code END;}, vì {@code ;} bên trong thân trigger không phải điểm
     *       kết thúc.</li>
     * </ol>
     *
     * @param script toàn bộ nội dung file SQL
     * @return danh sách câu lệnh, <b>đã bỏ dấu {@code ;} cuối</b>, theo đúng thứ tự trong file
     * @throws IllegalStateException nếu cuối file còn lại một câu lệnh chưa kết
     *                               thúc bằng {@code ;}
     */    
    static List<String> split(String script) {
        List<String> out = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        for (String line : script.replace("\r", "").split("\n")) {
            String s = line.strip();
            if (s.isEmpty() || s.startsWith("--")) continue;
            buf.append(line).append('\n');
            String stmt = buf.toString().strip();
            if (!stmt.endsWith(";")) continue;
            if (TRIGGER.matcher(stmt).find() && !END.matcher(stmt).find()) continue;
            out.add(stmt.substring(0, stmt.length() - 1));
            buf.setLength(0);
        }
        
        if (!buf.toString().isBlank()) {
            throw new IllegalStateException("Câu lệnh SQL không kết thúc bằng ';': " + buf);
        }
        return out;
    }
}