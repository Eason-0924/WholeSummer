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
import com.example.cramschool.service.TeacherSalaryService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/salary")
public class SalaryController {

	private final TeacherSalaryService teacherSalaryService;
	private final TeacherAccountService teacherAccountService;

	public SalaryController(TeacherSalaryService teacherSalaryService,
			TeacherAccountService teacherAccountService) {
		this.teacherSalaryService = teacherSalaryService;
		this.teacherAccountService = teacherAccountService;
	}

	@GetMapping
	public String index(@RequestParam(required = false) Integer year,
			@RequestParam(required = false) Integer month,
			HttpSession session, Model model) {
		YearMonth targetMonth = parseMonth(year, month);
		boolean director = isDirector(session);
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
			@RequestParam BigDecimal hourlyRate,
			@RequestParam(required = false) Integer year,
			@RequestParam(required = false) Integer month,
			HttpSession session,
			RedirectAttributes redirectAttributes) {
		if (!isDirector(session)) {
			redirectAttributes.addFlashAttribute("errorMessage", "只有主任可以設定教師時薪");
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

	private boolean isDirector(HttpSession session) {
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
