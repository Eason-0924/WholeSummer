package com.example.cramschool.controller;

import com.example.cramschool.config.WebPushProperties;
import com.example.cramschool.dto.PushSubscriptionRequest;
import com.example.cramschool.dto.PushSubscriptionResponse;
import com.example.cramschool.dto.PushUnsubscribeRequest;
import com.example.cramschool.dto.VapidPublicKeyResponse;
import com.example.cramschool.dto.WebPushSendResult;
import com.example.cramschool.service.CurrentUserService;
import com.example.cramschool.service.WebPushService;
import com.example.cramschool.service.WebPushSubscriptionService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/push")
public class PushNotificationController {

	private final WebPushProperties webPushProperties;
	private final CurrentUserService currentUserService;
	private final WebPushSubscriptionService subscriptionService;
	private final WebPushService webPushService;

	public PushNotificationController(WebPushProperties webPushProperties,
			CurrentUserService currentUserService,
			WebPushSubscriptionService subscriptionService,
			WebPushService webPushService) {
		this.webPushProperties = webPushProperties;
		this.currentUserService = currentUserService;
		this.subscriptionService = subscriptionService;
		this.webPushService = webPushService;
	}

	@GetMapping("/vapid-public-key")
	public VapidPublicKeyResponse vapidPublicKey() {
		return new VapidPublicKeyResponse(webPushProperties.getPublicKey(), webPushProperties.isConfigured());
	}

	@PostMapping("/subscribe")
	public PushSubscriptionResponse subscribe(@Valid @RequestBody PushSubscriptionRequest request,
			HttpSession session, HttpServletRequest servletRequest) {
		Long userId = currentUserService.currentTeacherId(session);
		subscriptionService.subscribe(userId, request, servletRequest.getHeader("User-Agent"));
		return new PushSubscriptionResponse(true, true);
	}

	@PostMapping("/unsubscribe")
	public ResponseEntity<PushSubscriptionResponse> unsubscribe(@Valid @RequestBody PushUnsubscribeRequest request,
			HttpSession session) {
		Long userId = currentUserService.currentTeacherId(session);
		boolean found = subscriptionService.unsubscribe(userId, request.getEndpoint());
		return ResponseEntity.ok(new PushSubscriptionResponse(true, found));
	}

	@PostMapping("/test")
	public WebPushSendResult test(HttpSession session) {
		Long userId = currentUserService.currentTeacherId(session);
		return webPushService.sendTestNotification(userId);
	}
}
