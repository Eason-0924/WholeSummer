package com.example.cramschool.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ExternalConfigInitializer {

	private ExternalConfigInitializer() {
	}

	public static boolean prepare() throws IOException {
		if (!ExternalConfigPaths.isExternalModeEnabled()) {
			return true;
		}
		createDirectories();
		Path configFile = ExternalConfigPaths.configFile();
		if (!Files.exists(configFile)) {
			throw new IllegalStateException("找不到外部設定檔，請先建立 EC2 設定檔：" + configFile);
		}
		ExternalConfigMigration.migrate(configFile);
		System.setProperty("spring.config.additional-location",
				ExternalConfigPaths.configDirectory().toUri().toString());
		return true;
	}

	private static void createDirectories() throws IOException {
		Files.createDirectories(ExternalConfigPaths.configDirectory());
		Files.createDirectories(ExternalConfigPaths.logsDirectory());
		Files.createDirectories(ExternalConfigPaths.dataDirectory());
		Files.createDirectories(ExternalConfigPaths.backupsDirectory());
		Files.createDirectories(ExternalConfigPaths.updateDirectory());
		Files.createDirectories(ExternalConfigPaths.classDataDirectory());
	}
}
