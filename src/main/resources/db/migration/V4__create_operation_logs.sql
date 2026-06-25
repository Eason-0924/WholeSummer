-- Operation logging was introduced together with Flyway adoption.
-- Existing databases may not have this table because V1 is skipped after baseline.

CREATE TABLE IF NOT EXISTS operation_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    account_id BIGINT,
    teacher_id BIGINT,
    actor_name VARCHAR(150) NOT NULL,
    action VARCHAR(200) NOT NULL,
    request_method VARCHAR(10) NOT NULL,
    request_path VARCHAR(500) NOT NULL,
    result VARCHAR(30) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
