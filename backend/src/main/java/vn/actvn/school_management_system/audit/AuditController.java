package vn.actvn.school_management_system.audit;

import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.actvn.school_management_system.audit.dto.AuditLogItem;
import vn.actvn.school_management_system.common.response.ApiResponse;

/**
 * Controller cung cấp API tra cứu nhật ký kiểm toán dành riêng cho quản trị viên.
 */
@RestController
@RequestMapping("/api/admin/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {

    /** Service xử lý nghiệp vụ audit log. */
    private final AuditService auditService;

    /**
     * Khởi tạo {@code AuditController}.
     *
     * @param auditService service tra cứu audit log
     */
    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * Lấy danh sách nhật ký kiểm toán theo các bộ lọc tùy chọn.
     *
     * @param studentId  ID học sinh cần lọc (tùy chọn)
     * @param action     loại hành động {@code CREATE}, {@code UPDATE}, {@code DELETE} (tùy chọn)
     * @param entityType tên bảng bị tác động (tùy chọn)
     * @param entityId   ID bản ghi bị tác động (tùy chọn)
     * @return envelope thành công chứa danh sách {@code items}
     */
    @GetMapping
    public ApiResponse<?> list(
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId
    ) {
        List<AuditLogItem> items = auditService.list(studentId, action, entityType, entityId);
        return ApiResponse.ok(Map.of("items", items));
    }
}