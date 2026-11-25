package vn.actvn.school_management_system.announcement.dto;

/**
 * DTO đại diện cho một thông báo lớp học trả về trong danh sách.
 *
 * @param id        mã thông báo (dạng chuỗi theo quy ước API)
 * @param title     tiêu đề thông báo
 * @param content   nội dung chi tiết thông báo
 * @param createdAt thời điểm tạo theo chuẩn ISO 8601 UTC
 */
public record AnnouncementItem(
        String id,
        String title,
        String content,
        String createdAt
) {
}