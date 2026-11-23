package vn.actvn.school_management_system.db;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.core.io.ClassPathResource;

final class SqlScriptRunner {
    private static final Pattern TRIGGER =
            Pattern.compile("^\\s*CREATE\\s+(TEMP\\w*\\s+)?TRIGGER", Pattern.CASE_INSENSITIVE);
    private static final Pattern END = Pattern.compile("\\bEND\\s*;$", Pattern.CASE_INSENSITIVE);

    private SqlScriptRunner() {}

    static void run(Connection c, String classpathFile) throws Exception {
        String script;
        try (InputStream in = new ClassPathResource(classpathFile).getInputStream()) {
            script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        try (Statement st = c.createStatement()) {
            for (String sql : split(script)) st.execute(sql);
        }
    }

    /** Tách theo ';' cuối dòng; câu CREATE TRIGGER chỉ kết thúc ở 'END;'. Bỏ dòng comment '--'. */
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