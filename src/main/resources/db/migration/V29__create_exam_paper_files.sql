CREATE TABLE exam_paper_files (
    id BIGINT NOT NULL AUTO_INCREMENT,
    exam_id BIGINT NOT NULL,
    file_path VARCHAR(1000) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    storage_mode VARCHAR(20) NOT NULL DEFAULT 'COPY',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_exam_paper_files_exam FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE,
    KEY idx_exam_paper_files_exam_id (exam_id)
);

INSERT INTO exam_paper_files (exam_id, file_path, file_name, storage_mode)
SELECT id, paper_file_path, COALESCE(NULLIF(paper_file_name, ''), SUBSTRING_INDEX(paper_file_path, '/', -1)),
       COALESCE(NULLIF(paper_storage_mode, ''), 'COPY')
FROM exams
WHERE paper_file_path IS NOT NULL AND TRIM(paper_file_path) <> '';
