package com.example.cramschool.dto;

import java.util.List;

import com.example.cramschool.entity.ClassSchedule;

public record DirectRescheduleView(
		ClassSchedule schedule,
		String className,
		String teacherName,
		String originalCourseText,
		List<MakeUpCalendarDate> calendarDates) {
}
