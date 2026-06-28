ALTER TABLE teachers
    ADD COLUMN url_slug VARCHAR(150) NULL,
    ADD UNIQUE KEY uk_teachers_url_slug (url_slug);

ALTER TABLE subjects
    ADD COLUMN url_slug VARCHAR(150) NULL,
    ADD UNIQUE KEY uk_subjects_url_slug (url_slug);

ALTER TABLE classes
    ADD COLUMN url_slug VARCHAR(150) NULL,
    ADD UNIQUE KEY uk_classes_url_slug (url_slug);
