package com.example.cramschool.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.cramschool.entity.AttendanceStatus;
import com.example.cramschool.form.StudentAttendanceForm;
import com.example.cramschool.service.ClassRoomService;
import com.example.cramschool.service.StudentAttendanceService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/classes/{classId}/attendance")
public class StudentAttendanceController {

	private final ClassRoomService classRoomService;
	private final StudentAttendanceService studentAttendanceService;

	public StudentAttendanceController(ClassRoomService classRoomService,
			StudentAttendanceService studentAttendanceService) {
		this.classRoomService = classRoomService;
		this.studentAttendanceService = studentAttendanceService;
	}

	@ModelAttribute
	public void addOptions(Model model) {
		model.addAttribute("statusOptions", AttendanceStatus.values());
	}

	@GetMapping
	public String form(@PathVariable Long classId,
			@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
			Model model) {
		LocalDate attendanceDate = date == null ? LocalDate.now() : date;
		model.addAttribute("pageTitle", "班級點名");
		model.addAttribute("classRoom", classRoomService.findById(classId));
		model.addAttribute("attendanceForm", studentAttendanceService.buildForm(classId, attendanceDate));
		return "classes/attendance";
	}

	@PostMapping
	public String save(@PathVariable Long classId,
			@Valid @ModelAttribute("attendanceForm") StudentAttendanceForm attendanceForm,
			BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			model.addAttribute("pageTitle", "班級點名");
			model.addAttribute("classRoom", classRoomService.findById(classId));
			return "classes/attendance";
		}

		studentAttendanceService.saveAttendance(classId, attendanceForm);
		redirectAttributes.addFlashAttribute("message", "已儲存點名紀錄");
		return "redirect:/classes/" + classId + "/attendance?date=" + attendanceForm.getAttendanceDate();
	}
}
