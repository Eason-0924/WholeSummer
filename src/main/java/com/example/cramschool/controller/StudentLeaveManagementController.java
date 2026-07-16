package com.example.cramschool.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.cramschool.service.CurrentUserService;
import com.example.cramschool.service.StudentLeaveManagementService;
import com.example.cramschool.service.StudentAttendanceService;
import com.example.cramschool.form.StudentAttendanceForm;
import com.example.cramschool.entity.AttendanceStatus;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/student-leaves")
public class StudentLeaveManagementController {

	private final StudentLeaveManagementService studentLeaveManagementService;
	private final CurrentUserService currentUserService;
	private final StudentAttendanceService studentAttendanceService;

	public StudentLeaveManagementController(StudentLeaveManagementService studentLeaveManagementService,
			CurrentUserService currentUserService, StudentAttendanceService studentAttendanceService) {
		this.studentLeaveManagementService = studentLeaveManagementService;
		this.currentUserService = currentUserService;
		this.studentAttendanceService = studentAttendanceService;
	}

	@ModelAttribute
	public void addAttendanceOptions(Model model) { model.addAttribute("statusOptions", AttendanceStatus.values()); }

	@GetMapping
	public String index(Model model) {
		LocalDate date = LocalDate.now();
		populateModel(model, date);
		return "student-leaves/index";
	}

	@GetMapping(params = "date")
	public String date(@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date, Model model) {
		populateModel(model, date);
		return "student-leaves/index";
	}

	@PostMapping("/attendance")
	public String saveAttendance(@Valid @ModelAttribute("attendanceForm") StudentAttendanceForm form,
			BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			populateLeaveModel(model);
			model.addAttribute("pageTitle", "學生出席");
			model.addAttribute("attendanceDate", form.getAttendanceDate());
			return "student-leaves/index";
		}
		studentAttendanceService.saveDailyAttendance(form);
		redirectAttributes.addFlashAttribute("message", "已儲存學生出席紀錄");
		return "redirect:/student-leaves?date=" + form.getAttendanceDate();
	}

	private void populateModel(Model model, LocalDate date) {
		model.addAttribute("pageTitle", "學生出席");
		model.addAttribute("attendanceForm", studentAttendanceService.buildDailyForm(date));
		model.addAttribute("attendanceDate", date);
		populateLeaveModel(model);
	}

	private void populateLeaveModel(Model model) {
		model.addAttribute("pendingLeaveRequests", studentLeaveManagementService.findPendingRequests());
		model.addAttribute("confirmedLeaveRequests", studentLeaveManagementService.findConfirmedRequests());
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
