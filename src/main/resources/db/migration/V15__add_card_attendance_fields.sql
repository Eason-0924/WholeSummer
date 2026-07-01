ALTER TABLE students
    ADD COLUMN card_id VARCHAR(100) NULL COMMENT '學生綁定的卡片識別碼',
    ADD COLUMN card_bound_at DATETIME(6) NULL COMMENT '卡片綁定時間',
    ADD COLUMN card_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '卡片狀態',
    ADD CONSTRAINT uk_students_card_id UNIQUE (card_id);

ALTER TABLE student_attendances
    ADD COLUMN check_method VARCHAR(30) NOT NULL DEFAULT 'MANUAL' COMMENT '點名方式',
    ADD COLUMN device_name VARCHAR(100) NULL COMMENT '刷卡設備名稱',
    ADD COLUMN card_id VARCHAR(100) NULL COMMENT '刷卡時使用的卡片識別碼',
    ADD COLUMN check_in_time DATETIME(6) NULL COMMENT '點名時間';
