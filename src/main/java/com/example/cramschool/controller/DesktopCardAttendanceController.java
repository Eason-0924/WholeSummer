package com.example.cramschool.controller;

import java.net.InetAddress;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.cramschool.dto.CardCheckInRequest;
import com.example.cramschool.dto.CardCheckInResponse;
import com.example.cramschool.service.RecentCardCheckInService;
import com.example.cramschool.service.StudentAttendanceService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/internal/desktop")
public class DesktopCardAttendanceController {

	private final StudentAttendanceService studentAttendanceService;
	private final RecentCardCheckInService recentCardCheckInService;

	public DesktopCardAttendanceController(StudentAttendanceService studentAttendanceService,
			RecentCardCheckInService recentCardCheckInService) {
		this.studentAttendanceService = studentAttendanceService;
		this.recentCardCheckInService = recentCardCheckInService;
	}

	@PostMapping("/card-check-in")
	public CardCheckInResponse cardCheckIn(@RequestBody CardCheckInRequest request,
			HttpServletRequest httpRequest) {
		if (!isLoopback(httpRequest.getRemoteAddr())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只允許本機刷卡程式呼叫");
		}
		CardCheckInResponse response = studentAttendanceService.cardCheckIn(request);
		recentCardCheckInService.record(request, response);
		return response;
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
