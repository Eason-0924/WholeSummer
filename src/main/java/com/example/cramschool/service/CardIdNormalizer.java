package com.example.cramschool.service;

public final class CardIdNormalizer {

	private CardIdNormalizer() {
	}

	public static String normalize(String cardId) {
		if (cardId == null) {
			return null;
		}
		String normalized = cardId.trim()
				.replace(" ", "")
				.replace("\n", "")
				.replace("\r", "")
				.toUpperCase();
		return normalized.isBlank() ? null : normalized;
	}
}
