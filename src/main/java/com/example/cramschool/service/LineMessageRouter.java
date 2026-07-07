package com.example.cramschool.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.cramschool.dto.LineBindingReply;

@Service
public class LineMessageRouter {

	private static final String QUERY_WEEKLY_SCHEDULE_COMMAND = "查詢課表";

	private final LineBindingService lineBindingService;
	private final LineScheduleService lineScheduleService;

	public LineMessageRouter(LineBindingService lineBindingService, LineScheduleService lineScheduleService) {
		this.lineBindingService = lineBindingService;
		this.lineScheduleService = lineScheduleService;
	}

	public Optional<String> routeTextMessage(String lineUserId, String lineDisplayName, String messageText) {
		String normalizedText = messageText == null ? "" : messageText.trim();
		if (QUERY_WEEKLY_SCHEDULE_COMMAND.equals(normalizedText)) {
			return Optional.of(lineScheduleService.buildWeeklyScheduleReply(lineUserId));
		}

		LineBindingReply bindingReply = lineBindingService.bindFromLineMessage(
				lineUserId, lineDisplayName, messageText);
		return bindingReply.handled()
				? Optional.of(bindingReply.message())
				: Optional.empty();
	}
}
