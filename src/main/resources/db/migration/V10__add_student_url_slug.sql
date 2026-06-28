ALTER TABLE students
    ADD COLUMN url_slug VARCHAR(150) NULL,
    ADD UNIQUE KEY uk_students_url_slug (url_slug);
