CREATE TABLE IF NOT EXISTS data_change_audits (
    id BIGINT NOT NULL AUTO_INCREMENT,
    account_id BIGINT,
    teacher_id BIGINT,
    actor_name VARCHAR(150) NOT NULL,
    resource_key VARCHAR(100) NOT NULL,
    table_name VARCHAR(100) NOT NULL,
    row_id VARCHAR(100) NOT NULL,
    action VARCHAR(30) NOT NULL,
    old_values JSON,
    new_values JSON,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_data_change_audits_resource_row (resource_key, row_id),
    KEY idx_data_change_audits_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
