package com.example.cramschool.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "exams")
public class Exam {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "class_id", nullable = false)
	private ClassRoom classRoom;

	@ManyToOne(optional = false)
	@JoinColumn(name = "subject_id", nullable = false)
	private Subject subject;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(name = "exam_date", nullable = false)
	private LocalDate examDate;

	@Column(name = "full_score", nullable = false)
	private Integer fullScore;

	@Column(length = 1000)
	private String description;

	@Column(name = "paper_file_path", length = 1000)
	private String paperFilePath;

	@Column(name = "paper_file_name", length = 255)
	private String paperFileName;

	@Column(name = "paper_storage_mode", length = 20)
	private String paperStorageMode;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	void prePersist() {
		LocalDateTime now = LocalDateTime.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	void preUpdate() {
		this.updatedAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public ClassRoom getClassRoom() {
		return classRoom;
	}

	public void setClassRoom(ClassRoom classRoom) {
		this.classRoom = classRoom;
	}

	public Subject getSubject() {
		return subject;
	}

	public void setSubject(Subject subject) {
		this.subject = subject;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public LocalDate getExamDate() {
		return examDate;
	}

	public void setExamDate(LocalDate examDate) {
		this.examDate = examDate;
	}

	public Integer getFullScore() {
		return fullScore;
	}

	public void setFullScore(Integer fullScore) {
		this.fullScore = fullScore;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getPaperFilePath() {
		return paperFilePath;
	}

	public void setPaperFilePath(String paperFilePath) {
		this.paperFilePath = paperFilePath;
	}

	public String getPaperFileName() {
		return paperFileName;
	}

	public void setPaperFileName(String paperFileName) {
		this.paperFileName = paperFileName;
	}

	public String getPaperStorageMode() {
		return paperStorageMode;
	}

	public void setPaperStorageMode(String paperStorageMode) {
		this.paperStorageMode = paperStorageMode;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
}
