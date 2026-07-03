package com.example.cramschool.controller;

import java.nio.file.Path;
import java.time.YearMonth;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.cramschool.service.AnalysisAttendanceExportService;
import com.example.cramschool.service.AnalysisHomeworkExportService;

@Controller
@RequestMapping("/analysis")
public class AnalysisExportController {

	private final AnalysisAttendanceExportService analysisAttendanceExportService;
	private final AnalysisHomeworkExportService analysisHomeworkExportService;

	public AnalysisExportController(AnalysisAttendanceExportService analysisAttendanceExportService,
			AnalysisHomeworkExportService analysisHomeworkExportService) {
		this.analysisAttendanceExportService = analysisAttendanceExportService;
		this.analysisHomeworkExportService = analysisHomeworkExportService;
	}

	@PostMapping("/attendance/export")
	public String exportAttendance(@RequestParam(defaultValue = "month") String attendanceRange,
			@RequestParam(required = false) Integer attendanceYear,
			@RequestParam(required = false) Integer attendanceMonth,
			@RequestParam(defaultValue = "month") String homeworkRange,
			@RequestParam(required = false) Integer homeworkYear,
			@RequestParam(required = false) Integer homeworkMonth,
			@RequestParam(defaultValue = "attendance") String activePanel,
			RedirectAttributes redirectAttributes) {
		YearMonth targetMonth = "all".equalsIgnoreCase(attendanceRange) ? null : resolveMonth(attendanceYear, attendanceMonth);
		try {
			Path file = analysisAttendanceExportService.exportAndOpenFolder(targetMonth);
			redirectAttributes.addFlashAttribute("message", "已匯出分析資料：" + file.getFileName());
		} catch (IllegalArgumentException | IllegalStateException | java.io.UncheckedIOException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return redirectTarget(attendanceRange, attendanceYear, attendanceMonth, homeworkRange, homeworkYear, homeworkMonth,
				activePanel);
	}

	@PostMapping("/homework/export")
	public String exportHomework(@RequestParam(defaultValue = "month") String attendanceRange,
			@RequestParam(required = false) Integer attendanceYear,
			@RequestParam(required = false) Integer attendanceMonth,
			@RequestParam(defaultValue = "month") String homeworkRange,
			@RequestParam(required = false) Integer homeworkYear,
			@RequestParam(required = false) Integer homeworkMonth,
			@RequestParam(defaultValue = "homework") String activePanel,
			RedirectAttributes redirectAttributes) {
		YearMonth targetMonth = "all".equalsIgnoreCase(homeworkRange) ? null : resolveMonth(homeworkYear, homeworkMonth);
		try {
			Path file = analysisHomeworkExportService.exportAndOpenFolder(targetMonth);
			redirectAttributes.addFlashAttribute("message", "已匯出分析資料：" + file.getFileName());
		} catch (IllegalArgumentException | IllegalStateException | java.io.UncheckedIOException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return redirectTarget(attendanceRange, attendanceYear, attendanceMonth, homeworkRange, homeworkYear, homeworkMonth,
				activePanel);
	}

	private YearMonth resolveMonth(Integer year, Integer month) {
		YearMonth now = YearMonth.now();
		if (year == null || month == null) {
			return now;
		}
		try {
			return YearMonth.of(year, month);
		} catch (RuntimeException ex) {
			return now;
		}
	}

	private String redirectTarget(String attendanceRange, Integer attendanceYear, Integer attendanceMonth,
			String homeworkRange, Integer homeworkYear, Integer homeworkMonth, String activePanel) {
		String normalizedAttendanceRange = "all".equalsIgnoreCase(attendanceRange) ? "all" : "month";
		String normalizedHomeworkRange = "all".equalsIgnoreCase(homeworkRange) ? "all" : "month";
		YearMonth attendanceTarget = resolveMonth(attendanceYear, attendanceMonth);
		YearMonth homeworkTarget = resolveMonth(homeworkYear, homeworkMonth);
		return "redirect:/analysis?attendanceRange=" + normalizedAttendanceRange
				+ "&year=" + attendanceTarget.getYear()
				+ "&month=" + attendanceTarget.getMonthValue()
				+ "&homeworkRange=" + normalizedHomeworkRange
				+ "&homeworkYear=" + homeworkTarget.getYear()
				+ "&homeworkMonth=" + homeworkTarget.getMonthValue()
				+ "&activePanel=" + ("homework".equalsIgnoreCase(activePanel) ? "homework" : "attendance");
	}
}
