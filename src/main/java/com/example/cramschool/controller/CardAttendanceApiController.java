package com.example.cramschool.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.cramschool.dto.CardCheckInRequest;
import com.example.cramschool.dto.CardCheckInResponse;
import com.example.cramschool.service.StudentAttendanceService;

@RestController
@RequestMapping("/api/attendance")
public class CardAttendanceApiController {

	private final StudentAttendanceService studentAttendanceService;

	public CardAttendanceApiController(StudentAttendanceService studentAttendanceService) {
		this.studentAttendanceService = studentAttendanceService;
	}

	@PostMapping("/card-check-in")
	public CardCheckInResponse cardCheckIn(@RequestBody CardCheckInRequest request) {
		return studentAttendanceService.cardCheckIn(request);
	}
}
