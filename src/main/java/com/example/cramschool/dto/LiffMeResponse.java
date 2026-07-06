package com.example.cramschool.dto;

public record LiffMeResponse(
		boolean bound,
		String lineUserId,
		String displayName,
		Integer studentCount) {
}
