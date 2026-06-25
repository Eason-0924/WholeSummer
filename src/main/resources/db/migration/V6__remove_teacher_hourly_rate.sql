-- Hourly rates are stored per teacher and month in teacher_monthly_salaries.

SET @drop_teacher_hourly_rate = (
    SELECT IF(
        COUNT(*) > 0,
        'ALTER TABLE teachers DROP COLUMN hourly_rate',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'teachers'
      AND column_name = 'hourly_rate'
);

PREPARE drop_teacher_hourly_rate_statement FROM @drop_teacher_hourly_rate;
EXECUTE drop_teacher_hourly_rate_statement;
DEALLOCATE PREPARE drop_teacher_hourly_rate_statement;
