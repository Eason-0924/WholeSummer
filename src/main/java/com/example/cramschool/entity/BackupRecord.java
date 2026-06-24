package com.example.cramschool.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "backup_records")
public class BackupRecord {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 255)
	private String fileName;

	@Column(nullable = false, length = 1000)
	private String filePath;

	@Column(name = "backup_time", nullable = false)
	private LocalDateTime backupTime;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private BackupStatus status = BackupStatus.SUCCESS;

	@Column(length = 1000)
	private String note;

	@Column(nullable = false)
	private long fileSize;

	public String getFileSizeText() {
		if (fileSize <= 0) {
			return "-";
		}
		if (fileSize < 1024) {
			return fileSize + " B";
		}
		if (fileSize < 1024 * 1024) {
			return String.format("%.1f KB", fileSize / 1024.0);
		}
		return String.format("%.1f MB", fileSize / 1024.0 / 1024.0);
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public LocalDateTime getBackupTime() {
		return backupTime;
	}

	public void setBackupTime(LocalDateTime backupTime) {
		this.backupTime = backupTime;
	}

	public BackupStatus getStatus() {
		return status;
	}

	public void setStatus(BackupStatus status) {
		this.status = status;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public long getFileSize() {
		return fileSize;
	}

	public void setFileSize(long fileSize) {
		this.fileSize = fileSize;
	}
}
