package com.example.cramschool.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Base64;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.config.WebPushProperties;
import com.example.cramschool.dto.PushSubscriptionRequest;
import com.example.cramschool.entity.WebPushSubscription;
import com.example.cramschool.repository.WebPushSubscriptionRepository;

@Service
public class WebPushSubscriptionService {

	private final WebPushSubscriptionRepository repository;
	private final WebPushProperties properties;

	public WebPushSubscriptionService(WebPushSubscriptionRepository repository, WebPushProperties properties) {
		this.repository = repository;
		this.properties = properties;
	}

	@Transactional
	public WebPushSubscription subscribe(Long userId, PushSubscriptionRequest request, String userAgent) {
		String endpoint = requireText(request.getEndpoint(), "缺少推播 endpoint");
		verifyApplicationServerKey(request.getApplicationServerKey());
		String endpointHash = endpointHash(endpoint);
		WebPushSubscription subscription = repository.findByEndpointHash(endpointHash)
				.orElseGet(WebPushSubscription::new);
		subscription.setUserId(userId);
		subscription.setEndpoint(endpoint);
		subscription.setEndpointHash(endpointHash);
		String vapidKeyHash = valueHash(properties.getPublicKey());
		subscription.setVapidKeyHash(vapidKeyHash);
		subscription.setP256dh(requireText(request.getKeys().getP256dh(), "缺少推播 p256dh key"));
		subscription.setAuth(requireText(request.getKeys().getAuth(), "缺少推播 auth key"));
		subscription.setBrowser(trimToLength(request.getBrowser(), 100));
		subscription.setDeviceName(trimToLength(request.getDeviceName(), 100));
		subscription.setUserAgent(userAgent);
		subscription.setEnabled(true);
		subscription.setFailureCount(0);
		WebPushSubscription saved = repository.save(subscription);
		repository.disableSubscriptionsWithDifferentVapidKey(userId, vapidKeyHash);
		return saved;
	}

	@Transactional
	public boolean unsubscribe(Long userId, String endpoint) {
		Optional<WebPushSubscription> subscription =
				repository.findByUserIdAndEndpointHash(userId, endpointHash(requireText(endpoint, "缺少推播 endpoint")));
		subscription.ifPresent(value -> value.setEnabled(false));
		return subscription.isPresent();
	}

	@Transactional(readOnly = true)
	public List<WebPushSubscription> findEnabledByUser(Long userId, String vapidKeyHash) {
		return repository.findByUserIdAndEnabledTrueAndVapidKeyHashOrderByUpdatedAtDesc(userId, vapidKeyHash);
	}

	public String endpointHash(String endpoint) {
		return valueHash(endpoint);
	}

	public String valueHash(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(requireText(value, "缺少要雜湊的內容")
					.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("此 Java 環境不支援 SHA-256", ex);
		}
	}

	private void verifyApplicationServerKey(String applicationServerKey) {
		byte[] expected = decodeBase64Url(properties.getPublicKey(), "伺服器 VAPID 公鑰格式不正確");
		byte[] actual = decodeBase64Url(applicationServerKey, "瀏覽器推播公鑰格式不正確");
		if (!MessageDigest.isEqual(expected, actual)) {
			throw new IllegalArgumentException("瀏覽器訂閱仍使用舊的 VAPID 公鑰，請重新建立桌面通知訂閱");
		}
	}

	private byte[] decodeBase64Url(String value, String errorMessage) {
		try {
			return Base64.getUrlDecoder().decode(requireText(value, errorMessage));
		} catch (IllegalArgumentException ex) {
			throw new IllegalArgumentException(errorMessage, ex);
		}
	}

	private String requireText(String value, String message) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(message);
		}
		return value.trim();
	}

	private String trimToLength(String value, int maximumLength) {
		if (value == null || value.isBlank()) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.length() <= maximumLength ? trimmed : trimmed.substring(0, maximumLength);
	}
}
