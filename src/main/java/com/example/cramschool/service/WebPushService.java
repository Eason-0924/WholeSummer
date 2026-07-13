package com.example.cramschool.service;

import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Collection;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.cramschool.config.WebPushProperties;
import com.example.cramschool.dto.WebPushPayload;
import com.example.cramschool.dto.WebPushSendResult;
import com.example.cramschool.entity.WebPushSubscription;
import com.example.cramschool.repository.WebPushSubscriptionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.Encoding;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;

@Service
public class WebPushService {

	private static final int MAX_FAILURE_COUNT = 5;
	private static final Logger log = LoggerFactory.getLogger(WebPushService.class);

	private final WebPushProperties properties;
	private final WebPushSubscriptionService subscriptionService;
	private final WebPushSubscriptionRepository subscriptionRepository;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public WebPushService(WebPushProperties properties,
			WebPushSubscriptionService subscriptionService,
			WebPushSubscriptionRepository subscriptionRepository) {
		this.properties = properties;
		this.subscriptionService = subscriptionService;
		this.subscriptionRepository = subscriptionRepository;
	}

	public WebPushSendResult sendToUser(Long userId, WebPushPayload payload) {
		if (!properties.isConfigured()) {
			return new WebPushSendResult(false, 0, 0, 0);
		}
		List<WebPushSubscription> subscriptions = subscriptionService.findEnabledByUser(userId,
				subscriptionService.valueHash(properties.getPublicKey()));
		return sendToSubscriptions(subscriptions, payload);
	}

	public WebPushSendResult sendToUsers(Collection<Long> userIds, WebPushPayload payload) {
		if (userIds == null || userIds.isEmpty()) {
			return new WebPushSendResult(properties.isConfigured(), 0, 0, 0);
		}
		int successCount = 0;
		int failureCount = 0;
		int skippedCount = 0;
		for (Long userId : userIds.stream().filter(id -> id != null).distinct().toList()) {
			WebPushSendResult result = sendToUser(userId, payload);
			successCount += result.successCount();
			failureCount += result.failureCount();
			skippedCount += result.skippedCount();
		}
		return new WebPushSendResult(properties.isConfigured(), successCount, failureCount, skippedCount);
	}

	public WebPushSendResult sendTestNotification(Long userId) {
		return sendToUser(userId, new WebPushPayload(
				"WholeSummer 桌面通知測試",
				"如果您看到這則通知，代表桌面通知已成功啟用。",
				"/",
				"/icons/icon-192.png"));
	}

	private WebPushSendResult sendToSubscriptions(List<WebPushSubscription> subscriptions, WebPushPayload payload) {
		if (subscriptions.isEmpty()) {
			return new WebPushSendResult(true, 0, 0, 1);
		}
		int successCount = 0;
		int failureCount = 0;
		int skippedCount = 0;
		String jsonPayload = toJson(payload);
		for (WebPushSubscription subscription : subscriptions) {
			try {
				PushDeliveryResponse delivery = sendOne(subscription, jsonPayload);
				if (delivery.statusCode() >= 200 && delivery.statusCode() < 300) {
					markSuccess(subscription);
					successCount++;
				} else {
					log.warn("Web Push delivery failed for subscription {} with HTTP status {}: {}",
							subscription.getId(), delivery.statusCode(), delivery.responseBody());
					markFailure(subscription, delivery.statusCode() == 404 || delivery.statusCode() == 410);
					failureCount++;
				}
			} catch (Exception ex) {
				log.warn("Web Push delivery failed for subscription {}", subscription.getId(), ex);
				markFailure(subscription, false);
				failureCount++;
			}
		}
		return new WebPushSendResult(true, successCount, failureCount, skippedCount);
	}

	private PushDeliveryResponse sendOne(WebPushSubscription subscription, String payload) throws Exception {
		Subscription webPushSubscription = new Subscription(subscription.getEndpoint(),
				new Subscription.Keys(subscription.getP256dh(), subscription.getAuth()));
		Notification notification = new Notification(webPushSubscription, payload);
		HttpResponse response = pushService().send(notification, Encoding.AES128GCM);
		try {
			String responseBody = response.getEntity() == null ? "" :
					EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
			return new PushDeliveryResponse(response.getStatusLine().getStatusCode(), summarizeResponse(responseBody));
		} finally {
			EntityUtils.consumeQuietly(response.getEntity());
		}
	}

	private String summarizeResponse(String responseBody) {
		if (responseBody == null || responseBody.isBlank()) {
			return "(empty response)";
		}
		String normalized = responseBody.replaceAll("[\\r\\n]+", " ").trim();
		return normalized.length() <= 500 ? normalized : normalized.substring(0, 500) + "...";
	}

	private PushService pushService() throws Exception {
		return new PushService(properties.getPublicKey(), properties.getPrivateKey(), properties.getSubject());
	}

	private String toJson(WebPushPayload payload) {
		try {
			return objectMapper.writeValueAsString(payload);
		} catch (JsonProcessingException ex) {
			throw new IllegalArgumentException("無法建立推播通知內容", ex);
		}
	}

	private void markSuccess(WebPushSubscription subscription) {
		subscription.setLastSuccessAt(LocalDateTime.now());
		subscription.setLastFailureAt(null);
		subscription.setFailureCount(0);
		subscriptionRepository.save(subscription);
	}

	private void markFailure(WebPushSubscription subscription, boolean definitelyExpired) {
		subscription.setLastFailureAt(LocalDateTime.now());
		subscription.setFailureCount(subscription.getFailureCount() + 1);
		if (definitelyExpired || subscription.getFailureCount() >= MAX_FAILURE_COUNT) {
			subscription.setEnabled(false);
		}
		subscriptionRepository.save(subscription);
	}

	private record PushDeliveryResponse(int statusCode, String responseBody) {
	}
}
