ALTER TABLE class_schedules
    ADD COLUMN schedule_type VARCHAR(50) NOT NULL DEFAULT 'NORMAL',
    ADD COLUMN original_schedule_id BIGINT NULL,
    ADD COLUMN course_date DATE NULL,
    ADD COLUMN scheduled_start_at DATETIME(6) NULL,
    ADD COLUMN scheduled_end_at DATETIME(6) NULL,
    ADD COLUMN reschedule_reason VARCHAR(255) NULL,
    ADD COLUMN created_by_teacher_id BIGINT NULL,
    ADD KEY idx_class_schedules_type_start (schedule_type, scheduled_start_at),
    ADD KEY idx_class_schedules_teacher_start (scheduled_start_at),
    ADD KEY idx_class_schedules_original (original_schedule_id),
    ADD CONSTRAINT fk_class_schedules_original FOREIGN KEY (original_schedule_id) REFERENCES class_schedules (id),
    ADD CONSTRAINT fk_class_schedules_created_by FOREIGN KEY (created_by_teacher_id) REFERENCES teachers (id);

CREATE TABLE IF NOT EXISTS course_reschedules (
    id BIGINT NOT NULL AUTO_INCREMENT,
    original_schedule_id BIGINT NOT NULL,
    cancelled_schedule_id BIGINT NOT NULL,
    new_schedule_id BIGINT NOT NULL,
    original_start_at DATETIME(6) NOT NULL,
    original_end_at DATETIME(6) NOT NULL,
    new_start_at DATETIME(6) NOT NULL,
    new_end_at DATETIME(6) NOT NULL,
    reason VARCHAR(255) NULL,
    created_by_teacher_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_course_reschedules_original (original_schedule_id),
    KEY idx_course_reschedules_new_schedule (new_schedule_id),
    CONSTRAINT fk_course_reschedules_original FOREIGN KEY (original_schedule_id) REFERENCES class_schedules (id),
    CONSTRAINT fk_course_reschedules_cancelled FOREIGN KEY (cancelled_schedule_id) REFERENCES class_schedules (id),
    CONSTRAINT fk_course_reschedules_new FOREIGN KEY (new_schedule_id) REFERENCES class_schedules (id),
    CONSTRAINT fk_course_reschedules_created_by FOREIGN KEY (created_by_teacher_id) REFERENCES teachers (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
