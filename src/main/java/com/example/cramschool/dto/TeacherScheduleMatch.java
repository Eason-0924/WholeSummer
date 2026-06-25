package com.example.cramschool.dto;

import java.time.LocalTime;

public record TeacherScheduleMatch(
		Long firstScheduleId,
		String courseNames,
		String timeRangeText,
		long workMinutes,
		LocalTime firstStartTime) {

	public static TeacherScheduleMatch empty() {
		return new TeacherScheduleMatch(null, null, null, 0, null);
	}

	public boolean isMatched() {
		return firstScheduleId != null;
	}
}
