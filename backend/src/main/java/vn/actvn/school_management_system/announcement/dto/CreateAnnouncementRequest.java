package vn.actvn.school_management_system.announcement.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO nhận dữ liệu đầu vào khi tạo mới thông báo cho lớp học.
 *
 * <p>Độ dài chính xác sau khi {@code trim()} (title: 5–50 ký tự, content: 10–1000 ký tự)
 * được kiểm tra bổ sung ở tầng service.
 *
 * @param title   tiêu đề thông báo
 * @param content nội dung thông báo
 */
public record CreateAnnouncementRequest(
        @NotBlank(message = "Tiêu đề không được để trống.")
        String title,

        @NotBlank(message = "Nội dung không được để trống.")
        String content
) {
}