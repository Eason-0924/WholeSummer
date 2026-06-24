package com.example.cramschool.entity;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "teacher_attendances",
		uniqueConstraints = @UniqueConstraint(columnNames = { "teacher_id", "attendance_date" }))
public class TeacherAttendance {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "teacher_id", nullable = false)
	private Teacher teacher;

	@Column(name = "attendance_date", nullable = false)
	private LocalDate date;

	@Column(name = "clock_in_time")
	private LocalTime clockInTime;

	@Column(name = "clock_out_time")
	private LocalTime clockOutTime;

	@Column(name = "work_minutes")
	private Long workMinutes;

	@Column(name = "scheduled_time_text", length = 500)
	private String scheduledTimeText;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private TeacherAttendanceStatus status = TeacherAttendanceStatus.WORKING;

	@Column(length = 1000)
	private String note;

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

	public String getWorkHoursText() {
		long minutes = getWorkMinutes();
		if (minutes <= 0) {
			return "-";
		}
		return minutes / 60 + " 小時 " + minutes % 60 + " 分";
	}

	public long getWorkMinutes() {
		if (status != TeacherAttendanceStatus.WORKING && status != TeacherAttendanceStatus.LATE) {
			return 0;
		}
		if (workMinutes != null) {
			return workMinutes;
		}
		if (clockInTime == null || clockOutTime == null || clockOutTime.isBefore(clockInTime)) {
			return 0;
		}
		return Duration.between(clockInTime, clockOutTime).toMinutes();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Teacher getTeacher() {
		return teacher;
	}

	public void setTeacher(Teacher teacher) {
		this.teacher = teacher;
	}

	public LocalDate getDate() {
		return date;
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

	public LocalTime getClockInTime() {
		return clockInTime;
	}

	public void setClockInTime(LocalTime clockInTime) {
		this.clockInTime = clockInTime;
	}

	public LocalTime getClockOutTime() {
		return clockOutTime;
	}

	public void setClockOutTime(LocalTime clockOutTime) {
		this.clockOutTime = clockOutTime;
	}

	public Long getStoredWorkMinutes() {
		return workMinutes;
	}

	public void setWorkMinutes(Long workMinutes) {
		this.workMinutes = workMinutes;
	}

	public String getScheduledTimeText() {
		return scheduledTimeText == null || scheduledTimeText.isBlank() ? "當日無課程" : scheduledTimeText;
	}

	public void setScheduledTimeText(String scheduledTimeText) {
		this.scheduledTimeText = scheduledTimeText;
	}

	public TeacherAttendanceStatus getStatus() {
		return status;
	}

	public void setStatus(TeacherAttendanceStatus status) {
		this.status = status;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
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
