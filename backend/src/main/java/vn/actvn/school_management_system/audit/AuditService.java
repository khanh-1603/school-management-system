package vn.actvn.school_management_system.audit;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import vn.actvn.school_management_system.audit.dto.AuditLogItem;
import vn.actvn.school_management_system.common.exception.AppException;
import vn.actvn.school_management_system.common.exception.ErrorCode;

/**
 * Service xử lý ghi và tra cứu nhật ký kiểm toán ({@code audit_logs}).
 *
 * <p>Các service nghiệp vụ khác inject {@code AuditService} và gọi {@link #record}
 * bên trong transaction của chính mình để đảm bảo tính nguyên tử.
 */
@Service
public class AuditService {

    /** Tập tên bảng hợp lệ được phép ghi nhận và lọc trong {@code audit_logs}. */
    private static final Set<String> ALLOWED_ENTITY_TYPES = Set.of(
            "users",
            "classes",
            "students",
            "parent_students",
            "subjects",
            "teaching_assignments",
            "timetable",
            "grades",
            "attendance",
            "leave_requests",
            "announcements"
    );

    /** Repository thao tác bảng {@code audit_logs}. */
    private final AuditRepository auditRepository;

    /** ObjectMapper dùng để chuyển đổi giữa đối tượng Java và chuỗi JSON. */
    private final ObjectMapper objectMapper;

    /** Đồng hồ hệ thống dùng để lấy thời điểm ghi log chuẩn UTC. */
    private final Clock clock;

    /**
     * Khởi tạo {@code AuditService}.
     *
     * @param auditRepository repository truy cập bảng {@code audit_logs}
     * @param objectMapper    mapper JSON (Jackson 3)
     * @param clock           đồng hồ hệ thống đã cấu hình
     */
    public AuditService(
            AuditRepository auditRepository,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.auditRepository = auditRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /**
     * Ghi một bản ghi audit log mới trong transaction hiện hành.
     *
     * <p>Các giá trị {@code oldValue} và {@code newValue} (thường là {@code Map} hoặc DTO)
     * sẽ được serialize sang chuỗi JSON; nếu truyền {@code null} thì lưu {@code NULL}.
     *
     * @param actorUserId ID người dùng thực hiện hành động
     * @param studentId   ID học sinh liên quan, hoặc {@code null} nếu không gắn học sinh
     * @param action      loại hành động ({@code CREATE}, {@code UPDATE}, {@code DELETE})
     * @param entityType  tên bảng bị tác động (phải thuộc {@link #ALLOWED_ENTITY_TYPES})
     * @param entityId    ID của bản ghi bị tác động
     * @param oldValue    dữ liệu cũ trước khi thay đổi (có thể {@code null})
     * @param newValue    dữ liệu mới sau khi thay đổi (có thể {@code null})
     * @throws IllegalArgumentException nếu {@code entityType} không nằm trong danh sách cho phép
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(
            long actorUserId,
            Long studentId,
            AuditAction action,
            String entityType,
            String entityId,
            Object oldValue,
            Object newValue
    ) {
        if (entityType == null || !ALLOWED_ENTITY_TYPES.contains(entityType)) {
            throw new IllegalArgumentException("entityType không hợp lệ: " + entityType);
        }

        String oldJson = oldValue == null ? null : objectMapper.writeValueAsString(oldValue);
        String newJson = newValue == null ? null : objectMapper.writeValueAsString(newValue);
        String createdAt = Instant.now(clock).truncatedTo(ChronoUnit.SECONDS).toString();

        auditRepository.insert(
                actorUserId,
                studentId,
                action,
                entityType,
                entityId,
                oldJson,
                newJson,
                createdAt
        );
    }

    /**
     * Tra cứu danh sách audit log với các tham số lọc tùy chọn từ query string.
     *
     * @param rawStudentId chuỗi ID học sinh cần lọc (tùy chọn)
     * @param rawAction    chuỗi hành động cần lọc (tùy chọn)
     * @param entityType   tên bảng cần lọc (tùy chọn)
     * @param entityId     ID bản ghi cần lọc (tùy chọn)
     * @return danh sách {@link AuditLogItem} đã parse JSON, không bao giờ {@code null}
     * @throws AppException với mã {@link ErrorCode#VALIDATION_ERROR} nếu tham số lọc sai định dạng
     */
    public List<AuditLogItem> list(
            String rawStudentId,
            String rawAction,
            String entityType,
            String entityId
    ) {
        Long studentId = parsePositiveId(rawStudentId);
        AuditAction action = parseAction(rawAction);
        String normalizedEntityType = validateEntityType(entityType);
        String normalizedEntityId = normalizeOptionalText(entityId);

        return auditRepository.findAll(studentId, action, normalizedEntityType, normalizedEntityId)
                .stream()
                .map(this::toItem)
                .toList();
    }

    /**
     * Chuyển đổi bản ghi thô từ database sang DTO trả về API.
     *
     * @param row bản ghi đọc từ bảng {@code audit_logs}
     * @return đối tượng {@link AuditLogItem} hoàn chỉnh
     */
    private AuditLogItem toItem(AuditRepository.AuditRow row) {
        return new AuditLogItem(
                String.valueOf(row.id()),
                String.valueOf(row.actorUserId()),
                row.studentId() == null ? null : String.valueOf(row.studentId()),
                row.action(),
                row.entityType(),
                row.entityId(),
                parseJsonOrNull(row.oldValue()),
                parseJsonOrNull(row.newValue()),
                row.createdAt()
        );
    }

    /**
     * Parse chuỗi JSON sang {@link JsonNode}, trả {@code null} nếu chuỗi rỗng hoặc {@code null}.
     *
     * @param json chuỗi JSON lưu trong database
     * @return {@link JsonNode} tương ứng hoặc {@code null}
     */
    private JsonNode parseJsonOrNull(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        return objectMapper.readTree(json);
    }

    /**
     * Kiểm tra và chuyển đổi chuỗi ID số nguyên dương tùy chọn.
     *
     * @param raw giá trị query string
     * @return {@code Long} dương hoặc {@code null} nếu không truyền
     * @throws AppException nếu giá trị không phải số nguyên dương
     */
    private Long parsePositiveId(String raw) {
        String trimmed = normalizeOptionalText(raw);
        if (trimmed == null) {
            return null;
        }
        try {
            long value = Long.parseLong(trimmed);
            if (value <= 0) {
                throw new AppException(ErrorCode.VALIDATION_ERROR, "Tham số không hợp lệ.");
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Tham số không hợp lệ.");
        }
    }

    /**
     * Kiểm tra và chuyển đổi chuỗi {@code action} sang enum {@link AuditAction}.
     *
     * @param raw giá trị query string
     * @return {@link AuditAction} hoặc {@code null} nếu không truyền
     * @throws AppException nếu giá trị không thuộc enum hợp lệ
     */
    private AuditAction parseAction(String raw) {
        String trimmed = normalizeOptionalText(raw);
        if (trimmed == null) {
            return null;
        }
        try {
            return AuditAction.valueOf(trimmed);
        } catch (IllegalArgumentException ex) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Tham số không hợp lệ.");
        }
    }

    /**
     * Kiểm tra tên bảng {@code entityType} nằm trong danh sách cho phép.
     *
     * @param raw giá trị query string
     * @return tên bảng hợp lệ hoặc {@code null} nếu không truyền
     * @throws AppException nếu tên bảng không hợp lệ
     */
    private String validateEntityType(String raw) {
        String trimmed = normalizeOptionalText(raw);
        if (trimmed == null) {
            return null;
        }
        if (!ALLOWED_ENTITY_TYPES.contains(trimmed)) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Tham số không hợp lệ.");
        }
        return trimmed;
    }

    /**
     * Chuẩn hóa chuỗi tùy chọn: trả {@code null} nếu {@code null} hoặc toàn khoảng trắng.
     *
     * @param raw chuỗi đầu vào
     * @return chuỗi đã trim hoặc {@code null}
     */
    private String normalizeOptionalText(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}