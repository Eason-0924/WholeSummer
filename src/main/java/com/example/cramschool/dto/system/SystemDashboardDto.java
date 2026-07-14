package com.example.cramschool.dto.system;

public record SystemDashboardDto(
		ApplicationStatusDto application,
		JvmStatusDto jvm,
		ServerStatusDto server,
		DatabaseStatusDto database,
		BackupStatusDto backup,
		LineStatusDto line,
		java.util.List<ScheduledTaskStatusDto> tasks) {
}
