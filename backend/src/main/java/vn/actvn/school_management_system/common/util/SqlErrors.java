package vn.actvn.school_management_system.common.util;

import org.sqlite.SQLiteErrorCode;
import org.sqlite.SQLiteException;

/**
 * Phân loại lỗi ràng buộc của SQLite <b>bằng mã lỗi</b>, không đọc chuỗi thông
 * báo.
 *
 * <p>Khi INSERT/UPDATE/DELETE vi phạm ràng buộc (trùng email, xóa bản ghi đang
 * được tham chiếu...), SQLite ném {@link SQLiteException} kèm mã lỗi. Spring
 * thường bọc nó trong một exception khác, nên lớp này duyệt chuỗi cause để tìm
 * lại.
 *
 * <p><b>Cách dùng (trong Service):</b>
 * <pre>{@code
 * try {
 *     userRepository.insert(user);
 * } catch (DataAccessException e) {
 *     if (SqlErrors.isUnique(e)) {
 *         throw AppException.conflict("Email đã tồn tại.");
 *     }
 *     throw e; // lỗi khác: để GlobalExceptionHandler xử lý
 * }
 * }</pre>
 *
 * <p><b>Ghi chú:</b>
 * <ul>
 *   <li>Lớp tiện ích: chỉ có hàm {@code static}, không tạo object.</li>
 *   <li>Vi phạm FOREIGN KEY khi <i>INSERT</i> (ID tham chiếu sai) khác nghĩa với
 *       khi <i>DELETE</i> (đang được dùng). Service nên kiểm tra ID trong body
 *       trước để trả 400, chỉ dùng {@link #isForeignKey(Throwable)} cho trường
 *       hợp xóa.</li>
 *   <li>Cách phân loại dựa vào việc sqlite-jdbc trả mã lỗi mở rộng (ví dụ
 *       {@code SQLITE_CONSTRAINT_UNIQUE}). Cần kiểm chứng khi chạy thật; nếu chỉ
 *       nhận được mã chung {@code SQLITE_CONSTRAINT} thì hai hàm {@code is...}
 *       sẽ trả {@code false}.</li>
 * </ul>
 *
 * @see vn.actvn.school_management_system.common.exception.GlobalExceptionHandler
 */
public final class SqlErrors {

    /** Chặn việc tạo object; lớp này chỉ chứa hàm {@code static}. */
    private SqlErrors() {}

    /**
     * Tìm mã lỗi SQLite trong chuỗi cause của một exception.
     *
     * <p>Đi từ chính {@code t} xuống cause sâu nhất; gặp {@link SQLiteException}
     * đầu tiên thì trả mã của nó.
     *
     * @param t exception bất kỳ, có thể là exception đã bị Spring bọc; chấp nhận {@code null}
     * @return mã lỗi SQLite, hoặc {@code null} nếu trong chuỗi không có
     *         {@link SQLiteException}
     */
    public static SQLiteErrorCode codeOf(Throwable t) {
        for (; t != null; t = t.getCause()) {
            if (t instanceof SQLiteException e) return e.getResultCode();
        }
        return null;
    }

    /**
     * Kiểm tra lỗi có phải vi phạm tính duy nhất không.
     *
     * <p>Gồm cả UNIQUE (ví dụ email, mã lớp + năm học) và PRIMARY KEY (ví dụ cặp
     * {@code parent_id, student_id}).
     *
     * @param t exception cần kiểm tra
     * @return {@code true} nếu là vi phạm UNIQUE hoặc PRIMARY KEY
     */
    public static boolean isUnique(Throwable t) {
        SQLiteErrorCode c = codeOf(t);
        return c == SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE || c == SQLiteErrorCode.SQLITE_CONSTRAINT_PRIMARYKEY;
    }

    /**
     * Kiểm tra lỗi có phải vi phạm khóa ngoại không.
     *
     * <p>Chỉ có hiệu lực khi đã bật {@code PRAGMA foreign_keys = ON} (đã cấu
     * hình trong {@code application.yml}).
     *
     * @param t exception cần kiểm tra
     * @return {@code true} nếu là vi phạm FOREIGN KEY
     */
    public static boolean isForeignKey(Throwable t) {
        return codeOf(t) == SQLiteErrorCode.SQLITE_CONSTRAINT_FOREIGNKEY;
    }
}