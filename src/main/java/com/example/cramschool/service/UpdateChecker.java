package com.example.cramschool.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.cramschool.dto.AvailableUpdate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class UpdateChecker {

	private final AppVersionService appVersionService;
	private final ObjectMapper objectMapper;
	private final HttpClient httpClient;
	private final String repositoryOwner;
	private final String repositoryName;
	private final boolean linuxTarget;

	public UpdateChecker(AppVersionService appVersionService, ObjectMapper objectMapper,
			@Value("${app.update.github-owner:Eason-0924}") String repositoryOwner,
			@Value("${app.update.github-repo:WholeSummer}") String repositoryName) {
		this.appVersionService = appVersionService;
		this.objectMapper = objectMapper;
		this.repositoryOwner = repositoryOwner;
		this.repositoryName = repositoryName;
		this.linuxTarget = System.getProperty("os.name", "").toLowerCase().contains("linux");
		this.httpClient = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(10))
				.followRedirects(HttpClient.Redirect.NORMAL)
				.build();
	}

	public Optional<AvailableUpdate> checkLatest() throws IOException, InterruptedException {
		URI apiUri = URI.create("https://api.github.com/repos/"
				+ repositoryOwner + "/" + repositoryName + "/releases/latest");
		HttpRequest request = HttpRequest.newBuilder(apiUri)
				.timeout(Duration.ofSeconds(20))
				.header("Accept", "application/vnd.github+json")
				.header("User-Agent", "WholeSummer-UpdateChecker")
				.GET()
				.build();
		HttpResponse<String> response = httpClient.send(
				request, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() == 404) {
			return Optional.empty();
		}
		if (response.statusCode() < 200 || response.statusCode() >= 300) {
			throw new IOException("GitHub Releases 回應狀態：" + response.statusCode());
		}
		JsonNode release = objectMapper.readTree(response.body());
		String latestVersion = normalizeVersion(release.path("tag_name").asText());
		String currentVersion = normalizeVersion(appVersionService.currentVersion());
		if (!isNewerVersion(latestVersion, currentVersion)) {
			return Optional.empty();
		}
		List<JsonNode> assets = new ArrayList<>();
		release.path("assets").forEach(assets::add);
		for (JsonNode asset : assets) {
			String assetName = asset.path("name").asText();
			String downloadUrl = asset.path("browser_download_url").asText();
			if (isTargetAsset(assetName) && isTrustedDownloadUrl(downloadUrl)) {
				return Optional.of(new AvailableUpdate(currentVersion, latestVersion,
						release.path("body").asText(""), assetName, URI.create(downloadUrl)));
			}
		}
		return Optional.empty();
	}

	public boolean isNewerVersion(String candidate, String current) {
		int[] candidateParts = parseVersion(candidate);
		int[] currentParts = parseVersion(current);
		for (int index = 0; index < 3; index++) {
			if (candidateParts[index] != currentParts[index]) {
				return candidateParts[index] > currentParts[index];
			}
		}
		return false;
	}

	private int[] parseVersion(String version) {
		String normalized = normalizeVersion(version);
		if (!normalized.matches("\\d+\\.\\d+\\.\\d+")) {
			return new int[] {0, 0, 0};
		}
		String[] parts = normalized.split("\\.");
		return new int[] {
				Integer.parseInt(parts[0]),
				Integer.parseInt(parts[1]),
				Integer.parseInt(parts[2])
		};
	}

	private String normalizeVersion(String version) {
		if (version == null) {
			return "0.0.0";
		}
		String normalized = version.trim();
		if (normalized.startsWith("v") || normalized.startsWith("V")) {
			normalized = normalized.substring(1);
		}
		// Keep compatibility with releases created before tags were restricted
		// to the application version (for example v1.5.3-card-listener-v3).
		int suffixIndex = normalized.indexOf("-card-listener-v");
		return suffixIndex >= 0 ? normalized.substring(0, suffixIndex) : normalized;
	}

	private boolean isTargetAsset(String assetName) {
		if (assetName == null) {
			return false;
		}
		if (linuxTarget) {
			return assetName.startsWith("WholeSummer-")
					&& assetName.toLowerCase().endsWith(".jar");
		}
		return assetName.startsWith("WholeSummer-Windows-Installer-")
				&& (assetName.toLowerCase().endsWith(".exe") || isMsiInstaller(assetName));
	}

	private boolean isMsiInstaller(String assetName) {
		return assetName != null && assetName.toLowerCase().endsWith(".msi");
	}

	private boolean isTrustedDownloadUrl(String downloadUrl) {
		if (downloadUrl == null || downloadUrl.isBlank()) {
			return false;
		}
		URI uri = URI.create(downloadUrl);
		return "https".equalsIgnoreCase(uri.getScheme())
				&& "github.com".equalsIgnoreCase(uri.getHost())
				&& uri.getPath().startsWith("/" + repositoryOwner + "/" + repositoryName + "/releases/download/");
	}
}
