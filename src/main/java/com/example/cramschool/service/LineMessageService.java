package com.example.cramschool.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.cramschool.config.LineProperties;
import com.example.cramschool.dto.LineSendResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class LineMessageService {

	private static final Logger log = LoggerFactory.getLogger(LineMessageService.class);
	private static final URI REPLY_URI = URI.create("https://api.line.me/v2/bot/message/reply");
	private static final URI PUSH_URI = URI.create("https://api.line.me/v2/bot/message/push");
	private static final String PROFILE_URI_PREFIX = "https://api.line.me/v2/bot/profile/";

	private final LineProperties lineProperties;
	private final ObjectMapper objectMapper;
	private final HttpClient httpClient;

	public LineMessageService(LineProperties lineProperties) {
		this.lineProperties = lineProperties;
		this.objectMapper = new ObjectMapper();
		this.httpClient = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(8))
				.build();
	}

	public void replyText(String replyToken, String text) {
		if (replyToken == null || replyToken.isBlank() || text == null || text.isBlank()) {
			return;
		}
		if (lineProperties.getChannelAccessToken() == null
				|| lineProperties.getChannelAccessToken().isBlank()) {
			log.warn("Cannot reply LINE message because channel access token is not configured.");
			return;
		}
		try {
			Map<String, Object> textMessage = new LinkedHashMap<>();
			textMessage.put("type", "text");
			textMessage.put("text", text);

			Map<String, Object> payload = new LinkedHashMap<>();
			payload.put("replyToken", replyToken);
			payload.put("messages", List.of(textMessage));

			HttpRequest request = HttpRequest.newBuilder(REPLY_URI)
					.timeout(Duration.ofSeconds(10))
					.header("Authorization", "Bearer " + lineProperties.getChannelAccessToken())
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
					.build();
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				log.warn("LINE reply failed. status={} body={}", response.statusCode(), response.body());
			}
		} catch (Exception ex) {
			log.warn("LINE reply failed.", ex);
		}
	}

	public LineSendResult pushText(String lineUserId, String text) {
		if (lineUserId == null || lineUserId.isBlank()) {
			return LineSendResult.failure("缺少 LINE userId");
		}
		if (text == null || text.isBlank()) {
			return LineSendResult.failure("缺少訊息內容");
		}
		if (lineProperties.getChannelAccessToken() == null
				|| lineProperties.getChannelAccessToken().isBlank()) {
			return LineSendResult.failure("尚未設定 LINE Channel access token");
		}
		try {
			Map<String, Object> textMessage = new LinkedHashMap<>();
			textMessage.put("type", "text");
			textMessage.put("text", text);

			Map<String, Object> payload = new LinkedHashMap<>();
			payload.put("to", lineUserId);
			payload.put("messages", List.of(textMessage));

			HttpResponse<String> response = postJson(PUSH_URI, payload);
			if (response.statusCode() >= 200 && response.statusCode() < 300) {
				return LineSendResult.success(response.headers()
						.firstValue("x-line-request-id")
						.orElse(null));
			}
			return LineSendResult.failure("LINE Push API 失敗（HTTP "
					+ response.statusCode() + "）：" + response.body());
		} catch (Exception ex) {
			log.warn("LINE push failed.", ex);
			return LineSendResult.failure(ex.getMessage() == null
					? ex.getClass().getSimpleName()
					: ex.getMessage());
		}
	}

	public Optional<String> getProfileDisplayName(String lineUserId) {
		if (lineUserId == null || lineUserId.isBlank()
				|| lineProperties.getChannelAccessToken() == null
				|| lineProperties.getChannelAccessToken().isBlank()) {
			return Optional.empty();
		}
		try {
			HttpRequest request = HttpRequest.newBuilder(URI.create(PROFILE_URI_PREFIX + lineUserId))
					.timeout(Duration.ofSeconds(10))
					.header("Authorization", "Bearer " + lineProperties.getChannelAccessToken())
					.GET()
					.build();
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				log.warn("LINE profile lookup failed. status={} body={}", response.statusCode(), response.body());
				return Optional.empty();
			}
			JsonNode profile = objectMapper.readTree(response.body());
			String displayName = profile.path("displayName").asText("");
			return displayName == null || displayName.isBlank()
					? Optional.empty()
					: Optional.of(displayName.trim());
		} catch (Exception ex) {
			log.warn("LINE profile lookup failed.", ex);
			return Optional.empty();
		}
	}

	private HttpResponse<String> postJson(URI uri, Map<String, Object> payload) throws Exception {
		HttpRequest request = HttpRequest.newBuilder(uri)
				.timeout(Duration.ofSeconds(10))
				.header("Authorization", "Bearer " + lineProperties.getChannelAccessToken())
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
				.build();
		return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
	}
}
