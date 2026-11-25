package vn.actvn.school_management_system.audit.dto;

import tools.jackson.databind.JsonNode;

/**
 * DTO đại diện cho một bản ghi nhật ký kiểm toán trả về qua API.
 *
 * <p>Mọi ID được serialize dạng {@code String} để nhất quán hợp đồng API.
 * Các trường {@code oldValue} và {@code newValue} giữ nguyên cấu trúc JSON object
 * (hoặc {@code null} tùy theo hành động CREATE/DELETE).
 *
 * @param id         mã bản ghi audit log
 * @param actorId    mã tài khoản người thực hiện thao tác
 * @param studentId  mã học sinh liên quan (có thể {@code null} với thao tác không gắn học sinh)
 * @param action     hành động thực hiện ({@code CREATE}, {@code UPDATE}, {@code DELETE})
 * @param entityType tên bảng bị tác động (ví dụ: {@code grades}, {@code parent_students})
 * @param entityId   mã định danh của bản ghi bị tác động
 * @param oldValue   dữ liệu trước khi thay đổi dưới dạng JSON (null nếu là {@code CREATE})
 * @param newValue   dữ liệu sau khi thay đổi dưới dạng JSON (null nếu là {@code DELETE})
 * @param createdAt  thời điểm ghi log theo chuẩn ISO 8601 UTC
 */
public record AuditLogItem(
        String id,
        String actorId,
        String studentId,
        String action,
        String entityType,
        String entityId,
        JsonNode oldValue,
        JsonNode newValue,
        String createdAt
) {
}