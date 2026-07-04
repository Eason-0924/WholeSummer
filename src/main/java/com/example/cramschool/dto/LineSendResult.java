package com.example.cramschool.dto;

public record LineSendResult(
		boolean success,
		String providerMessageId,
		String errorMessage) {

	public static LineSendResult success(String providerMessageId) {
		return new LineSendResult(true, providerMessageId, null);
	}

	public static LineSendResult failure(String errorMessage) {
		return new LineSendResult(false, null, errorMessage);
	}
}
