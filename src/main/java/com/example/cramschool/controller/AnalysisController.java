package com.example.cramschool.controller;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.cramschool.dto.ScoreStats;
import com.example.cramschool.entity.Exam;
import com.example.cramschool.service.ExamService;
import com.example.cramschool.service.HomeworkService;
import com.example.cramschool.service.ScoreService;
import com.example.cramschool.service.StudentAttendanceService;

@Controller
@RequestMapping("/analysis")
public class AnalysisController {

	private static final DateTimeFormatter MONTH_LABEL_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM");

	private final StudentAttendanceService studentAttendanceService;
	private final HomeworkService homeworkService;
	private final ExamService examService;
	private final ScoreService scoreService;

	public AnalysisController(StudentAttendanceService studentAttendanceService,
			HomeworkService homeworkService, ExamService examService, ScoreService scoreService) {
		this.studentAttendanceService = studentAttendanceService;
		this.homeworkService = homeworkService;
		this.examService = examService;
		this.scoreService = scoreService;
	}

	@GetMapping
	public String index(@RequestParam(defaultValue = "month") String attendanceRange,
			@RequestParam(required = false) Integer year,
			@RequestParam(required = false) Integer month,
			@RequestParam(defaultValue = "month") String homeworkRange,
			@RequestParam(required = false) Integer homeworkYear,
			@RequestParam(required = false) Integer homeworkMonth,
			@RequestParam(defaultValue = "attendance") String activePanel,
			Model model) {
		boolean monthlyRange = !"all".equalsIgnoreCase(attendanceRange);
		YearMonth currentMonth = resolveMonth(year, month);
		YearMonth targetMonth = monthlyRange ? currentMonth : null;
		boolean monthlyHomeworkRange = !"all".equalsIgnoreCase(homeworkRange);
		YearMonth currentHomeworkMonth = resolveMonth(homeworkYear, homeworkMonth);
		YearMonth targetHomeworkMonth = monthlyHomeworkRange ? currentHomeworkMonth : null;
		List<Exam> exams = examService.findAll();
		List<Exam> scoredExams = exams.stream()
				.filter(exam -> exam.getFullScore() != null && exam.getFullScore() > 0)
				.sorted(Comparator.comparing(Exam::getExamDate, Comparator.nullsLast(Comparator.naturalOrder()))
						.thenComparing(Exam::getId))
				.toList();
		List<Exam> practiceExams = exams.stream()
				.filter(exam -> exam.getFullScore() != null && exam.getFullScore() == 0)
				.toList();
		Map<Long, ScoreStats> scoreStatsByExam = scoreService.calculateStatsByExam(exams);
		String resolvedActivePanel = switch (activePanel == null ? "" : activePanel.toLowerCase()) {
			case "homework" -> "homework";
			case "exam" -> "exam";
			default -> "attendance";
		};

		model.addAttribute("pageTitle", "分析");
		model.addAttribute("activePanel", resolvedActivePanel);
		model.addAttribute("attendanceRange", monthlyRange ? "month" : "all");
		model.addAttribute("attendanceMonthLabel", currentMonth.format(MONTH_LABEL_FORMATTER));
		model.addAttribute("attendanceYear", currentMonth.getYear());
		model.addAttribute("attendanceMonth", currentMonth.getMonthValue());
		model.addAttribute("previousAttendanceMonth", currentMonth.minusMonths(1));
		model.addAttribute("nextAttendanceMonth", currentMonth.plusMonths(1));
		model.addAttribute("homeworkRange", monthlyHomeworkRange ? "month" : "all");
		model.addAttribute("homeworkMonthLabel", currentHomeworkMonth.format(MONTH_LABEL_FORMATTER));
		model.addAttribute("homeworkYear", currentHomeworkMonth.getYear());
		model.addAttribute("homeworkMonth", currentHomeworkMonth.getMonthValue());
		model.addAttribute("previousHomeworkMonth", currentHomeworkMonth.minusMonths(1));
		model.addAttribute("nextHomeworkMonth", currentHomeworkMonth.plusMonths(1));
		model.addAttribute("monthlyStudentAttendanceRates",
				studentAttendanceService.calculateStudentAttendanceRates(targetMonth));
		model.addAttribute("attendanceDetailsByStudentId",
				studentAttendanceService.buildStudentAttendanceDetails(targetMonth));
		model.addAttribute("studentHomeworkStatusRates", homeworkService.calculateStudentHomeworkStatusRates(targetHomeworkMonth));
		model.addAttribute("studentScoreTrends", scoreService.buildStudentScoreTrends(scoredExams));
		model.addAttribute("examChartMeta", buildExamChartMeta(scoredExams, scoreStatsByExam));
		model.addAttribute("scoredExamParticipantMeta", scoreService.buildExamParticipantMeta(scoredExams));
		model.addAttribute("practiceExamParticipantMeta", scoreService.buildExamParticipantMeta(practiceExams));
		model.addAttribute("scoredExams", scoredExams);
		model.addAttribute("practiceExams", practiceExams);
		model.addAttribute("scoreStatsByExam", scoreStatsByExam);
		return "analysis/index";
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

	private List<ExamChartMeta> buildExamChartMeta(List<Exam> scoredExams, Map<Long, ScoreStats> scoreStatsByExam) {
		return scoredExams.stream()
				.map(exam -> {
					ScoreStats scoreStats = scoreStatsByExam.get(exam.getId());
					return new ExamChartMeta(
							exam.getId(),
							exam.getExamDate().format(DateTimeFormatter.ofPattern("MM/dd")) + " " + exam.getName(),
							exam.getClassRoom().getId(),
							exam.getClassRoom().getDisplayName(),
							exam.getSubject().getId(),
							exam.getSubject().getName(),
							exam.getExamDate().format(DateTimeFormatter.ofPattern("yyyy/MM/dd")),
							scoreStats == null ? null : scoreStats.getAverage(),
							scoreStats == null ? null : scoreStats.getStandardDeviation());
				})
				.toList();
	}

	private record ExamChartMeta(Long id, String label, Long classRoomId, String classRoomName, Long subjectId,
			String subjectName, String examDate, Double average, Double standardDeviation) {
	}
}
