package com.example.cramschool.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Security;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import nl.martijndwars.webpush.Utils;

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
		assertThat(content).contains("webpush.vapid.public-key=");
		assertThat(content).contains("webpush.vapid.private-key=");
	}

	@Test
	void createsAValidVapidKeyPairOnlyWhenBothKeysAreMissing() throws Exception {
		Path configFile = temporaryDirectory.resolve("application.properties");
		Files.writeString(configFile, "webpush.vapid.auto-generate=true\n");

		ExternalConfigMigration.migrate(configFile);

		var properties = new java.util.Properties();
		try (var reader = Files.newBufferedReader(configFile)) {
			properties.load(reader);
		}
		assertThat(Base64.getUrlDecoder().decode(properties.getProperty("webpush.vapid.public-key"))).hasSize(65);
		assertThat(Utils.verifyKeyPair(
				Utils.loadPrivateKey(properties.getProperty("webpush.vapid.private-key")),
				Utils.loadPublicKey(properties.getProperty("webpush.vapid.public-key")))).isTrue();
		assertThat(Security.getProvider("BC")).isNotNull();
	}
}
