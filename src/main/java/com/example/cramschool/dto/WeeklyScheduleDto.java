package com.example.cramschool.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.example.cramschool.entity.ScheduleType;

public class WeeklyScheduleDto {

	private final Long scheduleId;
	private final Long originalScheduleId;
	private final Long classRoomId;
	private final String courseName;
	private final String className;
	private final String teacherName;
	private final LocalDate courseDate;
	private final LocalDateTime startTime;
	private final LocalDateTime endTime;
	private final ScheduleType scheduleType;
	private final String scheduleTypeDisplayName;
	private final String note;
	private final String rescheduleReason;
	private final String subjectKey;
	private final String teacherKey;
	private final String gradeKey;
	private final Boolean isMakeUp;
	private final Boolean isRescheduled;
	private final Boolean isCancelled;

	public WeeklyScheduleDto(Long scheduleId, Long originalScheduleId,
			String courseName, String className, String teacherName,
			LocalDate courseDate, LocalDateTime startTime, LocalDateTime endTime,
			ScheduleType scheduleType, String note, String rescheduleReason) {
		this(scheduleId, originalScheduleId, courseName, className, teacherName,
				courseDate, startTime, endTime, scheduleType, note, rescheduleReason,
				courseName, teacherName, className);
	}

	public WeeklyScheduleDto(Long scheduleId, Long originalScheduleId,
			String courseName, String className, String teacherName,
			LocalDate courseDate, LocalDateTime startTime, LocalDateTime endTime,
			ScheduleType scheduleType, String note, String rescheduleReason,
			String subjectKey, String teacherKey, String gradeKey) {
		this(scheduleId, originalScheduleId, null, courseName, className, teacherName,
				courseDate, startTime, endTime, scheduleType, note, rescheduleReason,
				subjectKey, teacherKey, gradeKey);
	}

	public WeeklyScheduleDto(Long scheduleId, Long originalScheduleId, Long classRoomId,
			String courseName, String className, String teacherName,
			LocalDate courseDate, LocalDateTime startTime, LocalDateTime endTime,
			ScheduleType scheduleType, String note, String rescheduleReason,
			String subjectKey, String teacherKey, String gradeKey) {
		this.scheduleId = scheduleId;
		this.originalScheduleId = originalScheduleId;
		this.classRoomId = classRoomId;
		this.courseName = courseName;
		this.className = className;
		this.teacherName = teacherName;
		this.courseDate = courseDate;
		this.startTime = startTime;
		this.endTime = endTime;
		this.scheduleType = scheduleType;
		this.scheduleTypeDisplayName = scheduleType.getDisplayName();
		this.note = note;
		this.rescheduleReason = rescheduleReason;
		this.subjectKey = subjectKey;
		this.teacherKey = teacherKey;
		this.gradeKey = gradeKey;
		this.isMakeUp = scheduleType == ScheduleType.MAKE_UP;
		this.isRescheduled = scheduleType == ScheduleType.RESCHEDULED;
		this.isCancelled = scheduleType == ScheduleType.CANCELLED;
	}

	public Long getScheduleId() {
		return scheduleId;
	}

	public Long getOriginalScheduleId() {
		return originalScheduleId;
	}

	public Long getClassRoomId() {
		return classRoomId;
	}

	public String getCourseName() {
		return courseName;
	}

	public String getClassName() {
		return className;
	}

	public String getTeacherName() {
		return teacherName;
	}

	public LocalDate getCourseDate() {
		return courseDate;
	}

	public LocalDateTime getStartTime() {
		return startTime;
	}

	public LocalDateTime getEndTime() {
		return endTime;
	}

	public ScheduleType getScheduleType() {
		return scheduleType;
	}

	public String getScheduleTypeDisplayName() {
		return scheduleTypeDisplayName;
	}

	public String getNote() {
		return note;
	}

	public String getRescheduleReason() {
		return rescheduleReason;
	}

	public String getSubjectKey() {
		return subjectKey;
	}

	public String getTeacherKey() {
		return teacherKey;
	}

	public String getGradeKey() {
		return gradeKey;
	}

	public Boolean getIsMakeUp() {
		return isMakeUp;
	}

	public Boolean getIsRescheduled() {
		return isRescheduled;
	}

	public Boolean getIsCancelled() {
		return isCancelled;
	}
}
