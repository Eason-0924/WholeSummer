package com.example.cramschool.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.cramschool.dto.AvailableLessonRequest;
import com.example.cramschool.dto.AvailableLessonResponse;
import com.example.cramschool.dto.CreateLiffLeaveRequest;
import com.example.cramschool.dto.LiffLeaveRequestResponse;
import com.example.cramschool.dto.LiffMeResponse;
import com.example.cramschool.dto.LiffStudentResponse;
import com.example.cramschool.dto.LiffTokenRequest;
import com.example.cramschool.service.LineStudentLeaveService;

@RestController
@RequestMapping("/api/line/liff")
public class LineLiffApiController {

	private final LineStudentLeaveService lineStudentLeaveService;

	public LineLiffApiController(LineStudentLeaveService lineStudentLeaveService) {
		this.lineStudentLeaveService = lineStudentLeaveService;
	}

	@PostMapping("/me")
	public LiffMeResponse me(@RequestBody LiffTokenRequest request) {
		return lineStudentLeaveService.currentUser(request.idToken());
	}

	@PostMapping("/students")
	public LiffStudentResponse students(@RequestBody LiffTokenRequest request) {
		return lineStudentLeaveService.findStudents(request.idToken());
	}

	@PostMapping("/students/{studentId}/available-lessons")
	public AvailableLessonResponse availableLessons(@PathVariable Long studentId,
			@RequestBody AvailableLessonRequest request) {
		return lineStudentLeaveService.findAvailableLessons(studentId, request);
	}

	@PostMapping("/leave-requests")
	public LiffLeaveRequestResponse createLeaveRequest(@RequestBody CreateLiffLeaveRequest request) {
		return lineStudentLeaveService.createLeaveRequest(request);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
		return ResponseEntity.badRequest().body(Map.of(
				"success", false,
				"message", ex.getMessage()));
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<Map<String, Object>> handleServiceUnavailable(IllegalStateException ex) {
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
				"success", false,
				"message", ex.getMessage()));
	}
}
