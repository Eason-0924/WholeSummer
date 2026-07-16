package com.example.cramschool.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.example.cramschool.config.LineProperties;
import com.example.cramschool.service.LineMessageRouter;
import com.example.cramschool.service.LineMessageService;
import com.example.cramschool.service.LineSignatureValidator;
import com.example.cramschool.service.LineReplyDeliveryService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
public class LineWebhookController {

	private static final Logger log = LoggerFactory.getLogger(LineWebhookController.class);

	private final LineProperties lineProperties;
	private final LineSignatureValidator signatureValidator;
	private final LineMessageRouter lineMessageRouter;
	private final LineMessageService lineMessageService;
	private final LineReplyDeliveryService lineReplyDeliveryService;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public LineWebhookController(LineProperties lineProperties,
			LineSignatureValidator signatureValidator, LineMessageRouter lineMessageRouter,
			LineMessageService lineMessageService) {
		this(lineProperties, signatureValidator, lineMessageRouter, lineMessageService, null);
	}

	@Autowired
	public LineWebhookController(LineProperties lineProperties,
			LineSignatureValidator signatureValidator, LineMessageRouter lineMessageRouter,
			LineMessageService lineMessageService, LineReplyDeliveryService lineReplyDeliveryService) {
		this.lineProperties = lineProperties;
		this.signatureValidator = signatureValidator;
		this.lineMessageRouter = lineMessageRouter;
		this.lineMessageService = lineMessageService;
		this.lineReplyDeliveryService = lineReplyDeliveryService;
	}

	@PostMapping("${line.webhook-path:/api/line/webhook}")
	public ResponseEntity<String> receiveWebhook(
			@RequestBody(required = false) String body,
			@RequestHeader(name = "x-line-signature", required = false) String signature) {
		if (!lineProperties.isEnabled()) {
			log.info("LINE webhook received while LINE integration is disabled.");
			return ResponseEntity.ok("LINE integration disabled");
		}
		if (!signatureValidator.isConfigured()) {
			log.warn("LINE webhook received, but channel secret is not configured.");
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
					.body("LINE channel secret is not configured");
		}
		if (!signatureValidator.isValid(body, signature)) {
			log.warn("Rejected LINE webhook because signature verification failed.");
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid signature");
		}

		handleEvents(body);
		return ResponseEntity.ok("OK");
	}

	private void handleEvents(String body) {
		try {
			JsonNode root = objectMapper.readTree(body == null ? "{}" : body);
			JsonNode events = root.path("events");
			if (!events.isArray()) {
				return;
			}
			for (JsonNode event : events) {
				handleEvent(event);
			}
		} catch (Exception ex) {
			log.warn("Failed to process LINE webhook body.", ex);
		}
	}

	private void handleEvent(JsonNode event) {
		if (!"message".equals(event.path("type").asText())
				|| !"text".equals(event.path("message").path("type").asText())) {
			return;
		}
		String lineUserId = event.path("source").path("userId").asText(null);
		String replyToken = event.path("replyToken").asText(null);
		String messageText = event.path("message").path("text").asText("");
		String displayName = isPotentialBindCommand(messageText)
				? lineMessageService.getProfileDisplayName(lineUserId).orElse(null)
				: null;
		lineMessageRouter.routeTextMessage(lineUserId, displayName, messageText)
				.ifPresent(reply -> {
					if (lineReplyDeliveryService == null) {
						lineMessageService.replyText(replyToken, reply);
					} else {
						lineReplyDeliveryService.send(lineUserId, replyToken, reply);
					}
				});
		
	}

	private boolean isPotentialBindCommand(String messageText) {
		return messageText != null && messageText.trim().startsWith("綁定");
	}
	
}
