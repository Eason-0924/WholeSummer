package com.example.cramschool.dto;

public record TeacherCourseOption(
		Long scheduleId,
		Long teacherId,
		String weekday,
		String timeRangeText,
		String className) {

	public String getDisplayText() {
		return weekday + " " + timeRangeText + "｜" + className;
	}
}
