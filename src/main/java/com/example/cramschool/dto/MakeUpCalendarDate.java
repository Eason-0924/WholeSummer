package com.example.cramschool.dto;

import java.time.LocalDate;

public record MakeUpCalendarDate(
		LocalDate date,
		String statusClass,
		String statusLabel,
		long availableCount,
		long teacherConflictCount,
		long studentConflictCount,
		boolean calculated) {
}
