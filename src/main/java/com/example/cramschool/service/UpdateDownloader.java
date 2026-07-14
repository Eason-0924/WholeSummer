package com.example.cramschool.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.cramschool.config.ExternalConfigPaths;
import com.example.cramschool.dto.AvailableUpdate;

@Service
public class UpdateDownloader {

	private final Path updateDirectory;
	private final HttpClient httpClient;

	public UpdateDownloader(@Value("${app.update.dir:}") String updateDirectory) {
		this.updateDirectory = updateDirectory == null || updateDirectory.isBlank()
				? ExternalConfigPaths.updateDirectory()
				: Path.of(updateDirectory).toAbsolutePath().normalize();
		this.httpClient = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(15))
				.followRedirects(HttpClient.Redirect.NORMAL)
				.build();
	}

	public Path download(AvailableUpdate update) throws IOException, InterruptedException {
		validate(update);
		Files.createDirectories(updateDirectory);
		Path destination = updateDirectory.resolve(update.assetName()).normalize();
		if (!destination.startsWith(updateDirectory)) {
			throw new IllegalArgumentException("更新檔名不合法");
		}
		Path temporaryFile = Files.createTempFile(updateDirectory, "download-", ".tmp");
		try {
			HttpRequest request = HttpRequest.newBuilder(update.downloadUri())
					.timeout(Duration.ofMinutes(10))
					.header("Accept", "application/octet-stream")
					.header("User-Agent", "WholeSummer-UpdateDownloader")
					.GET()
					.build();
			HttpResponse<Path> response = httpClient.send(
					request, HttpResponse.BodyHandlers.ofFile(temporaryFile));
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				throw new IOException("更新下載失敗，HTTP 狀態：" + response.statusCode());
			}
			if (Files.size(temporaryFile) == 0) {
				throw new IOException("下載的更新安裝檔為空");
			}
			try {
				return Files.move(temporaryFile, destination,
						StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (java.nio.file.AtomicMoveNotSupportedException ex) {
				return Files.move(temporaryFile, destination, StandardCopyOption.REPLACE_EXISTING);
			}
		} finally {
			Files.deleteIfExists(temporaryFile);
		}
	}

	private void validate(AvailableUpdate update) {
		if (update == null || update.downloadUri() == null
				|| update.assetName() == null
				|| !update.assetName().startsWith("WholeSummer-Windows-Installer-")
				|| !(update.assetName().toLowerCase().endsWith(".exe")
						|| update.assetName().toLowerCase().endsWith(".msi"))) {
			throw new IllegalArgumentException("更新安裝檔資訊不合法");
		}
		URI uri = update.downloadUri();
		if (!"https".equalsIgnoreCase(uri.getScheme())
				|| !"github.com".equalsIgnoreCase(uri.getHost())) {
			throw new IllegalArgumentException("更新檔案必須來自 GitHub Releases");
		}
	}
}
