package com.example.cramschool.dto.system;

import java.time.LocalDateTime;

public record SystemLogDto(
		Long id,
		LocalDateTime createdAt,
		String actorName,
		String action,
		String method,
		String path,
		String result) {
}
