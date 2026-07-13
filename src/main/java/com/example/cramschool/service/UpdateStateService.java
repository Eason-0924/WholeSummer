package com.example.cramschool.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class UpdateStateService {

	private static final String LAST_CHECK_TIME = "lastCheckTime";
	private static final String LAST_IGNORED_VERSION = "lastIgnoredVersion";
	private static final String LAST_PUSH_NOTIFIED_VERSION = "lastPushNotifiedVersion";

	private final Path stateFile;
	private final long checkIntervalHours;

	public UpdateStateService(
			@Value("${app.update.dir:${user.dir}/data/update}") String updateDirectory,
			@Value("${app.update.check-interval-hours:24}") long checkIntervalHours) {
		this.stateFile = Path.of(updateDirectory).toAbsolutePath().normalize()
				.resolve("update-state.properties");
		this.checkIntervalHours = Math.max(1, checkIntervalHours);
	}

	public boolean shouldCheckNow() {
		Properties state = load();
		try {
			long lastCheck = Long.parseLong(state.getProperty(LAST_CHECK_TIME, "0"));
			return Instant.ofEpochMilli(lastCheck)
					.plus(Duration.ofHours(checkIntervalHours))
					.isBefore(Instant.now());
		} catch (NumberFormatException ex) {
			return true;
		}
	}

	public void recordCheckNow() {
		Properties state = load();
		state.setProperty(LAST_CHECK_TIME, String.valueOf(System.currentTimeMillis()));
		save(state);
	}

	public void ignoreVersion(String version) {
		Properties state = load();
		state.setProperty(LAST_IGNORED_VERSION, version == null ? "" : version);
		save(state);
	}

	public boolean isIgnored(String version) {
		return version != null && version.equals(load().getProperty(LAST_IGNORED_VERSION, ""));
	}

	public boolean shouldNotifyVersion(String version) {
		return version != null && !version.isBlank()
				&& !version.equals(load().getProperty(LAST_PUSH_NOTIFIED_VERSION, ""));
	}

	public void markVersionNotified(String version) {
		if (version == null || version.isBlank()) {
			return;
		}
		Properties state = load();
		state.setProperty(LAST_PUSH_NOTIFIED_VERSION, version.trim());
		save(state);
	}

	private Properties load() {
		Properties properties = new Properties();
		if (!Files.isRegularFile(stateFile)) {
			return properties;
		}
		try (var reader = Files.newBufferedReader(stateFile, StandardCharsets.UTF_8)) {
			properties.load(reader);
		} catch (IOException ignored) {
		}
		return properties;
	}

	private void save(Properties properties) {
		try {
			Files.createDirectories(stateFile.getParent());
			Path temporaryFile = Files.createTempFile(stateFile.getParent(), "update-state-", ".tmp");
			try (var writer = Files.newBufferedWriter(temporaryFile, StandardCharsets.UTF_8)) {
				properties.store(writer, "WholeSummer update state");
			}
			try {
				Files.move(temporaryFile, stateFile,
						StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (java.nio.file.AtomicMoveNotSupportedException ex) {
				Files.move(temporaryFile, stateFile, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException ignored) {
			// Update checks must never prevent the application from running.
		}
	}
}
