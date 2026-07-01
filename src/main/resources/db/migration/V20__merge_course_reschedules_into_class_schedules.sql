-- Keep class_schedules as the single source of truth for rescheduled courses.
-- Existing course_reschedules rows already point to CANCELLED and RESCHEDULED
-- class_schedules rows; backfill event metadata before removing the redundant table.

SET @course_reschedules_exists = (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'course_reschedules'
);

SET @backfill_cancelled_reschedules = IF(
    @course_reschedules_exists > 0,
    'UPDATE class_schedules cancelled
     JOIN course_reschedules reschedule ON reschedule.cancelled_schedule_id = cancelled.id
     SET cancelled.schedule_type = ''CANCELLED'',
         cancelled.original_schedule_id = reschedule.original_schedule_id,
         cancelled.course_date = DATE(reschedule.original_start_at),
         cancelled.scheduled_start_at = reschedule.original_start_at,
         cancelled.scheduled_end_at = reschedule.original_end_at,
         cancelled.reschedule_reason = COALESCE(cancelled.reschedule_reason, reschedule.reason),
         cancelled.created_by_teacher_id = COALESCE(cancelled.created_by_teacher_id, reschedule.created_by_teacher_id)',
    'SELECT 1'
);

PREPARE backfill_cancelled_reschedules_statement FROM @backfill_cancelled_reschedules;
EXECUTE backfill_cancelled_reschedules_statement;
DEALLOCATE PREPARE backfill_cancelled_reschedules_statement;

SET @backfill_new_reschedules = IF(
    @course_reschedules_exists > 0,
    'UPDATE class_schedules new_schedule
     JOIN course_reschedules reschedule ON reschedule.new_schedule_id = new_schedule.id
     SET new_schedule.schedule_type = ''RESCHEDULED'',
         new_schedule.original_schedule_id = reschedule.original_schedule_id,
         new_schedule.course_date = DATE(reschedule.new_start_at),
         new_schedule.scheduled_start_at = reschedule.new_start_at,
         new_schedule.scheduled_end_at = reschedule.new_end_at,
         new_schedule.reschedule_reason = COALESCE(new_schedule.reschedule_reason, reschedule.reason),
         new_schedule.created_by_teacher_id = COALESCE(new_schedule.created_by_teacher_id, reschedule.created_by_teacher_id)',
    'SELECT 1'
);

PREPARE backfill_new_reschedules_statement FROM @backfill_new_reschedules;
EXECUTE backfill_new_reschedules_statement;
DEALLOCATE PREPARE backfill_new_reschedules_statement;

DROP TABLE IF EXISTS course_reschedules;
