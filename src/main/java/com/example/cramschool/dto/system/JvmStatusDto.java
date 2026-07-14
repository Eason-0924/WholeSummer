package com.example.cramschool.dto.system;

public record JvmStatusDto(
		long usedMemoryBytes,
		long maxMemoryBytes,
		double memoryUsagePercent,
		int threadCount,
		double processCpuPercent) {
}
