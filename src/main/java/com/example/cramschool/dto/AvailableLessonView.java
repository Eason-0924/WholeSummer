package com.example.cramschool.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record AvailableLessonView(
		Long classRoomId,
		Long scheduleId,
		String className,
		LocalDate courseDate,
		LocalTime startTime,
		LocalTime endTime,
		String teacherName,
		String leaveStatus) {
}
