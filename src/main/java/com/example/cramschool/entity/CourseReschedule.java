package com.example.cramschool.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "course_reschedules")
public class CourseReschedule {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "original_schedule_id", nullable = false)
	private ClassSchedule originalSchedule;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "cancelled_schedule_id", nullable = false)
	private ClassSchedule cancelledSchedule;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "new_schedule_id", nullable = false)
	private ClassSchedule newSchedule;

	@Column(name = "original_start_at", nullable = false)
	private LocalDateTime originalStartAt;

	@Column(name = "original_end_at", nullable = false)
	private LocalDateTime originalEndAt;

	@Column(name = "new_start_at", nullable = false)
	private LocalDateTime newStartAt;

	@Column(name = "new_end_at", nullable = false)
	private LocalDateTime newEndAt;

	@Column(length = 255)
	private String reason;

	@Column(name = "created_by_teacher_id", nullable = false)
	private Long createdByTeacherId;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@PrePersist
	void prePersist() {
		this.createdAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public ClassSchedule getOriginalSchedule() {
		return originalSchedule;
	}

	public void setOriginalSchedule(ClassSchedule originalSchedule) {
		this.originalSchedule = originalSchedule;
	}

	public ClassSchedule getCancelledSchedule() {
		return cancelledSchedule;
	}

	public void setCancelledSchedule(ClassSchedule cancelledSchedule) {
		this.cancelledSchedule = cancelledSchedule;
	}

	public ClassSchedule getNewSchedule() {
		return newSchedule;
	}

	public void setNewSchedule(ClassSchedule newSchedule) {
		this.newSchedule = newSchedule;
	}

	public LocalDateTime getOriginalStartAt() {
		return originalStartAt;
	}

	public void setOriginalStartAt(LocalDateTime originalStartAt) {
		this.originalStartAt = originalStartAt;
	}

	public LocalDateTime getOriginalEndAt() {
		return originalEndAt;
	}

	public void setOriginalEndAt(LocalDateTime originalEndAt) {
		this.originalEndAt = originalEndAt;
	}

	public LocalDateTime getNewStartAt() {
		return newStartAt;
	}

	public void setNewStartAt(LocalDateTime newStartAt) {
		this.newStartAt = newStartAt;
	}

	public LocalDateTime getNewEndAt() {
		return newEndAt;
	}

	public void setNewEndAt(LocalDateTime newEndAt) {
		this.newEndAt = newEndAt;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public Long getCreatedByTeacherId() {
		return createdByTeacherId;
	}

	public void setCreatedByTeacherId(Long createdByTeacherId) {
		this.createdByTeacherId = createdByTeacherId;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
