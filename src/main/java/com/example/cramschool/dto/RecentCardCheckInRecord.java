package com.example.cramschool.dto;

import java.time.LocalDateTime;

public record RecentCardCheckInRecord(
		LocalDateTime occurredAt,
		boolean success,
		String personTypeLabel,
		String displayName,
		String actionLabel,
		String message,
		String className,
		String cardId,
		String deviceName) {
}
