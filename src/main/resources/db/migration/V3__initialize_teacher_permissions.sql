-- Store an explicit initial value for every current permission without overwriting
-- permissions that a director has already configured.

INSERT IGNORE INTO teacher_permissions (teacher_id, permission_type, enabled)
SELECT
    t.id,
    p.permission_type,
    CASE
        WHEN t.position = 'DIRECTOR' THEN b'1'
        WHEN t.position = 'TEACHER'
             AND p.permission_type IN (
                 'STUDENT_CREATE',
                 'CLASS_CREATE',
                 'STUDENT_UPDATE',
                 'CLASS_UPDATE',
                 'STUDENT_SENSITIVE_VIEW'
             )
            THEN b'1'
        ELSE b'0'
    END
FROM teachers t
CROSS JOIN (
    SELECT 'GENERAL_SETTINGS' AS permission_type
    UNION ALL SELECT 'STUDENT_CREATE'
    UNION ALL SELECT 'CREATE_TEACHER'
    UNION ALL SELECT 'CLASS_CREATE'
    UNION ALL SELECT 'STUDENT_UPDATE'
    UNION ALL SELECT 'TEACHER_UPDATE'
    UNION ALL SELECT 'MANAGE_TEACHER_POSITION'
    UNION ALL SELECT 'CLASS_UPDATE'
    UNION ALL SELECT 'STUDENT_SENSITIVE_VIEW'
    UNION ALL SELECT 'TEACHER_SENSITIVE_VIEW'
    UNION ALL SELECT 'MANAGE_ALL_ATTENDANCE'
    UNION ALL SELECT 'MANAGE_ALL_SALARY'
    UNION ALL SELECT 'MANAGE_TUITION'
    UNION ALL SELECT 'REGISTRATION_CODE'
    UNION ALL SELECT 'SYSTEM_UPDATE'
    UNION ALL SELECT 'DATABASE_BACKUP'
    UNION ALL SELECT 'GRADE_PROMOTION'
) p;
