package com.example.cramschool.controller;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.cramschool.dto.TeacherSalarySummary;
import com.example.cramschool.service.TeacherAccountService;
import com.example.cramschool.service.TeacherAttendanceService;
import com.example.cramschool.service.TeacherSalaryService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/salary")
public class SalaryController {

	private final TeacherSalaryService teacherSalaryService;
	private final TeacherAccountService teacherAccountService;
	private final TeacherAttendanceService teacherAttendanceService;

	public SalaryController(TeacherSalaryService teacherSalaryService,
			TeacherAccountService teacherAccountService,
			TeacherAttendanceService teacherAttendanceService) {
		this.teacherSalaryService = teacherSalaryService;
		this.teacherAccountService = teacherAccountService;
		this.teacherAttendanceService = teacherAttendanceService;
	}

	@GetMapping
	public String index(@RequestParam(required = false) Integer year,
			@RequestParam(required = false) Integer month,
			HttpSession session, Model model) {
		YearMonth targetMonth = parseMonth(year, month);
		boolean director = isActualDirector(session);
		Long currentTeacherId = currentTeacherId(session);
		List<TeacherSalarySummary> summaries = director
				? teacherSalaryService.calculateAll(targetMonth)
				: List.of(teacherSalaryService.calculate(currentTeacherId, targetMonth));

		model.addAttribute("pageTitle", "薪資試算");
		model.addAttribute("selectedYear", targetMonth.getYear());
		model.addAttribute("selectedMonth", targetMonth.getMonthValue());
		model.addAttribute("salaryYears", availableYears(targetMonth.getYear()));
		model.addAttribute("salarySummaries", summaries);
		model.addAttribute("salaryDirectorView", director);
		return "salary/index";
	}

	@PostMapping("/{teacherId}/hourly-rate")
	public String updateHourlyRate(@PathVariable Long teacherId,
			@RequestParam Integer hourlyRate,
			@RequestParam(required = false) Integer year,
			@RequestParam(required = false) Integer month,
			HttpSession session,
			RedirectAttributes redirectAttributes) {
		if (!isActualDirector(session)) {
			redirectAttributes.addFlashAttribute("errorMessage", "目前帳號沒有管理全體薪資的權限");
			return redirectToMonth(year, month);
		}
		try {
			YearMonth targetMonth = parseMonth(year, month);
			teacherSalaryService.updateHourlyRate(teacherId, targetMonth, hourlyRate);
			redirectAttributes.addFlashAttribute("message", "已更新該月份教師時薪");
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return redirectToMonth(year, month);
	}

	@PostMapping("/attendance/{attendanceId}/adjust")
	public String adjustAttendance(@PathVariable Long attendanceId,
			@RequestParam(required = false) String manualRemark,
			@RequestParam BigDecimal manualHours,
			@RequestParam Integer year,
			@RequestParam Integer month,
			HttpSession session,
			RedirectAttributes redirectAttributes) {
		try {
			var attendance = teacherAttendanceService.updateManualAdjustment(
					attendanceId, manualRemark, manualHours,
					currentTeacherId(session), isActualDirector(session));
			teacherSalaryService.calculate(attendance.getTeacher().getId(), parseMonth(year, month));
			redirectAttributes.addFlashAttribute("message", "已更新打卡紀錄並重新計算薪資");
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return redirectToMonth(year, month);
	}

	private boolean isActualDirector(HttpSession session) {
		Object accountId = session.getAttribute(AuthController.ACCOUNT_ID_SESSION_KEY);
		return accountId instanceof Long id && teacherAccountService.isDirector(id);
	}

	private Long currentTeacherId(HttpSession session) {
		Object teacherId = session.getAttribute(AuthController.TEACHER_ID_SESSION_KEY);
		if (teacherId instanceof Long id) {
			return id;
		}
		throw new IllegalArgumentException("找不到目前登入教師");
	}

	private YearMonth parseMonth(Integer year, Integer month) {
		try {
			return YearMonth.of(
					year == null ? YearMonth.now().getYear() : year,
					month == null ? YearMonth.now().getMonthValue() : month);
		} catch (RuntimeException ex) {
			return YearMonth.now();
		}
	}

	private List<Integer> availableYears(int selectedYear) {
		int currentYear = YearMonth.now().getYear();
		int firstYear = Math.min(currentYear - 5, selectedYear);
		int lastYear = Math.max(currentYear + 1, selectedYear);
		List<Integer> years = new ArrayList<>();
		for (int year = lastYear; year >= firstYear; year--) {
			years.add(year);
		}
		return years;
	}

	private String redirectToMonth(Integer year, Integer month) {
		YearMonth targetMonth = parseMonth(year, month);
		return "redirect:/salary?year=" + targetMonth.getYear()
				+ "&month=" + targetMonth.getMonthValue();
	}
}
