package com.example.cramschool.controller;

import java.net.InetAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.cramschool.dto.CardCheckInRequest;
import com.example.cramschool.dto.CardCheckInResponse;
import com.example.cramschool.service.CardBindingModeService;
import com.example.cramschool.service.CardIdNormalizer;
import com.example.cramschool.service.RecentCardCheckInService;
import com.example.cramschool.service.StudentAttendanceService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/internal/desktop")
public class DesktopCardAttendanceController {

	private static final Logger logger = LoggerFactory.getLogger(DesktopCardAttendanceController.class);

	private final StudentAttendanceService studentAttendanceService;
	private final RecentCardCheckInService recentCardCheckInService;
	private final CardBindingModeService cardBindingModeService;

	public DesktopCardAttendanceController(StudentAttendanceService studentAttendanceService,
			RecentCardCheckInService recentCardCheckInService,
			CardBindingModeService cardBindingModeService) {
		this.studentAttendanceService = studentAttendanceService;
		this.recentCardCheckInService = recentCardCheckInService;
		this.cardBindingModeService = cardBindingModeService;
	}

	@PostMapping("/card-check-in")
	public CardCheckInResponse cardCheckIn(@RequestBody CardCheckInRequest request,
			HttpServletRequest httpRequest) {
		if (!isLoopback(httpRequest.getRemoteAddr())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只允許本機刷卡程式呼叫");
		}
		try {
			CardCheckInResponse response = cardBindingModeService.completeIfPending(request)
					.orElseGet(() -> studentAttendanceService.cardCheckIn(request));
			recentCardCheckInService.record(request, response);
			return response;
		} catch (RuntimeException ex) {
			logger.warn("Desktop card check-in failed. cardId={}, deviceName={}",
					CardIdNormalizer.normalize(request == null ? null : request.getCardId()),
					request == null ? null : request.getDeviceName(), ex);
			CardCheckInResponse response = CardCheckInResponse.fail("SERVER_ERROR",
					"刷卡處理失敗：" + safeMessage(ex));
			response.setCardId(CardIdNormalizer.normalize(request == null ? null : request.getCardId()));
			recentCardCheckInService.record(request, response);
			return response;
		}
	}

	private String safeMessage(RuntimeException ex) {
		if (ex.getMessage() == null || ex.getMessage().isBlank()) {
			return "請查看 WholeSummer 操作紀錄或系統紀錄";
		}
		return ex.getMessage();
	}

	private boolean isLoopback(String remoteAddress) {
		if (remoteAddress == null || remoteAddress.isBlank()) {
			return false;
		}
		try {
			InetAddress address = InetAddress.getByName(remoteAddress);
			return address.isLoopbackAddress();
		} catch (Exception ex) {
			return false;
		}
	}
}
