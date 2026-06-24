package com.example.cramschool.config;

import java.awt.GraphicsEnvironment;
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
			if (GraphicsEnvironment.isHeadless()) {
				throw new IllegalStateException("找不到外部設定檔，且目前環境無法顯示首次設定視窗："
						+ configFile);
			}
			if (!new FirstRunSetupDialog().showAndCreateConfiguration()) {
				return false;
			}
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
	}
}
