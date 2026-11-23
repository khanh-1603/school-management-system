package vn.actvn.school_management_system.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Db db, Security security) {
    public record Db(String dir, boolean seed) {}
    public record Security(List<String> allowedOrigins) {}
}