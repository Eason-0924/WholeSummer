package com.example.cramschool.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.cramschool.config.LineProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class LineMessageService {

	private static final Logger log = LoggerFactory.getLogger(LineMessageService.class);
	private static final URI REPLY_URI = URI.create("https://api.line.me/v2/bot/message/reply");

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
}
