-- ==========================================
-- 0. KICH HOAT FOREIGN KEY
-- ==========================================

PRAGMA foreign_keys = ON;

-- ==========================================
-- 1. USERS
-- ==========================================

CREATE TABLE users (
    id INTEGER PRIMARY KEY,
    full_name TEXT NOT NULL,
    email TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,

    role TEXT NOT NULL
        CHECK (role IN ('PARENT', 'TEACHER', 'ADMIN')),

    is_active INTEGER NOT NULL DEFAULT 1
        CHECK (is_active IN (0, 1)),

    failed_logins INTEGER NOT NULL DEFAULT 0
        CHECK (failed_logins >= 0),

    locked_until TEXT,
    created_at TEXT NOT NULL,

    CHECK (email = lower(email))
);

-- ==========================================
-- 2. CLASSES
-- ==========================================

CREATE TABLE classes (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    class_code TEXT NOT NULL,
    school_year TEXT NOT NULL,
    homeroom_teacher_id INTEGER NOT NULL,

    FOREIGN KEY (homeroom_teacher_id)
        REFERENCES users(id),

    UNIQUE (class_code, school_year),
    UNIQUE (homeroom_teacher_id, school_year)
);

-- ==========================================
-- 3. STUDENTS
-- ==========================================

CREATE TABLE students (
    id INTEGER PRIMARY KEY,
    student_code TEXT NOT NULL UNIQUE,
    full_name TEXT NOT NULL,
    date_of_birth TEXT NOT NULL,
    class_id INTEGER NOT NULL,

    FOREIGN KEY (class_id)
        REFERENCES classes(id)
);

-- ==========================================
-- 4. PARENT_STUDENTS
-- ==========================================

CREATE TABLE parent_students (
    parent_id INTEGER NOT NULL,
    student_id INTEGER NOT NULL UNIQUE,

    relationship TEXT NOT NULL
        CHECK (relationship IN ('PARENT', 'GUARDIAN')),

    PRIMARY KEY (parent_id, student_id),

    FOREIGN KEY (parent_id)
        REFERENCES users(id),

    FOREIGN KEY (student_id)
        REFERENCES students(id)
);

-- ==========================================
-- 5. SUBJECTS
-- ==========================================

CREATE TABLE subjects (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    subject_code TEXT UNIQUE NOT NULL
);

-- ==========================================
-- 6. TEACHING_ASSIGNMENTS
-- ==========================================

CREATE TABLE teaching_assignments (
    id INTEGER PRIMARY KEY,
    teacher_id INTEGER NOT NULL,
    class_id INTEGER NOT NULL,
    subject_id INTEGER NOT NULL,

    FOREIGN KEY (teacher_id)
        REFERENCES users(id),

    FOREIGN KEY (class_id)
        REFERENCES classes(id),

    FOREIGN KEY (subject_id)
        REFERENCES subjects(id),

    UNIQUE (class_id, subject_id)
);

-- ==========================================
-- 7. TIMETABLE
-- ==========================================

CREATE TABLE timetable (
    id INTEGER PRIMARY KEY,
    class_id INTEGER NOT NULL,
    subject_id INTEGER NOT NULL,

    weekday INTEGER NOT NULL
        CHECK (weekday BETWEEN 2 AND 6),

    period INTEGER NOT NULL
        CHECK (period BETWEEN 1 AND 12),

    room TEXT,
    school_year TEXT NOT NULL,

    term TEXT NOT NULL
        CHECK (term IN ('HK1', 'HK2')),

    FOREIGN KEY (class_id, subject_id)
        REFERENCES teaching_assignments(class_id, subject_id),

    UNIQUE (class_id, school_year, term, weekday, period)
);

-- ==========================================
-- 8. GRADES
-- ==========================================

CREATE TABLE grades (
    id INTEGER PRIMARY KEY,
    student_id INTEGER NOT NULL,
    subject_id INTEGER NOT NULL,
    school_year TEXT NOT NULL,

    term TEXT NOT NULL
        CHECK (term IN ('HK1', 'HK2')),

    score_type TEXT NOT NULL
        CHECK (score_type IN ('TX1', 'TX2', 'GK', 'CK')),

    score REAL NOT NULL
        CHECK (score BETWEEN 0 AND 10),

    updated_by INTEGER NOT NULL,
    updated_at TEXT NOT NULL,

    FOREIGN KEY (student_id)
        REFERENCES students(id),

    FOREIGN KEY (subject_id)
        REFERENCES subjects(id),

    FOREIGN KEY (updated_by)
        REFERENCES users(id),

    UNIQUE (
        student_id,
        subject_id,
        school_year,
        term,
        score_type
    )
);

-- ==========================================
-- 9. ATTENDANCE
-- ==========================================

CREATE TABLE attendance (
    id INTEGER PRIMARY KEY,
    student_id INTEGER NOT NULL,
    date TEXT NOT NULL,

    status TEXT NOT NULL
        CHECK (
            status IN (
                'PRESENT',
                'EXCUSED',
                'UNEXCUSED',
                'LATE'
            )
        ),

    recorded_by INTEGER NOT NULL,
    created_at TEXT NOT NULL,

    FOREIGN KEY (student_id)
        REFERENCES students(id),

    FOREIGN KEY (recorded_by)
        REFERENCES users(id),

    UNIQUE (student_id, date)
);

-- ==========================================
-- 10. LEAVE_REQUESTS
-- ==========================================

CREATE TABLE leave_requests (
    id INTEGER PRIMARY KEY,
    student_id INTEGER NOT NULL,
    parent_id INTEGER NOT NULL,

    from_date TEXT NOT NULL,
    to_date TEXT NOT NULL,

    reason TEXT NOT NULL,

    status TEXT NOT NULL DEFAULT 'PENDING'
        CHECK (
            status IN ('PENDING', 'APPROVED', 'REJECTED')
        ),

    reviewed_by INTEGER,
    reviewed_at TEXT,
    created_at TEXT NOT NULL,

    CHECK (from_date <= to_date),
    CHECK ((status = 'PENDING') = (reviewed_by IS NULL)),
    CHECK ((reviewed_by IS NULL) = (reviewed_at IS NULL)),

    FOREIGN KEY (parent_id, student_id)
        REFERENCES parent_students(parent_id, student_id),

    FOREIGN KEY (reviewed_by)
        REFERENCES users(id)
);

-- ==========================================
-- 11. ANNOUNCEMENTS
-- ==========================================

CREATE TABLE announcements (
    id INTEGER PRIMARY KEY,
    class_id INTEGER NOT NULL,
    author_id INTEGER NOT NULL,
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    created_at TEXT NOT NULL,

    FOREIGN KEY (class_id)
        REFERENCES classes(id),

    FOREIGN KEY (author_id)
        REFERENCES users(id)
);

-- ==========================================
-- 12. AUDIT_LOGS
-- ==========================================

CREATE TABLE audit_logs (
    id INTEGER PRIMARY KEY,
    student_id INTEGER,
    actor_user_id INTEGER NOT NULL,
    action TEXT NOT NULL,
    entity_type TEXT NOT NULL,
    entity_id TEXT NOT NULL,
    old_value TEXT,
    new_value TEXT,
    created_at TEXT NOT NULL,

    CHECK (action IN ('CREATE', 'UPDATE', 'DELETE')),

    FOREIGN KEY (student_id) REFERENCES students(id),
    FOREIGN KEY (actor_user_id) REFERENCES users(id)
);

-- ==========================================
-- END OF DATABASE SCHEMA
-- ==========================================

CREATE TRIGGER audit_no_update BEFORE UPDATE ON audit_logs
BEGIN SELECT RAISE(ABORT, 'audit_logs is append-only'); END;
CREATE TRIGGER audit_no_delete BEFORE DELETE ON audit_logs
BEGIN SELECT RAISE(ABORT, 'audit_logs is append-only'); END;
CREATE INDEX idx_audit_student ON audit_logs(student_id);
