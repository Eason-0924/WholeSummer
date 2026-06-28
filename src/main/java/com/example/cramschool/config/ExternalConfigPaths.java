package com.example.cramschool.config;

import java.nio.file.Path;

public final class ExternalConfigPaths {

	public static final String ENABLED_PROPERTY = "wholesummer.external-config.enabled";
	public static final String BASE_DIR_PROPERTY = "wholesummer.base-dir";
	public static final String HOME_ENVIRONMENT = "WHOLESUMMER_HOME";

	private ExternalConfigPaths() {
	}

	public static boolean isExternalModeEnabled() {
		return Boolean.parseBoolean(System.getProperty(ENABLED_PROPERTY, "false"));
	}

	public static Path baseDirectory() {
		String configured = System.getProperty(BASE_DIR_PROPERTY);
		if (configured != null && !configured.isBlank()) {
			return Path.of(configured).toAbsolutePath().normalize();
		}
		String environmentHome = System.getenv(HOME_ENVIRONMENT);
		if (environmentHome != null && !environmentHome.isBlank()) {
			return Path.of(environmentHome).toAbsolutePath().normalize();
		}
		String programData = System.getenv("ProgramData");
		if (programData != null && !programData.isBlank()) {
			return Path.of(programData, "WholeSummer").toAbsolutePath().normalize();
		}
		return Path.of(System.getProperty("user.home"), "WholeSummer").toAbsolutePath().normalize();
	}

	public static Path classDataDirectory() {
		if (isWindows()) {
			return Path.of("C:\\WholeSummer").toAbsolutePath().normalize();
		}
		return Path.of(System.getProperty("user.home"), "WholeSummer").toAbsolutePath().normalize();
	}

	private static boolean isWindows() {
		return System.getProperty("os.name", "").toLowerCase().contains("win");
	}

	public static Path configDirectory() {
		return baseDirectory().resolve("config");
	}

	public static Path configFile() {
		return configDirectory().resolve("application.properties");
	}

	public static Path logsDirectory() {
		return baseDirectory().resolve("logs");
	}

	public static Path dataDirectory() {
		return baseDirectory().resolve("data");
	}

	public static Path backupsDirectory() {
		return baseDirectory().resolve("backups");
	}

	public static Path updateDirectory() {
		return baseDirectory().resolve("update");
	}
}
