package com.example.cramschool.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.cramschool.service.CurrentUserService;
import com.example.cramschool.service.LeaveService;
import com.example.cramschool.service.TeacherAttendanceService;

import jakarta.servlet.http.HttpSession;

@Controller
public class MyAttendanceController {

	private final TeacherAttendanceService teacherAttendanceService;
	private final LeaveService leaveService;
	private final CurrentUserService currentUserService;

	public MyAttendanceController(TeacherAttendanceService teacherAttendanceService,
			LeaveService leaveService,
			CurrentUserService currentUserService) {
		this.teacherAttendanceService = teacherAttendanceService;
		this.leaveService = leaveService;
		this.currentUserService = currentUserService;
	}

	@GetMapping("/attendance/my")
	public String myAttendance(Model model, HttpSession session) {
		Long teacherId = currentUserService.currentTeacherId(session);
		LocalDate today = LocalDate.now();
		model.addAttribute("pageTitle", "我的打卡");
		model.addAttribute("attendanceDate", today);
		model.addAttribute("attendanceDirectorView", false);
		model.addAttribute("currentTeacherId", teacherId);
		model.addAttribute("currentTeacherName", currentUserService.currentTeacherName(session));
		model.addAttribute("attendances", teacherAttendanceService.findByTeacherIdAndDate(teacherId, today)
				.map(attendance -> List.of(attendance))
				.orElseGet(List::of));
		model.addAttribute("leaveDate", today);
		model.addAttribute("leaveCourseOptions", leaveService.findCourseOptions(teacherId));
		model.addAttribute("teacherLeaves", leaveService.findByTeacherAndDate(teacherId, today));
		return "teachers/attendance";
	}

	@PostMapping("/attendance/quick-clock")
	public String quickClock(HttpSession session, RedirectAttributes redirectAttributes) {
		try {
			teacherAttendanceService.quickClock(currentUserService.currentTeacherId(session));
			redirectAttributes.addFlashAttribute("message", "已完成快速打卡");
		} catch (IllegalArgumentException | IllegalStateException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return "redirect:/attendance/my";
	}

	@PostMapping("/attendance/leave")
	public String leave(@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate leaveDate,
			@RequestParam(required = false) Long courseScheduleId,
			@RequestParam(required = false) String reason,
			HttpSession session,
			RedirectAttributes redirectAttributes) {
		try {
			leaveService.createLeave(currentUserService.currentTeacherId(session),
					leaveDate, courseScheduleId, reason);
			redirectAttributes.addFlashAttribute("message", "已建立請假紀錄");
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return "redirect:/attendance/my";
	}
}
