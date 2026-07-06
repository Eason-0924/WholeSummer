package com.example.cramschool.dto;

import java.time.LocalDate;

public record CreateLiffLeaveRequest(
		String idToken,
		Long studentId,
		Long classRoomId,
		Long scheduleId,
		LocalDate courseDate,
		String reasonType,
		String note) {
}
