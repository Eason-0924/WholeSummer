package com.example.cramschool.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TodayWorkbenchView(
		LocalDate date,
		List<TodayCourseItem> courses,
		TodayAttendanceSummary attendance,
		List<TodayHomeworkItem> homeworks,
		List<TodayExamItem> exams) {

	public record TodayCourseItem(
			Long classRoomId,
			String className,
			String teacherName,
			LocalDateTime startTime,
			LocalDateTime endTime,
			String scheduleTypeLabel) {
	}

	public record TodayAttendanceSummary(
			long presentCount,
			long missingCount,
			long lateCount,
			long earlyLeaveCount,
			List<String> presentNames,
			List<String> missingNames,
			List<String> lateNames,
			List<String> earlyLeaveNames) {
	}

	public record TodayHomeworkItem(
			Long homeworkId,
			String title,
			String className,
			String teacherName) {
	}

	public record TodayExamItem(
			Long examId,
			String name,
			String className,
			String teacherName) {
	}
}
