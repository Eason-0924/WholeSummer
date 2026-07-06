package com.example.cramschool.dto;

public record LiffLeaveRequestResponse(
		boolean success,
		Long leaveRequestId,
		String status,
		String message) {
}
