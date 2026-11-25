package vn.actvn.school_management_system.announcement;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.actvn.school_management_system.announcement.dto.AnnouncementItem;
import vn.actvn.school_management_system.announcement.dto.CreateAnnouncementRequest;
import vn.actvn.school_management_system.auth.AuthUser;
import vn.actvn.school_management_system.common.response.ApiResponse;

/**
 * Controller cung cấp API xem và đăng thông báo theo lớp học.
 */
@RestController
@RequestMapping("/api/classes/{classId}/announcements")
public class AnnouncementController {

    /** Service xử lý nghiệp vụ thông báo lớp. */
    private final AnnouncementService announcementService;

    /**
     * Khởi tạo {@code AnnouncementController}.
     *
     * @param announcementService service thông báo lớp
     */
    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    /**
     * Lấy danh sách thông báo của một lớp học.
     *
     * <p>Chỉ cho phép {@code PARENT} có con trong lớp và {@code TEACHER} là GVCN lớp đó.
     *
     * @param actor   người dùng đang đăng nhập
     * @param classId ID lớp học trên đường dẫn
     * @return envelope thành công chứa {@code items}
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('PARENT', 'TEACHER')")
    public ApiResponse<?> list(
            @AuthenticationPrincipal AuthUser actor,
            @PathVariable long classId
    ) {
        List<AnnouncementItem> items = announcementService.listByClass(actor, classId);
        return ApiResponse.ok(Map.of("items", items));
    }

    /**
     * Đăng thông báo mới cho lớp học (chỉ dành cho GVCN của lớp).
     *
     * @param actor   giáo viên đang đăng nhập
     * @param classId ID lớp học trên đường dẫn
     * @param request dữ liệu tiêu đề và nội dung thông báo
     * @return HTTP {@code 201 Created} kèm thông tin thông báo vừa tạo
     */
    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<?>> create(
            @AuthenticationPrincipal AuthUser actor,
            @PathVariable long classId,
            @Valid @RequestBody CreateAnnouncementRequest request
    ) {
        Map<String, String> announcement = announcementService.create(actor, classId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(Map.of("announcement", announcement)));
    }
}