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
@Table(name = "student_leave_requests")
public class StudentLeaveRequest {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "student_id", nullable = false)
	private Student student;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "class_id", nullable = false)
	private ClassRoom classRoom;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "class_schedule_id")
	private ClassSchedule classSchedule;

	@Column(name = "course_date", nullable = false)
	private LocalDate courseDate;

	@Column(name = "scheduled_start_at", nullable = false)
	private LocalDateTime scheduledStartAt;

	@Column(name = "scheduled_end_at", nullable = false)
	private LocalDateTime scheduledEndAt;

	@Column(length = 500)
	private String reason;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private StudentLeaveStatus status = StudentLeaveStatus.PENDING;

	@Column(nullable = false, length = 50)
	private String source = "LINE_LIFF";

	@Column(name = "requester_line_user_id", length = 100)
	private String requesterLineUserId;

	@Column(name = "requester_display_name", length = 200)
	private String requesterDisplayName;

	@Column(name = "parent_relation", length = 50)
	private String parentRelation;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reviewed_by_teacher_id")
	private Teacher reviewedByTeacher;

	@Column(name = "reviewed_at")
	private LocalDateTime reviewedAt;

	@Column(name = "review_note", length = 1000)
	private String reviewNote;

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

	public Student getStudent() {
		return student;
	}

	public void setStudent(Student student) {
		this.student = student;
	}

	public ClassRoom getClassRoom() {
		return classRoom;
	}

	public void setClassRoom(ClassRoom classRoom) {
		this.classRoom = classRoom;
	}

	public ClassSchedule getClassSchedule() {
		return classSchedule;
	}

	public void setClassSchedule(ClassSchedule classSchedule) {
		this.classSchedule = classSchedule;
	}

	public LocalDate getCourseDate() {
		return courseDate;
	}

	public void setCourseDate(LocalDate courseDate) {
		this.courseDate = courseDate;
	}

	public LocalDateTime getScheduledStartAt() {
		return scheduledStartAt;
	}

	public void setScheduledStartAt(LocalDateTime scheduledStartAt) {
		this.scheduledStartAt = scheduledStartAt;
	}

	public LocalDateTime getScheduledEndAt() {
		return scheduledEndAt;
	}

	public void setScheduledEndAt(LocalDateTime scheduledEndAt) {
		this.scheduledEndAt = scheduledEndAt;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public StudentLeaveStatus getStatus() {
		return status;
	}

	public void setStatus(StudentLeaveStatus status) {
		this.status = status == null ? StudentLeaveStatus.PENDING : status;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source == null || source.isBlank() ? "LINE_LIFF" : source.trim();
	}

	public String getRequesterLineUserId() {
		return requesterLineUserId;
	}

	public void setRequesterLineUserId(String requesterLineUserId) {
		this.requesterLineUserId = requesterLineUserId;
	}

	public String getRequesterDisplayName() {
		return requesterDisplayName;
	}

	public void setRequesterDisplayName(String requesterDisplayName) {
		this.requesterDisplayName = requesterDisplayName;
	}

	public String getParentRelation() {
		return parentRelation;
	}

	public void setParentRelation(String parentRelation) {
		this.parentRelation = parentRelation;
	}

	public Teacher getReviewedByTeacher() {
		return reviewedByTeacher;
	}

	public void setReviewedByTeacher(Teacher reviewedByTeacher) {
		this.reviewedByTeacher = reviewedByTeacher;
	}

	public LocalDateTime getReviewedAt() {
		return reviewedAt;
	}

	public void setReviewedAt(LocalDateTime reviewedAt) {
		this.reviewedAt = reviewedAt;
	}

	public String getReviewNote() {
		return reviewNote;
	}

	public void setReviewNote(String reviewNote) {
		this.reviewNote = reviewNote;
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
