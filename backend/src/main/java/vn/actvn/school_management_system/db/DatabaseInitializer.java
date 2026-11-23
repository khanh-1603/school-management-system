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

@Component
public class DatabaseInitializer implements InitializingBean {
    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);
    private final DataSource ds;
    private final AppProperties props;

    public DatabaseInitializer(DataSource ds, AppProperties props) {
        this.ds = ds;
        this.props = props;
    }

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

    private static boolean hasTable(Connection c, String name) throws Exception {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }
}