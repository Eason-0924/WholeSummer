-- A notification can be delivered to more than one LINE user. Keep one
-- idempotency key per student/notification/occurrence/recipient instead of
-- collapsing all recipients into one aggregate row.
ALTER TABLE line_notification_logs
    DROP INDEX uk_line_notification_reference,
    ADD UNIQUE KEY uk_line_notification_reference_recipient
        (student_id, notification_type, reference_type, reference_id, line_user_id);
