package com.example.cramschool.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public final class ExternalConfigMigration {

	private ExternalConfigMigration() {
	}

	public static void migrate(Path configFile) throws IOException {
		replaceLegacyDdlAuto(configFile);
		Properties existing = new Properties();
		try (var reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
			existing.load(reader);
		}
		Map<String, String> defaults = defaultValues();
		StringBuilder additions = new StringBuilder();
		for (Map.Entry<String, String> entry : defaults.entrySet()) {
			if (!existing.containsKey(entry.getKey())) {
				additions.append(entry.getKey())
						.append('=')
						.append(escapePropertyValue(entry.getValue()))
						.append(System.lineSeparator());
			}
		}
		appendVapidKeyPairIfNeeded(existing, additions);
		if (!additions.isEmpty()) {
			String section = System.lineSeparator()
					+ "# Added automatically by WholeSummer" + System.lineSeparator()
					+ additions;
			Files.writeString(configFile, section, StandardCharsets.UTF_8,
					StandardOpenOption.APPEND);
		}
	}

	static Map<String, String> defaultValues() {
		Map<String, String> values = new LinkedHashMap<>();
		values.put("server.address", "0.0.0.0");
		values.put("spring.flyway.enabled", "true");
		values.put("spring.flyway.locations", "classpath:db/migration");
		values.put("spring.flyway.baseline-on-migrate", "true");
		values.put("spring.flyway.baseline-version", "1");
		values.put("spring.flyway.validate-on-migrate", "true");
		values.put("spring.flyway.clean-disabled", "true");
		values.put("spring.jpa.hibernate.ddl-auto", "validate");
		values.put("spring.jpa.show-sql", "false");
		values.put("spring.jpa.open-in-view", "false");
		values.put("spring.servlet.multipart.max-file-size", "20MB");
		values.put("spring.servlet.multipart.max-request-size", "20MB");
		values.put("app.data.dir", windowsPath(ExternalConfigPaths.dataDirectory()));
		values.put("app.backup.dir", windowsPath(ExternalConfigPaths.backupsDirectory()));
		values.put("app.exam-paper.dir", windowsPath(ExternalConfigPaths.classDataDirectory()));
		values.put("app.update.dir", windowsPath(ExternalConfigPaths.updateDirectory()));
		values.put("app.mysql.bin-dir", "");
		values.put("app.status-window.enabled", "true");
		values.put("logging.file.name",
				windowsPath(ExternalConfigPaths.logsDirectory().resolve("wholesummer.log")));
		values.put("app.auto-update.enabled", "true");
		values.put("app.update.check-on-startup", "true");
		values.put("app.update.check-interval-hours", "24");
		values.put("app.update.github-owner", "Eason-0924");
		values.put("app.update.github-repo", "WholeSummer");
		values.put("app.report.mail.enabled", "${WHOLESUMMER_REPORT_ENABLED:false}");
		values.put("app.report.mail.api-key", "${RESEND_API_KEY:}");
		values.put("app.report.mail.from", "${WHOLESUMMER_REPORT_FROM:}");
		values.put("app.report.mail.recipient", "${WHOLESUMMER_REPORT_RECIPIENT:}");
		values.put("webpush.vapid.auto-generate", "true");
		return values;
	}

	private static void appendVapidKeyPairIfNeeded(Properties existing, StringBuilder additions) {
		if (!Boolean.parseBoolean(existing.getProperty("webpush.vapid.auto-generate", "true"))) {
			return;
		}
		String publicKey = existing.getProperty("webpush.vapid.public-key", "").trim();
		String privateKey = existing.getProperty("webpush.vapid.private-key", "").trim();
		if (!publicKey.isEmpty() && !privateKey.isEmpty()) {
			return;
		}
		if (!publicKey.isEmpty() || !privateKey.isEmpty()) {
			throw new IllegalStateException("VAPID 公鑰與私鑰必須同時設定；請修正 "
					+ "webpush.vapid.public-key 與 webpush.vapid.private-key");
		}
		VapidKeyGenerator.VapidKeyPair keyPair = VapidKeyGenerator.generate();
		additions.append("# Web Push VAPID key pair generated automatically by WholeSummer")
				.append(System.lineSeparator())
				.append("webpush.vapid.public-key=").append(keyPair.publicKey())
				.append(System.lineSeparator())
				.append("webpush.vapid.private-key=").append(keyPair.privateKey())
				.append(System.lineSeparator())
				.append("webpush.vapid.subject=mailto:admin@whole-summer.com")
				.append(System.lineSeparator());
	}

	private static void replaceLegacyDdlAuto(Path configFile) throws IOException {
		String content = Files.readString(configFile, StandardCharsets.UTF_8);
		String migrated = content.replaceAll(
				"(?m)^spring\\.jpa\\.hibernate\\.ddl-auto\\s*=\\s*(update|create|create-drop)\\s*$",
				"spring.jpa.hibernate.ddl-auto=validate");
		if (!migrated.equals(content)) {
			Files.writeString(configFile, migrated, StandardCharsets.UTF_8);
		}
	}

	static String windowsPath(Path path) {
		return path.toAbsolutePath().normalize().toString().replace('\\', '/');
	}

	static String escapePropertyValue(String value) {
		return value.replace("\\", "\\\\")
				.replace("\r", "\\r")
				.replace("\n", "\\n");
	}
}
