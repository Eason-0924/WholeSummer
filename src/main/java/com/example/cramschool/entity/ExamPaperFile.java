package com.example.cramschool.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "exam_paper_files")
public class ExamPaperFile {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "exam_id", nullable = false)
	private Exam exam;

	@Column(name = "file_path", nullable = false, length = 1000)
	private String filePath;

	@Column(name = "file_name", nullable = false, length = 255)
	private String fileName;

	@Column(name = "storage_mode", nullable = false, length = 20)
	private String storageMode = "COPY";

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	public Long getId() { return id; }
	public Exam getExam() { return exam; }
	public void setExam(Exam exam) { this.exam = exam; }
	public String getFilePath() { return filePath; }
	public void setFilePath(String filePath) { this.filePath = filePath; }
	public String getFileName() { return fileName; }
	public void setFileName(String fileName) { this.fileName = fileName; }
	public String getStorageMode() { return storageMode; }
	public void setStorageMode(String storageMode) { this.storageMode = storageMode; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
