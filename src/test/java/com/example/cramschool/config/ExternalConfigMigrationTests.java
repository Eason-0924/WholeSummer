package com.example.cramschool.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExternalConfigMigrationTests {

	@TempDir
	private Path temporaryDirectory;

	@Test
	void appendsMissingKeysWithoutOverwritingExistingValues() throws Exception {
		Path configFile = temporaryDirectory.resolve("application.properties");
		Files.writeString(configFile, "server.address=127.0.0.1\napp.auto-update.enabled=false\n");

		ExternalConfigMigration.migrate(configFile);

		String content = Files.readString(configFile);
		assertThat(content).contains("server.address=127.0.0.1");
		assertThat(content).doesNotContain("server.address=0.0.0.0");
		assertThat(content).contains("app.auto-update.enabled=false");
		assertThat(content).doesNotContain("app.auto-update.enabled=true");
		assertThat(content).contains("app.backup.dir=");
		assertThat(content).contains("app.update.check-interval-hours=24");
	}
}
