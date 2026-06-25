-- Align databases created before Flyway with the current teacher permission model.
-- The existing table name and permission_type column are intentionally preserved.

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

SET @enabled_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'teacher_permissions'
      AND column_name = 'enabled'
);

SET @add_enabled_column_sql = IF(
    @enabled_column_exists = 0,
    'ALTER TABLE teacher_permissions ADD COLUMN enabled BIT(1) NOT NULL DEFAULT b''1''',
    'SELECT 1'
);

PREPARE add_enabled_column_statement FROM @add_enabled_column_sql;
EXECUTE add_enabled_column_statement;
DEALLOCATE PREPARE add_enabled_column_statement;

ALTER TABLE teacher_permissions
    MODIFY COLUMN permission_type ENUM(
        'CLASS_CREATE','CLASS_UPDATE','CREATE_TEACHER','DATABASE_BACKUP',
        'GENERAL_SETTINGS','GRADE_PROMOTION','MANAGE_ALL_ATTENDANCE',
        'MANAGE_ALL_SALARY','MANAGE_TEACHER_POSITION','MANAGE_TUITION',
        'REGISTRATION_CODE','STUDENT_CREATE','STUDENT_SENSITIVE_VIEW',
        'STUDENT_UPDATE','SYSTEM_UPDATE','TEACHER_SENSITIVE_VIEW','TEACHER_UPDATE'
    ) NOT NULL;

UPDATE teacher_permissions SET enabled = b'1' WHERE enabled IS NULL;
