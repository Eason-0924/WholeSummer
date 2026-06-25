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
import com.example.cramschool.entity.TeacherPermissionType;
import com.example.cramschool.form.TeacherAttendanceForm;
import com.example.cramschool.service.TeacherAccountService;
import com.example.cramschool.service.TeacherAttendanceService;
import com.example.cramschool.service.TeacherService;
import com.example.cramschool.service.TeacherPermissionService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/teachers/attendance")
public class TeacherAttendanceController {

	private final TeacherAttendanceService teacherAttendanceService;
	private final TeacherService teacherService;
	private final TeacherAccountService teacherAccountService;
	private final TeacherPermissionService teacherPermissionService;

	public TeacherAttendanceController(TeacherAttendanceService teacherAttendanceService,
			TeacherService teacherService, TeacherAccountService teacherAccountService,
			TeacherPermissionService teacherPermissionService) {
		this.teacherAttendanceService = teacherAttendanceService;
		this.teacherService = teacherService;
		this.teacherAccountService = teacherAccountService;
		this.teacherPermissionService = teacherPermissionService;
	}

	@ModelAttribute
	public void addOptions(Model model, HttpSession session) {
		boolean director = isDirector(session);
		Long teacherId = currentTeacherId(session);
		model.addAttribute("teacherOptions", director
				? teacherService.findActiveTeachers()
				: List.of(teacherService.findById(teacherId)));
		model.addAttribute("statusOptions", List.of(
				TeacherAttendanceStatus.WORKING,
				TeacherAttendanceStatus.LEAVE,
				TeacherAttendanceStatus.ABSENT));
		model.addAttribute("attendanceDirectorView", director);
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
			HttpSession session,
			RedirectAttributes redirectAttributes) {
		if (!isDirector(session)) {
			redirectAttributes.addFlashAttribute("errorMessage", "目前帳號沒有手動登記所有教師出勤的權限");
			return "redirect:/teachers/attendance";
		}
		try {
			teacherAttendanceService.save(form);
			redirectAttributes.addFlashAttribute("message", "已儲存教師出勤紀錄");
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return "redirect:/teachers/attendance?date=" + form.getDate();
	}

	@PostMapping("/clock-in")
	public String clockIn(@RequestParam Long teacherId, HttpSession session,
			RedirectAttributes redirectAttributes) {
		teacherAttendanceService.clockIn(quickAttendanceTeacherId(teacherId, session));
		redirectAttributes.addFlashAttribute("message", "已完成上班打卡");
		return "redirect:/teachers/attendance";
	}

	@PostMapping("/clock-out")
	public String clockOut(@RequestParam Long teacherId, HttpSession session,
			RedirectAttributes redirectAttributes) {
		try {
			teacherAttendanceService.clockOut(quickAttendanceTeacherId(teacherId, session));
			redirectAttributes.addFlashAttribute("message", "已完成下班打卡");
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return "redirect:/teachers/attendance";
	}

	@PostMapping("/leave")
	public String leave(@RequestParam Long teacherId, HttpSession session,
			RedirectAttributes redirectAttributes) {
		teacherAttendanceService.markLeave(quickAttendanceTeacherId(teacherId, session));
		redirectAttributes.addFlashAttribute("message", "已登記教師請假");
		return "redirect:/teachers/attendance";
	}

	@PostMapping("/absent")
	public String absent(@RequestParam Long teacherId, HttpSession session,
			RedirectAttributes redirectAttributes) {
		teacherAttendanceService.markAbsent(quickAttendanceTeacherId(teacherId, session));
		redirectAttributes.addFlashAttribute("message", "已登記教師缺勤");
		return "redirect:/teachers/attendance";
	}

	private Long quickAttendanceTeacherId(Long requestedTeacherId, HttpSession session) {
		return isDirector(session) ? requestedTeacherId : currentTeacherId(session);
	}

	private boolean isDirector(HttpSession session) {
		return teacherPermissionService.hasPermission(
				currentTeacherId(session), TeacherPermissionType.MANAGE_ALL_ATTENDANCE);
	}

	private Long currentTeacherId(HttpSession session) {
		Object teacherId = session.getAttribute(AuthController.TEACHER_ID_SESSION_KEY);
		if (teacherId instanceof Long id) {
			return id;
		}
		throw new IllegalArgumentException("找不到目前登入教師");
	}
}
