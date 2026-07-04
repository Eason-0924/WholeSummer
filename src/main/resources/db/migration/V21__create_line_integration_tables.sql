CREATE TABLE IF NOT EXISTS line_bind_codes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    parent_name VARCHAR(100),
    code VARCHAR(20) NOT NULL,
    used BIT(1) NOT NULL DEFAULT b'0',
    expired_at DATETIME(6) NOT NULL,
    used_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_line_bind_codes_code (code),
    KEY idx_line_bind_codes_student (student_id),
    CONSTRAINT fk_line_bind_codes_student
        FOREIGN KEY (student_id) REFERENCES students (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS parent_line_bindings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    parent_name VARCHAR(100),
    relation VARCHAR(30),
    line_user_id VARCHAR(100) NOT NULL,
    line_display_name VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'BOUND',
    bound_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_parent_line_binding_student_user
        UNIQUE (student_id, line_user_id),
    KEY idx_parent_line_bindings_line_user (line_user_id),
    KEY idx_parent_line_bindings_student_status (student_id, status),
    CONSTRAINT fk_parent_line_bindings_student
        FOREIGN KEY (student_id) REFERENCES students (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS line_notification_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    student_id BIGINT,
    line_user_id VARCHAR(100),
    notification_type VARCHAR(50) NOT NULL,
    reference_type VARCHAR(50),
    reference_id BIGINT,
    title VARCHAR(100),
    content TEXT,
    status VARCHAR(20) NOT NULL,
    provider_message_id VARCHAR(200),
    error_message TEXT,
    sent_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_line_notification_reference
        UNIQUE (student_id, notification_type, reference_type, reference_id),
    KEY idx_line_notification_logs_student (student_id),
    KEY idx_line_notification_logs_line_user (line_user_id),
    KEY idx_line_notification_logs_status (status),
    CONSTRAINT fk_line_notification_logs_student
        FOREIGN KEY (student_id) REFERENCES students (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
