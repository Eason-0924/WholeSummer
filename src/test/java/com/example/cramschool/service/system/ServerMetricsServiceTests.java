package com.example.cramschool.service.system;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;

import org.junit.jupiter.api.Test;

class ServerMetricsServiceTests {

	@Test
	void reportsJvmAndStorageMetricsWithoutWindowsSpecificPaths() throws Exception {
		var storage = Files.createTempDirectory("wholesummer-system-status");
		var service = new ServerMetricsService(storage.toString());

		var jvm = service.getJvmStatus();
		var server = service.getServerStatus();

		assertThat(jvm.maxMemoryBytes()).isPositive();
		assertThat(jvm.usedMemoryBytes()).isGreaterThanOrEqualTo(0);
		assertThat(jvm.threadCount()).isPositive();
		assertThat(server.availableProcessors()).isPositive();
		assertThat(server.storageTotalBytes()).isPositive();
		assertThat(server.storageFreeBytes()).isGreaterThanOrEqualTo(0);
	}
}
