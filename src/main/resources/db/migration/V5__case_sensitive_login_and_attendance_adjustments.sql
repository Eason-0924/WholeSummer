-- Make login usernames case-sensitive and add salary attendance adjustment fields.

ALTER TABLE teacher_accounts
    MODIFY COLUMN username VARCHAR(50)
        CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL;

ALTER TABLE teacher_attendances
    ADD COLUMN matched_course_id BIGINT NULL,
    ADD COLUMN matched_course_name VARCHAR(1000) NULL,
    ADD COLUMN matched_course_time_text VARCHAR(500) NULL,
    ADD COLUMN manual_remark VARCHAR(255) NULL,
    ADD COLUMN manual_hours DECIMAL(5,2) NULL,
    ADD COLUMN manual_adjusted BIT(1) NOT NULL DEFAULT b'0',
    ADD COLUMN adjusted_by_teacher_id BIGINT NULL,
    ADD COLUMN adjusted_at DATETIME(6) NULL;
