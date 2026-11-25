package vn.actvn.school_management_system.audit;

/**
 * Các loại hành động được ghi nhận trong bảng {@code audit_logs}.
 *
 * <p>Dùng chung giữa lớp service khi ghi nhật ký và tầng API khi lọc dữ liệu
 * theo tham số {@code action}.
 */
public enum AuditAction {

    /** Hành động tạo mới bản ghi. */
    CREATE,

    /** Hành động cập nhật bản ghi hiện có. */
    UPDATE,

    /** Hành động xóa bản ghi. */
    DELETE
}