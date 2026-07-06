package com.example.cramschool.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.cramschool.service.CurrentUserService;
import com.example.cramschool.service.StudentLeaveManagementService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/student-leaves")
public class StudentLeaveManagementController {

	private final StudentLeaveManagementService studentLeaveManagementService;
	private final CurrentUserService currentUserService;

	public StudentLeaveManagementController(StudentLeaveManagementService studentLeaveManagementService,
			CurrentUserService currentUserService) {
		this.studentLeaveManagementService = studentLeaveManagementService;
		this.currentUserService = currentUserService;
	}

	@GetMapping
	public String index(Model model) {
		model.addAttribute("pageTitle", "學生請假");
		model.addAttribute("pendingLeaveRequests", studentLeaveManagementService.findPendingRequests());
		model.addAttribute("confirmedLeaveRequests", studentLeaveManagementService.findConfirmedRequests());
		return "student-leaves/index";
	}

	@PostMapping("/{requestId}/confirm")
	public String confirm(@PathVariable Long requestId, HttpSession session,
			RedirectAttributes redirectAttributes) {
		try {
			studentLeaveManagementService.confirm(requestId, currentUserService.currentTeacherId(session));
			redirectAttributes.addFlashAttribute("message", "已確認請假紀錄，並通知家長。");
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return "redirect:/student-leaves";
	}
}
