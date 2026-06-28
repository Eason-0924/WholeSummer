CREATE TABLE IF NOT EXISTS teacher_leaves (
    id BIGINT NOT NULL AUTO_INCREMENT,
    teacher_id BIGINT NOT NULL,
    leave_date DATE NOT NULL,
    course_schedule_id BIGINT NULL,
    reason VARCHAR(255) NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'APPROVED',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_teacher_leave_schedule_date (teacher_id, leave_date, course_schedule_id),
    KEY idx_teacher_leaves_teacher_date (teacher_id, leave_date),
    KEY idx_teacher_leaves_course_schedule (course_schedule_id),
    CONSTRAINT fk_teacher_leaves_teacher FOREIGN KEY (teacher_id) REFERENCES teachers (id),
    CONSTRAINT fk_teacher_leaves_course_schedule FOREIGN KEY (course_schedule_id) REFERENCES class_schedules (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
