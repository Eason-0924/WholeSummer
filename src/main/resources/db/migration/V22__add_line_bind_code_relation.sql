ALTER TABLE line_bind_codes
    ADD COLUMN relation VARCHAR(30) AFTER parent_name;
