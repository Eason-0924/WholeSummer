package com.example.cramschool.dto;

import java.util.List;

public record MakeUpSlotOption(
		String start,
		String end,
		String timeText,
		String statusLabel,
		String statusClass,
		List<String> teacherConflictDetails) {
}
