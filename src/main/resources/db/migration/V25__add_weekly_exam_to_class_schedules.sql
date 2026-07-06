ALTER TABLE class_schedules
    ADD COLUMN weekly_exam BIT(1) NOT NULL DEFAULT b'0';
