package com.example.cramschool.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

import com.example.cramschool.config.LineProperties;

@Service
public class LineSignatureValidator {

	private static final String HMAC_ALGORITHM = "HmacSHA256";

	private final LineProperties lineProperties;

	public LineSignatureValidator(LineProperties lineProperties) {
		this.lineProperties = lineProperties;
	}

	public boolean isConfigured() {
		return lineProperties.getChannelSecret() != null
				&& !lineProperties.getChannelSecret().isBlank();
	}

	public boolean isValid(String requestBody, String signature) {
		if (!isConfigured() || signature == null || signature.isBlank()) {
			return false;
		}
		try {
			Mac mac = Mac.getInstance(HMAC_ALGORITHM);
			SecretKeySpec key = new SecretKeySpec(
					lineProperties.getChannelSecret().getBytes(StandardCharsets.UTF_8),
					HMAC_ALGORITHM);
			mac.init(key);
			byte[] digest = mac.doFinal((requestBody == null ? "" : requestBody)
					.getBytes(StandardCharsets.UTF_8));
			String expected = Base64.getEncoder().encodeToString(digest);
			return MessageDigest.isEqual(
					expected.getBytes(StandardCharsets.UTF_8),
					signature.getBytes(StandardCharsets.UTF_8));
		} catch (Exception ex) {
			return false;
		}
	}
}
