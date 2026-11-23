package vn.actvn.school_management_system.common.util;

import org.sqlite.SQLiteErrorCode;
import org.sqlite.SQLiteException;

/** Đọc mã lỗi SQLite trong chuỗi cause; không đoán theo thông điệp. */
public final class SqlErrors {
    private SqlErrors() {}

    public static SQLiteErrorCode codeOf(Throwable t) {
        for (; t != null; t = t.getCause()) {
            if (t instanceof SQLiteException e) return e.getResultCode();
        }
        return null;
    }
    public static boolean isUnique(Throwable t) {
        SQLiteErrorCode c = codeOf(t);
        return c == SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE || c == SQLiteErrorCode.SQLITE_CONSTRAINT_PRIMARYKEY;
    }
    public static boolean isForeignKey(Throwable t) {
        return codeOf(t) == SQLiteErrorCode.SQLITE_CONSTRAINT_FOREIGNKEY;
    }
}