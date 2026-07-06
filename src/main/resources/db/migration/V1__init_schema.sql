-- Complete schema for a new WholeSummer database.
-- Existing non-empty databases are baselined at version 1 and do not execute this file.

CREATE TABLE IF NOT EXISTS teachers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    nickname VARCHAR(100),
    phone VARCHAR(30),
    email VARCHAR(150),
    hire_date DATE,
    hourly_rate INT NOT NULL DEFAULT 0,
    position ENUM('DIRECTOR','TEACHER','TUTOR') NOT NULL DEFAULT 'TEACHER',
    status ENUM('ACTIVE','LEFT') NOT NULL,
    note VARCHAR(1000),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS students (
    id BIGINT NOT NULL AUTO_INCREMENT,
    chinese_name VARCHAR(100) NOT NULL,
    english_name VARCHAR(100),
    gender VARCHAR(20),
    birthday DATE,
    school VARCHAR(100),
    grade VARCHAR(50),
    phone VARCHAR(30),
    note VARCHAR(1000),
    active BIT(1) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS subjects (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(1000),
    grade_levels VARCHAR(200),
    active BIT(1) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS system_settings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    setting_key VARCHAR(100) NOT NULL,
    setting_value VARCHAR(1000) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_system_setting_key UNIQUE (setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS backup_records (
    id BIGINT NOT NULL AUTO_INCREMENT,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(1000) NOT NULL,
    backup_time DATETIME(6) NOT NULL,
    status ENUM('FAILED','SUCCESS') NOT NULL,
    note VARCHAR(1000),
    file_size BIGINT NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS operation_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    account_id BIGINT,
    teacher_id BIGINT,
    actor_name VARCHAR(150) NOT NULL,
    action VARCHAR(200) NOT NULL,
    request_method VARCHAR(10) NOT NULL,
    request_path VARCHAR(500) NOT NULL,
    result VARCHAR(30) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS classes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    grade VARCHAR(50),
    subject_id BIGINT,
    class_type VARCHAR(100),
    teacher_id BIGINT,
    description VARCHAR(1000),
    active BIT(1) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_classes_subject (subject_id),
    KEY idx_classes_teacher (teacher_id),
    CONSTRAINT fk_classes_subject FOREIGN KEY (subject_id) REFERENCES subjects (id),
    CONSTRAINT fk_classes_teacher FOREIGN KEY (teacher_id) REFERENCES teachers (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS class_schedules (
    id BIGINT NOT NULL AUTO_INCREMENT,
    class_id BIGINT NOT NULL,
    weekday VARCHAR(20) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    weekly_exam BIT(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (id),
    KEY idx_class_schedules_class (class_id),
    CONSTRAINT fk_class_schedules_class FOREIGN KEY (class_id) REFERENCES classes (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS class_students (
    id BIGINT NOT NULL AUTO_INCREMENT,
    class_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    joined_at DATETIME(6) NOT NULL,
    active BIT(1) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_class_student UNIQUE (class_id, student_id),
    KEY idx_class_students_student (student_id),
    CONSTRAINT fk_class_students_class FOREIGN KEY (class_id) REFERENCES classes (id),
    CONSTRAINT fk_class_students_student FOREIGN KEY (student_id) REFERENCES students (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS subject_teachers (
    subject_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    PRIMARY KEY (subject_id, teacher_id),
    KEY idx_subject_teachers_teacher (teacher_id),
    CONSTRAINT fk_subject_teachers_subject FOREIGN KEY (subject_id) REFERENCES subjects (id),
    CONSTRAINT fk_subject_teachers_teacher FOREIGN KEY (teacher_id) REFERENCES teachers (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS exams (
    id BIGINT NOT NULL AUTO_INCREMENT,
    class_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    exam_date DATE NOT NULL,
    full_score INT NOT NULL,
    description VARCHAR(1000),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_exams_class (class_id),
    KEY idx_exams_subject (subject_id),
    CONSTRAINT fk_exams_class FOREIGN KEY (class_id) REFERENCES classes (id),
    CONSTRAINT fk_exams_subject FOREIGN KEY (subject_id) REFERENCES subjects (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS scores (
    id BIGINT NOT NULL AUTO_INCREMENT,
    exam_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    score INT NOT NULL,
    comment VARCHAR(1000),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_exam_student UNIQUE (exam_id, student_id),
    KEY idx_scores_student (student_id),
    CONSTRAINT fk_scores_exam FOREIGN KEY (exam_id) REFERENCES exams (id),
    CONSTRAINT fk_scores_student FOREIGN KEY (student_id) REFERENCES students (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS homeworks (
    id BIGINT NOT NULL AUTO_INCREMENT,
    class_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    assigned_date DATE NOT NULL,
    due_date DATE NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_homeworks_class (class_id),
    KEY idx_homeworks_subject (subject_id),
    CONSTRAINT fk_homeworks_class FOREIGN KEY (class_id) REFERENCES classes (id),
    CONSTRAINT fk_homeworks_subject FOREIGN KEY (subject_id) REFERENCES subjects (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS homework_records (
    id BIGINT NOT NULL AUTO_INCREMENT,
    homework_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    status ENUM('EXCUSED','LATE','NOT_SUBMITTED','SUBMITTED') NOT NULL,
    submitted_at DATETIME(6),
    teacher_comment TEXT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_homework_student UNIQUE (homework_id, student_id),
    KEY idx_homework_records_student (student_id),
    CONSTRAINT fk_homework_records_homework FOREIGN KEY (homework_id) REFERENCES homeworks (id),
    CONSTRAINT fk_homework_records_student FOREIGN KEY (student_id) REFERENCES students (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS student_attendances (
    id BIGINT NOT NULL AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    attendance_date DATE NOT NULL,
    status ENUM('ABSENT','LATE','LEAVE','PRESENT') NOT NULL,
    note VARCHAR(1000),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_student_attendance UNIQUE (student_id, class_id, attendance_date),
    KEY idx_student_attendances_class (class_id),
    CONSTRAINT fk_student_attendances_student FOREIGN KEY (student_id) REFERENCES students (id),
    CONSTRAINT fk_student_attendances_class FOREIGN KEY (class_id) REFERENCES classes (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS teacher_accounts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    teacher_id BIGINT NOT NULL,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(128) NOT NULL,
    password_salt VARCHAR(64) NOT NULL,
    last_login_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_teacher_account_username UNIQUE (username),
    CONSTRAINT uk_teacher_account_teacher UNIQUE (teacher_id),
    CONSTRAINT fk_teacher_accounts_teacher FOREIGN KEY (teacher_id) REFERENCES teachers (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS teacher_attendances (
    id BIGINT NOT NULL AUTO_INCREMENT,
    teacher_id BIGINT NOT NULL,
    attendance_date DATE NOT NULL,
    clock_in_time TIME,
    clock_out_time TIME,
    work_minutes BIGINT,
    scheduled_time_text VARCHAR(500),
    status ENUM('ABSENT','LATE','LEAVE','WORKING') NOT NULL,
    note VARCHAR(1000),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_teacher_attendance UNIQUE (teacher_id, attendance_date),
    CONSTRAINT fk_teacher_attendances_teacher FOREIGN KEY (teacher_id) REFERENCES teachers (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS teacher_monthly_salaries (
    id BIGINT NOT NULL AUTO_INCREMENT,
    teacher_id BIGINT NOT NULL,
    salary_year INT NOT NULL,
    salary_month INT NOT NULL,
    hourly_rate INT NOT NULL DEFAULT 0,
    work_minutes BIGINT NOT NULL DEFAULT 0,
    total_salary DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_teacher_monthly_salary_period
        UNIQUE (teacher_id, salary_year, salary_month),
    CONSTRAINT fk_teacher_monthly_salaries_teacher
        FOREIGN KEY (teacher_id) REFERENCES teachers (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS teacher_permissions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    teacher_id BIGINT NOT NULL,
    permission_type ENUM(
        'CLASS_CREATE','CLASS_UPDATE','CREATE_TEACHER','DATABASE_BACKUP',
        'GENERAL_SETTINGS','GRADE_PROMOTION','MANAGE_ALL_ATTENDANCE',
        'MANAGE_ALL_SALARY','MANAGE_TEACHER_POSITION','MANAGE_TUITION',
        'REGISTRATION_CODE','STUDENT_CREATE','STUDENT_SENSITIVE_VIEW',
        'STUDENT_UPDATE','SYSTEM_UPDATE','TEACHER_SENSITIVE_VIEW','TEACHER_UPDATE'
    ) NOT NULL,
    enabled BIT(1) NOT NULL DEFAULT b'1',
    PRIMARY KEY (id),
    CONSTRAINT uk_teacher_permission UNIQUE (teacher_id, permission_type),
    CONSTRAINT fk_teacher_permissions_teacher FOREIGN KEY (teacher_id) REFERENCES teachers (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tuition_records (
    id BIGINT NOT NULL AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    amount_due INT NOT NULL,
    amount_paid INT NOT NULL DEFAULT 0,
    due_date DATE NOT NULL,
    paid_date DATE,
    status ENUM('PAID','PARTIALLY_PAID','UNPAID') NOT NULL,
    note VARCHAR(2000),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_tuition_records_student (student_id),
    CONSTRAINT fk_tuition_records_student FOREIGN KEY (student_id) REFERENCES students (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS bug_reports (
    id BIGINT NOT NULL AUTO_INCREMENT,
    teacher_id BIGINT NOT NULL,
    type ENUM('BUG','FEATURE','OTHER','USAGE') NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(5000) NOT NULL,
    contact_email VARCHAR(200),
    page_url VARCHAR(1000),
    application_version VARCHAR(50),
    system_information VARCHAR(2000),
    status ENUM('FAILED','PENDING','SENT') NOT NULL,
    provider_message_id VARCHAR(200),
    error_message VARCHAR(2000),
    sent_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_bug_reports_teacher (teacher_id),
    CONSTRAINT fk_bug_reports_teacher FOREIGN KEY (teacher_id) REFERENCES teachers (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
