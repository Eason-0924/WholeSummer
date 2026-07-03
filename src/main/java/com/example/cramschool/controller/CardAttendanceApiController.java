package com.example.cramschool.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.cramschool.dto.CardCheckInRequest;
import com.example.cramschool.dto.CardCheckInResponse;
import com.example.cramschool.service.RecentCardCheckInService;
import com.example.cramschool.service.StudentAttendanceService;

@RestController
@RequestMapping("/api/attendance")
public class CardAttendanceApiController {

	private final StudentAttendanceService studentAttendanceService;
	private final RecentCardCheckInService recentCardCheckInService;

	public CardAttendanceApiController(StudentAttendanceService studentAttendanceService,
			RecentCardCheckInService recentCardCheckInService) {
		this.studentAttendanceService = studentAttendanceService;
		this.recentCardCheckInService = recentCardCheckInService;
	}

	@PostMapping("/card-check-in")
	public CardCheckInResponse cardCheckIn(@RequestBody CardCheckInRequest request) {
		CardCheckInResponse response = studentAttendanceService.cardCheckIn(request);
		recentCardCheckInService.record(request, response);
		return response;
	}
}
