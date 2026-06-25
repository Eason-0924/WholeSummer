package com.example.cramschool.dto;

import java.time.LocalDate;

public record HomeNotification(
		String type,
		String typeLabel,
		String title,
		String detail,
		String supplementary,
		String link,
		LocalDate date) {
}
