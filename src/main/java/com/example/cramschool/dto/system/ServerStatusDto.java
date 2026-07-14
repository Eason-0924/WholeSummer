package com.example.cramschool.dto.system;

public record ServerStatusDto(
		String hostName,
		int availableProcessors,
		double systemCpuPercent,
		double systemLoadAverage,
		long storageTotalBytes,
		long storageFreeBytes,
		double storageUsagePercent) {
}
