package com.example.cramschool.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.cramschool.entity.TeacherAttendanceStatus;
import com.example.cramschool.form.TeacherAttendanceForm;
import com.example.cramschool.service.TeacherAttendanceService;
import com.example.cramschool.service.TeacherService;

@Controller
@RequestMapping("/teachers/attendance")
public class TeacherAttendanceController {

	private final TeacherAttendanceService teacherAttendanceService;
	private final TeacherService teacherService;

	public TeacherAttendanceController(TeacherAttendanceService teacherAttendanceService, TeacherService teacherService) {
		this.teacherAttendanceService = teacherAttendanceService;
		this.teacherService = teacherService;
	}

	@ModelAttribute
	public void addOptions(Model model) {
		model.addAttribute("teacherOptions", teacherService.findActiveTeachers());
		model.addAttribute("statusOptions", List.of(
				TeacherAttendanceStatus.WORKING,
				TeacherAttendanceStatus.LEAVE,
				TeacherAttendanceStatus.ABSENT));
	}

	@GetMapping
	public String index(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
			Model model) {
		LocalDate targetDate = date == null ? LocalDate.now() : date;
		TeacherAttendanceForm form = new TeacherAttendanceForm();
		form.setDate(targetDate);
		model.addAttribute("pageTitle", "教師出勤");
		model.addAttribute("attendanceDate", targetDate);
		model.addAttribute("attendanceForm", form);
		model.addAttribute("attendances", teacherAttendanceService.findByDate(targetDate));
		return "teachers/attendance";
	}

	@PostMapping
	public String save(@ModelAttribute("attendanceForm") TeacherAttendanceForm form,
			RedirectAttributes redirectAttributes) {
		try {
			teacherAttendanceService.save(form);
			redirectAttributes.addFlashAttribute("message", "已儲存教師出勤紀錄");
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return "redirect:/teachers/attendance?date=" + form.getDate();
	}

	@PostMapping("/clock-in")
	public String clockIn(@RequestParam Long teacherId, RedirectAttributes redirectAttributes) {
		teacherAttendanceService.clockIn(teacherId);
		redirectAttributes.addFlashAttribute("message", "已完成上班打卡");
		return "redirect:/teachers/attendance";
	}

	@PostMapping("/clock-out")
	public String clockOut(@RequestParam Long teacherId, RedirectAttributes redirectAttributes) {
		try {
			teacherAttendanceService.clockOut(teacherId);
			redirectAttributes.addFlashAttribute("message", "已完成下班打卡");
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return "redirect:/teachers/attendance";
	}

	@PostMapping("/leave")
	public String leave(@RequestParam Long teacherId, RedirectAttributes redirectAttributes) {
		teacherAttendanceService.markLeave(teacherId);
		redirectAttributes.addFlashAttribute("message", "已登記教師請假");
		return "redirect:/teachers/attendance";
	}

	@PostMapping("/absent")
	public String absent(@RequestParam Long teacherId, RedirectAttributes redirectAttributes) {
		teacherAttendanceService.markAbsent(teacherId);
		redirectAttributes.addFlashAttribute("message", "已登記教師缺勤");
		return "redirect:/teachers/attendance";
	}
}
