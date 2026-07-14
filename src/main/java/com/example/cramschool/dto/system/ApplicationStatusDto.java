package com.example.cramschool.dto.system;

public record ApplicationStatusDto(
		String status,
		String version,
		String environment,
		String javaVersion,
		String operatingSystem,
		String startedAt,
		long uptimeSeconds,
		String serverTime) {
}
