package com.example.cramschool.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "make_up_class_requests")
public class MakeUpClassRequest {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "original_course_schedule_id", nullable = false)
	private ClassSchedule originalCourseSchedule;

	@Column(name = "original_course_date", nullable = false)
	private LocalDate originalCourseDate;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "teacher_id", nullable = false)
	private Teacher teacher;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "class_id")
	private ClassRoom classRoom;

	@Enumerated(EnumType.STRING)
	@Column(name = "source_type", nullable = false, length = 50)
	private MakeUpSourceType sourceType;

	@Column(name = "source_record_id")
	private Long sourceRecordId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private MakeUpStatus status = MakeUpStatus.PENDING;

	@Column(name = "selected_make_up_start")
	private LocalDateTime selectedMakeUpStart;

	@Column(name = "selected_make_up_end")
	private LocalDateTime selectedMakeUpEnd;

	@Column(name = "selected_by_teacher_id")
	private Long selectedByTeacherId;

	@Column(name = "selected_at")
	private LocalDateTime selectedAt;

	@Column(length = 255)
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

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public ClassSchedule getOriginalCourseSchedule() {
		return originalCourseSchedule;
	}

	public void setOriginalCourseSchedule(ClassSchedule originalCourseSchedule) {
		this.originalCourseSchedule = originalCourseSchedule;
	}

	public LocalDate getOriginalCourseDate() {
		return originalCourseDate;
	}

	public void setOriginalCourseDate(LocalDate originalCourseDate) {
		this.originalCourseDate = originalCourseDate;
	}

	public Teacher getTeacher() {
		return teacher;
	}

	public void setTeacher(Teacher teacher) {
		this.teacher = teacher;
	}

	public ClassRoom getClassRoom() {
		return classRoom;
	}

	public void setClassRoom(ClassRoom classRoom) {
		this.classRoom = classRoom;
	}

	public MakeUpSourceType getSourceType() {
		return sourceType;
	}

	public void setSourceType(MakeUpSourceType sourceType) {
		this.sourceType = sourceType;
	}

	public Long getSourceRecordId() {
		return sourceRecordId;
	}

	public void setSourceRecordId(Long sourceRecordId) {
		this.sourceRecordId = sourceRecordId;
	}

	public MakeUpStatus getStatus() {
		return status;
	}

	public void setStatus(MakeUpStatus status) {
		this.status = status;
	}

	public LocalDateTime getSelectedMakeUpStart() {
		return selectedMakeUpStart;
	}

	public void setSelectedMakeUpStart(LocalDateTime selectedMakeUpStart) {
		this.selectedMakeUpStart = selectedMakeUpStart;
	}

	public LocalDateTime getSelectedMakeUpEnd() {
		return selectedMakeUpEnd;
	}

	public void setSelectedMakeUpEnd(LocalDateTime selectedMakeUpEnd) {
		this.selectedMakeUpEnd = selectedMakeUpEnd;
	}

	public Long getSelectedByTeacherId() {
		return selectedByTeacherId;
	}

	public void setSelectedByTeacherId(Long selectedByTeacherId) {
		this.selectedByTeacherId = selectedByTeacherId;
	}

	public LocalDateTime getSelectedAt() {
		return selectedAt;
	}

	public void setSelectedAt(LocalDateTime selectedAt) {
		this.selectedAt = selectedAt;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}
}
