package com.example.cramschool.service.system;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.cramschool.dto.system.ApplicationStatusDto;
import com.example.cramschool.dto.system.SystemDashboardDto;
import com.example.cramschool.service.AppVersionService;

@Service
public class SystemStatusService {

	private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private final ServerMetricsService serverMetricsService;
	private final DatabaseStatusService databaseStatusService;
	private final AppVersionService appVersionService;
	private final BackupStatusService backupStatusService;
	private final LineStatusService lineStatusService;
	private final ScheduledTaskStatusService scheduledTaskStatusService;
	private final Instant startedAt = Instant.now();
	private final String environment;

	public SystemStatusService(ServerMetricsService serverMetricsService,
			DatabaseStatusService databaseStatusService, AppVersionService appVersionService,
			BackupStatusService backupStatusService, LineStatusService lineStatusService,
			ScheduledTaskStatusService scheduledTaskStatusService,
			@Value("${spring.profiles.active:default}") String environment) {
		this.serverMetricsService = serverMetricsService;
		this.databaseStatusService = databaseStatusService;
		this.appVersionService = appVersionService;
		this.backupStatusService = backupStatusService;
		this.lineStatusService = lineStatusService;
		this.scheduledTaskStatusService = scheduledTaskStatusService;
		this.environment = environment;
	}

	public SystemDashboardDto getDashboard() {
		Instant now = Instant.now();
		Duration uptime = Duration.between(startedAt, now);
		ApplicationStatusDto application = new ApplicationStatusDto(
				"UP",
				appVersionService.currentVersion(),
				environment,
				System.getProperty("java.version", "unknown"),
				System.getProperty("os.name", "unknown") + " " + System.getProperty("os.version", ""),
				format(startedAt),
				uptime.getSeconds(),
				format(now));
		return new SystemDashboardDto(application,
				serverMetricsService.getJvmStatus(),
				serverMetricsService.getServerStatus(),
				databaseStatusService.getStatus(),
				backupStatusService.getStatus(),
				lineStatusService.getStatus(),
				scheduledTaskStatusService.getTasks());
	}

	private String format(Instant instant) {
		return ZonedDateTime.ofInstant(instant, ZoneId.systemDefault()).format(DATE_TIME);
	}
}
