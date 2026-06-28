package com.example.cramschool.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

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
import jakarta.persistence.Table;

@Entity
@Table(name = "class_schedules")
public class ClassSchedule {

	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "class_id", nullable = false)
	private ClassRoom classRoom;

	@Column(length = 20, nullable = false)
	private String weekday;

	@Column(name = "start_time", nullable = false)
	private LocalTime startTime;

	@Column(name = "end_time", nullable = false)
	private LocalTime endTime;

	@Enumerated(EnumType.STRING)
	@Column(name = "schedule_type", nullable = false, length = 50)
	private ScheduleType scheduleType = ScheduleType.NORMAL;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "original_schedule_id")
	private ClassSchedule originalSchedule;

	@Column(name = "course_date")
	private LocalDate courseDate;

	@Column(name = "scheduled_start_at")
	private LocalDateTime scheduledStartAt;

	@Column(name = "scheduled_end_at")
	private LocalDateTime scheduledEndAt;

	@Column(name = "reschedule_reason", length = 255)
	private String rescheduleReason;

	@Column(name = "created_by_teacher_id")
	private Long createdByTeacherId;

	public ClassSchedule() {
	}

	public ClassSchedule(String weekday, LocalTime startTime, LocalTime endTime) {
		this.weekday = weekday;
		this.startTime = startTime;
		this.endTime = endTime;
	}

	public String getDisplayText() {
		return weekday + " " + getTimeRangeText();
	}

	public String getTimeRangeText() {
		return startTime.format(TIME_FORMATTER) + " ~ " + endTime.format(TIME_FORMATTER);
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

	public String getWeekday() {
		return weekday;
	}

	public void setWeekday(String weekday) {
		this.weekday = weekday;
	}

	public LocalTime getStartTime() {
		return startTime;
	}

	public void setStartTime(LocalTime startTime) {
		this.startTime = startTime;
	}

	public LocalTime getEndTime() {
		return endTime;
	}

	public void setEndTime(LocalTime endTime) {
		this.endTime = endTime;
	}

	public ScheduleType getScheduleType() {
		return scheduleType == null ? ScheduleType.NORMAL : scheduleType;
	}

	public void setScheduleType(ScheduleType scheduleType) {
		this.scheduleType = scheduleType == null ? ScheduleType.NORMAL : scheduleType;
	}

	public ClassSchedule getOriginalSchedule() {
		return originalSchedule;
	}

	public void setOriginalSchedule(ClassSchedule originalSchedule) {
		this.originalSchedule = originalSchedule;
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

	public String getRescheduleReason() {
		return rescheduleReason;
	}

	public void setRescheduleReason(String rescheduleReason) {
		this.rescheduleReason = rescheduleReason;
	}

	public Long getCreatedByTeacherId() {
		return createdByTeacherId;
	}

	public void setCreatedByTeacherId(Long createdByTeacherId) {
		this.createdByTeacherId = createdByTeacherId;
	}
}
