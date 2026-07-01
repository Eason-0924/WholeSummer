ALTER TABLE teachers
    ADD COLUMN card_id VARCHAR(100) NULL COMMENT '教師綁定的卡片識別碼',
    ADD COLUMN card_bound_at DATETIME(6) NULL COMMENT '卡片綁定時間',
    ADD COLUMN card_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '卡片狀態',
    ADD CONSTRAINT uk_teachers_card_id UNIQUE (card_id);

ALTER TABLE teacher_attendances
    ADD COLUMN check_method VARCHAR(30) NOT NULL DEFAULT 'MANUAL' COMMENT '出勤方式',
    ADD COLUMN device_name VARCHAR(100) NULL COMMENT '刷卡設備名稱',
    ADD COLUMN card_id VARCHAR(100) NULL COMMENT '刷卡時使用的卡片識別碼';
