package com.example.cramschool.service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Service;

import com.example.cramschool.config.LineProperties;
import com.example.cramschool.dto.VerifiedLineUser;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class LineLiffAuthService {

	private static final URI VERIFY_URI = URI.create("https://api.line.me/oauth2/v2.1/verify");

	private final LineProperties lineProperties;
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final HttpClient httpClient = HttpClient.newHttpClient();

	public LineLiffAuthService(LineProperties lineProperties) {
		this.lineProperties = lineProperties;
	}

	public VerifiedLineUser verifyIdToken(String idToken) {
		String normalizedToken = normalizeRequired(idToken, "LINE 登入資訊已失效，請重新開啟頁面");
		String channelId = normalizeRequired(lineProperties.getLiffChannelId(), "尚未設定 LINE LIFF Channel ID");
		String body = "id_token=" + encode(normalizedToken) + "&client_id=" + encode(channelId);
		HttpRequest request = HttpRequest.newBuilder(VERIFY_URI)
				.header("Content-Type", "application/x-www-form-urlencoded")
				.POST(HttpRequest.BodyPublishers.ofString(body))
				.build();
		try {
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				throw new IllegalArgumentException("LINE 身分驗證失敗，請重新開啟頁面");
			}
			LineVerifyResponse verifyResponse = objectMapper.readValue(response.body(), LineVerifyResponse.class);
			if (verifyResponse.sub() == null || verifyResponse.sub().isBlank()) {
				throw new IllegalArgumentException("LINE 身分驗證失敗，請重新開啟頁面");
			}
			return new VerifiedLineUser(verifyResponse.sub(), verifyResponse.name(), verifyResponse.picture());
		} catch (IOException ex) {
			throw new IllegalStateException("無法連線至 LINE 驗證服務，請稍後再試", ex);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("LINE 身分驗證已中斷，請稍後再試", ex);
		}
	}

	private String normalizeRequired(String value, String message) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(message);
		}
		return value.trim();
	}

	private String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record LineVerifyResponse(
			String iss,
			String sub,
			String aud,
			Long exp,
			Long iat,
			String nonce,
			String name,
			String picture,
			@JsonProperty("email") String email) {
	}
}
