package vn.actvn.school_management_system.audit;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Repository thao tác trực tiếp với bảng {@code audit_logs} bằng Spring JDBC.
 *
 * <p>Bảng {@code audit_logs} chỉ cho phép thêm mới (append-only) và đọc;
 * không cung cấp phương thức cập nhật hay xóa.
 */
@Repository
public class AuditRepository {

    /** Client thực thi truy vấn SQL tham số hóa. */
    private final JdbcClient jdbcClient;

    /**
     * Khởi tạo repository với {@link JdbcClient}.
     *
     * @param jdbcClient client truy cập cơ sở dữ liệu
     */
    public AuditRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    /**
     * Bản ghi thô đọc từ bảng {@code audit_logs} trước khi parse JSON.
     *
     * @param id          khóa chính của bản ghi log
     * @param actorUserId ID người thực hiện
     * @param studentId   ID học sinh liên quan (có thể {@code null})
     * @param action      chuỗi hành động ({@code CREATE}, {@code UPDATE}, {@code DELETE})
     * @param entityType  tên bảng bị tác động
     * @param entityId    ID bản ghi bị tác động
     * @param oldValue    chuỗi JSON giá trị cũ (có thể {@code null})
     * @param newValue    chuỗi JSON giá trị mới (có thể {@code null})
     * @param createdAt   chuỗi thời điểm ISO 8601 UTC
     */
    public record AuditRow(
            long id,
            long actorUserId,
            Long studentId,
            String action,
            String entityType,
            String entityId,
            String oldValue,
            String newValue,
            String createdAt
    ) {
    }

    /**
     * Ghi một dòng nhật ký mới vào bảng {@code audit_logs}.
     *
     * <p>Gọi phương thức này bên trong {@code @Transactional} của nghiệp vụ chính
     * để đảm bảo thay đổi dữ liệu và audit log cùng thành công hoặc cùng rollback.
     *
     * @param actorUserId ID người dùng thực hiện thao tác
     * @param studentId   ID học sinh liên quan, hoặc {@code null} nếu không áp dụng
     * @param action      hành động ({@code CREATE}, {@code UPDATE}, {@code DELETE})
     * @param entityType  tên bảng bị tác động
     * @param entityId    ID bản ghi bị tác động
     * @param oldValue    chuỗi JSON giá trị cũ, hoặc {@code null}
     * @param newValue    chuỗi JSON giá trị mới, hoặc {@code null}
     * @param createdAt   thời điểm ghi log theo chuẩn ISO 8601 UTC
     */
    public void insert(
            long actorUserId,
            Long studentId,
            AuditAction action,
            String entityType,
            String entityId,
            String oldValue,
            String newValue,
            String createdAt
    ) {
        String sql = """
                INSERT INTO audit_logs (
                    actor_user_id, student_id, action, entity_type, entity_id,
                    old_value, new_value, created_at
                ) VALUES (
                    :actorUserId, :studentId, :action, :entityType, :entityId,
                    :oldValue, :newValue, :createdAt
                )
                """;

        jdbcClient.sql(sql)
                .param("actorUserId", actorUserId)
                .param("studentId", studentId)
                .param("action", action.name())
                .param("entityType", entityType)
                .param("entityId", entityId)
                .param("oldValue", oldValue)
                .param("newValue", newValue)
                .param("createdAt", createdAt)
                .update();
    }

    /**
     * Truy vấn danh sách audit log theo các tiêu chí lọc tùy chọn, sắp xếp mới nhất trước.
     *
     * @param studentId  lọc theo ID học sinh (bỏ qua nếu {@code null})
     * @param action     lọc theo loại hành động (bỏ qua nếu {@code null})
     * @param entityType lọc theo tên bảng (bỏ qua nếu {@code null})
     * @param entityId   lọc theo ID bản ghi bị tác động (bỏ qua nếu {@code null})
     * @return danh sách bản ghi khớp điều kiện, không bao giờ {@code null}
     */
    public List<AuditRow> findAll(
            Long studentId,
            AuditAction action,
            String entityType,
            String entityId
    ) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, actor_user_id, student_id, action, entity_type,
                       entity_id, old_value, new_value, created_at
                FROM audit_logs
                WHERE 1 = 1
                """);

        Map<String, Object> params = new LinkedHashMap<>();

        if (studentId != null) {
            sql.append(" AND student_id = :studentId");
            params.put("studentId", studentId);
        }
        if (action != null) {
            sql.append(" AND action = :action");
            params.put("action", action.name());
        }
        if (entityType != null) {
            sql.append(" AND entity_type = :entityType");
            params.put("entityType", entityType);
        }
        if (entityId != null) {
            sql.append(" AND entity_id = :entityId");
            params.put("entityId", entityId);
        }

        sql.append(" ORDER BY created_at DESC, id DESC");

        var spec = jdbcClient.sql(sql.toString());
        for (var entry : params.entrySet()) {
            spec = spec.param(entry.getKey(), entry.getValue());
        }

        return spec.query((rs, rowNum) -> {
            long rawStudentId = rs.getLong("student_id");
            Long mappedStudentId = rs.wasNull() ? null : rawStudentId;
            return new AuditRow(
                    rs.getLong("id"),
                    rs.getLong("actor_user_id"),
                    mappedStudentId,
                    rs.getString("action"),
                    rs.getString("entity_type"),
                    rs.getString("entity_id"),
                    rs.getString("old_value"),
                    rs.getString("new_value"),
                    rs.getString("created_at")
            );
        }).list();
    }
}