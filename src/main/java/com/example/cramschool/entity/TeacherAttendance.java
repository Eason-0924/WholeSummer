package com.example.cramschool.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
import jakarta.persistence.Transient;
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

	@Column(name = "matched_course_id")
	private Long matchedCourseId;

	@Column(name = "matched_course_name", length = 1000)
	private String matchedCourseName;

	@Column(name = "matched_course_time_text", length = 500)
	private String matchedCourseTimeText;

	@Column(name = "manual_remark", length = 255)
	private String manualRemark;

	@Column(name = "manual_hours", precision = 5, scale = 2)
	private BigDecimal manualHours;

	@Column(name = "manual_adjusted", nullable = false)
	private boolean manualAdjusted;

	@Column(name = "adjusted_by_teacher_id")
	private Long adjustedByTeacherId;

	@Column(name = "adjusted_at")
	private LocalDateTime adjustedAt;

	@Transient
	private boolean manualAdjustmentAllowed;

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
		long minutes = 0;
		if (matchedCourseId != null && workMinutes != null) {
			minutes += workMinutes;
		}
		if (manualAdjusted && manualHours != null) {
			minutes += manualHours.multiply(BigDecimal.valueOf(60))
					.setScale(0, RoundingMode.HALF_UP)
					.longValue();
		}
		return minutes;
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

	public Long getMatchedCourseId() {
		return matchedCourseId;
	}

	public void setMatchedCourseId(Long matchedCourseId) {
		this.matchedCourseId = matchedCourseId;
	}

	public String getMatchedCourseName() {
		return matchedCourseName;
	}

	public void setMatchedCourseName(String matchedCourseName) {
		this.matchedCourseName = matchedCourseName;
	}

	public String getMatchedCourseTimeText() {
		return matchedCourseTimeText;
	}

	public void setMatchedCourseTimeText(String matchedCourseTimeText) {
		this.matchedCourseTimeText = matchedCourseTimeText;
	}

	public String getManualRemark() {
		return manualRemark;
	}

	public void setManualRemark(String manualRemark) {
		this.manualRemark = manualRemark;
	}

	public BigDecimal getManualHours() {
		return manualHours;
	}

	public String getManualHoursInputValue() {
		return manualHours == null ? null : manualHours.setScale(1, RoundingMode.HALF_UP).toPlainString();
	}

	public void setManualHours(BigDecimal manualHours) {
		this.manualHours = manualHours;
	}

	public boolean isManualAdjusted() {
		return manualAdjusted;
	}

	public void setManualAdjusted(boolean manualAdjusted) {
		this.manualAdjusted = manualAdjusted;
	}

	public Long getAdjustedByTeacherId() {
		return adjustedByTeacherId;
	}

	public void setAdjustedByTeacherId(Long adjustedByTeacherId) {
		this.adjustedByTeacherId = adjustedByTeacherId;
	}

	public LocalDateTime getAdjustedAt() {
		return adjustedAt;
	}

	public void setAdjustedAt(LocalDateTime adjustedAt) {
		this.adjustedAt = adjustedAt;
	}

	public boolean isManualAdjustmentAllowed() {
		return manualAdjustmentAllowed;
	}

	public void setManualAdjustmentAllowed(boolean manualAdjustmentAllowed) {
		this.manualAdjustmentAllowed = manualAdjustmentAllowed;
	}

	public boolean hasMatchedCourse() {
		return matchedCourseId != null;
	}

	public String getCountedHoursText() {
		long minutes = getWorkMinutes();
		return minutes == 0 ? "0 小時" : minutes / 60 + " 小時 " + minutes % 60 + " 分";
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
