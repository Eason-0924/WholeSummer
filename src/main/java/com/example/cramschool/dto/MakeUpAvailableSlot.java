package com.example.cramschool.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record MakeUpAvailableSlot(LocalDateTime start, LocalDateTime end, MakeUpSlotStatus status) {

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

	public String getDisplayText() {
		return start.format(FORMATTER) + " - " + end.format(TIME_FORMATTER);
	}

	public String getTimeText() {
		return start.format(TIME_FORMATTER) + " - " + end.format(TIME_FORMATTER);
	}
}
