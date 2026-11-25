package vn.actvn.school_management_system.announcement;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.actvn.school_management_system.announcement.dto.AnnouncementItem;
import vn.actvn.school_management_system.announcement.dto.CreateAnnouncementRequest;
import vn.actvn.school_management_system.audit.AuditAction;
import vn.actvn.school_management_system.audit.AuditService;
import vn.actvn.school_management_system.auth.AuthUser;
import vn.actvn.school_management_system.common.exception.AppException;
import vn.actvn.school_management_system.common.exception.ErrorCode;

/**
 * Service xử lý nghiệp vụ xem và đăng thông báo lớp học.
 */
@Service
public class AnnouncementService {

    /** Độ dài tối thiểu của tiêu đề thông báo sau khi trim. */
    private static final int MIN_TITLE_LENGTH = 5;

    /** Độ dài tối đa của tiêu đề thông báo sau khi trim. */
    private static final int MAX_TITLE_LENGTH = 50;

    /** Độ dài tối thiểu của nội dung thông báo sau khi trim. */
    private static final int MIN_CONTENT_LENGTH = 10;

    /** Độ dài tối đa của nội dung thông báo sau khi trim. */
    private static final int MAX_CONTENT_LENGTH = 1000;

    /** Repository thao tác bảng {@code announcements}. */
    private final AnnouncementRepository announcementRepository;

    /** Service ghi nhật ký kiểm toán. */
    private final AuditService auditService;

    /** Đồng hồ hệ thống để lấy thời điểm tạo chuẩn UTC. */
    private final Clock clock;

    /**
     * Khởi tạo {@code AnnouncementService}.
     *
     * @param announcementRepository repository thông báo
     * @param auditService           service ghi audit log
     * @param clock                  đồng hồ hệ thống
     */
    public AnnouncementService(
            AnnouncementRepository announcementRepository,
            AuditService auditService,
            Clock clock
    ) {
        this.announcementRepository = announcementRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    /**
     * Lấy danh sách thông báo của một lớp học nếu người dùng có quyền truy cập.
     *
     * <p>Phụ huynh phải có con học tại lớp; giáo viên phải là GVCN của lớp.
     * Nếu không thỏa mãn hoặc lớp không tồn tại, trả về {@code 404 NOT_FOUND} để chống IDOR.
     *
     * @param actor   người dùng đang đăng nhập
     * @param classId ID lớp học
     * @return danh sách thông báo của lớp
     * @throws AppException {@link ErrorCode#NOT_FOUND} nếu không có quyền hoặc lớp không tồn tại
     */
    public List<AnnouncementItem> listByClass(AuthUser actor, long classId) {
        boolean allowed = switch (actor.role()) {
            case "PARENT" -> announcementRepository.isParentOfClass(actor.id(), classId);
            case "TEACHER" -> announcementRepository.isHomeroomTeacher(actor.id(), classId);
            default -> false;
        };

        if (!allowed) {
            throw new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy tài nguyên.");
        }

        return announcementRepository.findByClassId(classId);
    }

    /**
     * Tạo mới một thông báo cho lớp học (chỉ dành cho GVCN của lớp đó) và ghi audit log.
     *
     * @param actor   giáo viên đang đăng nhập
     * @param classId ID lớp học
     * @param request dữ liệu tiêu đề và nội dung thông báo
     * @return map chứa thông tin tóm tắt của thông báo vừa tạo ({@code id}, {@code classId}, {@code title})
     * @throws AppException {@link ErrorCode#NOT_FOUND} nếu không phải GVCN của lớp;
     *                      {@link ErrorCode#VALIDATION_ERROR} nếu tiêu đề/nội dung sai độ dài
     */
    @Transactional
    public Map<String, String> create(AuthUser actor, long classId, CreateAnnouncementRequest request) {
        if (!announcementRepository.isHomeroomTeacher(actor.id(), classId)) {
            throw new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy tài nguyên.");
        }

        String title = request.title().trim();
        String content = request.content().trim();

        if (title.length() < MIN_TITLE_LENGTH || title.length() > MAX_TITLE_LENGTH) {
            throw new AppException(
                    ErrorCode.VALIDATION_ERROR,
                    "Tiêu đề phải từ 5 đến 50 ký tự."
            );
        }
        if (content.length() < MIN_CONTENT_LENGTH || content.length() > MAX_CONTENT_LENGTH) {
            throw new AppException(
                    ErrorCode.VALIDATION_ERROR,
                    "Nội dung phải từ 10 đến 1000 ký tự."
            );
        }

        String createdAt = Instant.now(clock).truncatedTo(ChronoUnit.SECONDS).toString();
        long id = announcementRepository.insert(classId, actor.id(), title, content, createdAt);

        auditService.record(
                actor.id(),
                null,
                AuditAction.CREATE,
                "announcements",
                String.valueOf(id),
                null,
                Map.of("classId", classId, "title", title)
        );

        return Map.of(
                "id", String.valueOf(id),
                "classId", String.valueOf(classId),
                "title", title
        );
    }
}