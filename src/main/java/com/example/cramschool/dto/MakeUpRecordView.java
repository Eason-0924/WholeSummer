package com.example.cramschool.dto;

import java.time.LocalDateTime;

public record MakeUpRecordView(
		String typeLabel,
		String statusLabel,
		Long makeUpRequestId,
		Long rescheduleScheduleId,
		String className,
		String teacherName,
		LocalDateTime scheduledStart,
		LocalDateTime scheduledEnd,
		String sourceLabel,
		String originalCourseText,
		String note,
		boolean makeUpRequest) {
}
