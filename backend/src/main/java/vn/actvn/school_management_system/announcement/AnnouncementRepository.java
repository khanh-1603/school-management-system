package vn.actvn.school_management_system.announcement;

import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import vn.actvn.school_management_system.announcement.dto.AnnouncementItem;

/**
 * Repository thao tác bảng {@code announcements} và kiểm tra quyền truy cập lớp học
 * bằng Spring JDBC.
 */
@Repository
public class AnnouncementRepository {

    /** Client thực thi truy vấn SQL tham số hóa. */
    private final JdbcClient jdbcClient;

    /**
     * Khởi tạo {@code AnnouncementRepository}.
     *
     * @param jdbcClient client truy cập cơ sở dữ liệu
     */
    public AnnouncementRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    /**
     * Kiểm tra giáo viên có phải là giáo viên chủ nhiệm của lớp hay không.
     *
     * @param teacherId ID tài khoản giáo viên
     * @param classId   ID lớp học
     * @return {@code true} nếu đúng là GVCN của lớp, ngược lại {@code false}
     */
    public boolean isHomeroomTeacher(long teacherId, long classId) {
        String sql = """
                SELECT COUNT(1)
                FROM classes
                WHERE id = :classId AND homeroom_teacher_id = :teacherId
                """;
        Integer count = jdbcClient.sql(sql)
                .param("classId", classId)
                .param("teacherId", teacherId)
                .query(Integer.class)
                .single();
        return count != null && count > 0;
    }

    /**
     * Kiểm tra phụ huynh có ít nhất một học sinh đã liên kết đang học tại lớp hay không.
     *
     * @param parentId ID tài khoản phụ huynh
     * @param classId  ID lớp học
     * @return {@code true} nếu phụ huynh có con học tại lớp, ngược lại {@code false}
     */
    public boolean isParentOfClass(long parentId, long classId) {
        String sql = """
                SELECT COUNT(1)
                FROM parent_students ps
                JOIN students s ON s.id = ps.student_id
                WHERE ps.parent_id = :parentId AND s.class_id = :classId
                """;
        Integer count = jdbcClient.sql(sql)
                .param("parentId", parentId)
                .param("classId", classId)
                .query(Integer.class)
                .single();
        return count != null && count > 0;
    }

    /**
     * Lấy danh sách thông báo của một lớp học, sắp xếp mới nhất trước.
     *
     * @param classId ID lớp học
     * @return danh sách {@link AnnouncementItem}, không bao giờ {@code null}
     */
    public List<AnnouncementItem> findByClassId(long classId) {
        String sql = """
                SELECT id, title, content, created_at
                FROM announcements
                WHERE class_id = :classId
                ORDER BY created_at DESC, id DESC
                """;
        return jdbcClient.sql(sql)
                .param("classId", classId)
                .query((rs, rowNum) -> new AnnouncementItem(
                        String.valueOf(rs.getLong("id")),
                        rs.getString("title"),
                        rs.getString("content"),
                        rs.getString("created_at")
                ))
                .list();
    }

    /**
     * Thêm mới một thông báo vào bảng {@code announcements}.
     *
     * @param classId   ID lớp học nhận thông báo
     * @param authorId  ID giáo viên chủ nhiệm tạo thông báo
     * @param title     tiêu đề đã chuẩn hóa
     * @param content   nội dung đã chuẩn hóa
     * @param createdAt thời điểm tạo chuẩn ISO 8601 UTC
     * @return ID tự tăng của thông báo vừa tạo
     */
    public long insert(
            long classId,
            long authorId,
            String title,
            String content,
            String createdAt
    ) {
        String sql = """
                INSERT INTO announcements (class_id, author_id, title, content, created_at)
                VALUES (:classId, :authorId, :title, :content, :createdAt)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcClient.sql(sql)
                .param("classId", classId)
                .param("authorId", authorId)
                .param("title", title)
                .param("content", content)
                .param("createdAt", createdAt)
                .update(keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Không lấy được ID của thông báo vừa tạo.");
        }
        return key.longValue();
    }
}