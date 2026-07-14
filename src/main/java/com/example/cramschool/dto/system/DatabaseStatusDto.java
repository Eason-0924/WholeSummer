package com.example.cramschool.dto.system;

public record DatabaseStatusDto(
		String status,
		String schema,
		String databaseProduct,
		String flywayVersion,
		long responseTimeMs,
		String message) {

	public static DatabaseStatusDto unavailable(String message) {
		return new DatabaseStatusDto("ERROR", "-", "-", "-", -1, message);
	}
}
