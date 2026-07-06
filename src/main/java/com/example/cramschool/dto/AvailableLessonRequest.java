package com.example.cramschool.dto;

import java.time.LocalDate;

public record AvailableLessonRequest(
		String idToken,
		LocalDate fromDate,
		LocalDate toDate) {
}
