-- Remove legacy string fields that have been replaced by relations or are no longer recorded.

SET @drop_classes_teacher = (
    SELECT IF(
        COUNT(*) > 0,
        'ALTER TABLE classes DROP COLUMN teacher',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'classes'
      AND column_name = 'teacher'
);

PREPARE drop_classes_teacher_statement FROM @drop_classes_teacher;
EXECUTE drop_classes_teacher_statement;
DEALLOCATE PREPARE drop_classes_teacher_statement;

SET @drop_subjects_teacher = (
    SELECT IF(
        COUNT(*) > 0,
        'ALTER TABLE subjects DROP COLUMN teacher',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'subjects'
      AND column_name = 'teacher'
);

PREPARE drop_subjects_teacher_statement FROM @drop_subjects_teacher;
EXECUTE drop_subjects_teacher_statement;
DEALLOCATE PREPARE drop_subjects_teacher_statement;

SET @drop_operation_logs_actor_username = (
    SELECT IF(
        COUNT(*) > 0,
        'ALTER TABLE operation_logs DROP COLUMN actor_username',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'operation_logs'
      AND column_name = 'actor_username'
);

PREPARE drop_operation_logs_actor_username_statement
    FROM @drop_operation_logs_actor_username;
EXECUTE drop_operation_logs_actor_username_statement;
DEALLOCATE PREPARE drop_operation_logs_actor_username_statement;
