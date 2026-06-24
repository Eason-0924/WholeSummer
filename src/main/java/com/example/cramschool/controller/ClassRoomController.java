package com.example.cramschool.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

import com.example.cramschool.config.SchoolOptions;
import com.example.cramschool.entity.ClassRoom;
import com.example.cramschool.entity.ClassSchedule;
import com.example.cramschool.entity.ClassStudent;
import com.example.cramschool.entity.StudentAttendance;
import com.example.cramschool.form.ClassRoomForm;
import com.example.cramschool.service.ClassStudentService;
import com.example.cramschool.service.ClassRoomService;
import com.example.cramschool.service.ExamService;
import com.example.cramschool.service.HomeworkService;
import com.example.cramschool.service.ScoreService;
import com.example.cramschool.service.StudentAttendanceService;
import com.example.cramschool.service.SubjectService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/classes")
public class ClassRoomController {

	private static final List<String> SCHEDULE_WEEKDAYS = SchoolOptions.WEEKDAYS;
	private static final List<String> SCHEDULE_TIME_SLOTS = List.of("早上", "下午", "晚上");
	private static final LocalTime NOON = LocalTime.NOON;
	private static final LocalTime EVENING = LocalTime.of(18, 0);

	private final ClassRoomService classRoomService;
	private final ClassStudentService classStudentService;
	private final SubjectService subjectService;
	private final ExamService examService;
	private final ScoreService scoreService;
	private final HomeworkService homeworkService;
	private final StudentAttendanceService studentAttendanceService;

	public ClassRoomController(ClassRoomService classRoomService, ClassStudentService classStudentService,
			SubjectService subjectService, ExamService examService, ScoreService scoreService,
			HomeworkService homeworkService, StudentAttendanceService studentAttendanceService) {
		this.classRoomService = classRoomService;
		this.classStudentService = classStudentService;
		this.subjectService = subjectService;
		this.examService = examService;
		this.scoreService = scoreService;
		this.homeworkService = homeworkService;
		this.studentAttendanceService = studentAttendanceService;
	}

	@ModelAttribute
	public void addOptions(Model model) {
		model.addAttribute("gradeOptions", SchoolOptions.GRADES);
		model.addAttribute("weekdayOptions", SchoolOptions.WEEKDAYS);
		model.addAttribute("subjectOptions", subjectService.findActiveSubjects());
		model.addAttribute("teacherOptions", subjectService.findActiveTeachers());
	}

	@GetMapping
	public String list(Model model) {
		List<ClassRoom> activeClasses = classRoomService.findActiveClasses();
		List<ClassRoom> inactiveClasses = classRoomService.findInactiveClasses();
		Map<Long, Long> classStudentCounts = new LinkedHashMap<>();
		for (ClassRoom classRoom : activeClasses) {
			classStudentCounts.put(classRoom.getId(), classStudentService.countActiveByClassRoomId(classRoom.getId()));
		}
		for (ClassRoom classRoom : inactiveClasses) {
			classStudentCounts.put(classRoom.getId(), classStudentService.countActiveByClassRoomId(classRoom.getId()));
		}

		model.addAttribute("pageTitle", "班級管理");
		model.addAttribute("activeClasses", activeClasses);
		model.addAttribute("inactiveClasses", inactiveClasses);
		model.addAttribute("classStudentCounts", classStudentCounts);
		Map<String, Map<String, List<ScheduledClass>>> scheduleGrid = buildScheduleGrid(activeClasses);
		List<String> visibleScheduleWeekdays = findVisibleScheduleWeekdays(scheduleGrid);
		List<ScheduleRow> scheduleRows = buildScheduleRows(scheduleGrid, visibleScheduleWeekdays);
		model.addAttribute("scheduleWeekdays", visibleScheduleWeekdays);
		model.addAttribute("scheduleRows", scheduleRows);
		return "classes/list";
	}

	@GetMapping("/new")
	public String newForm(Model model) {
		model.addAttribute("pageTitle", "新增班級");
		model.addAttribute("classRoomForm", ClassRoomForm.newForm());
		model.addAttribute("formAction", "/classes");
		model.addAttribute("submitLabel", "新增");
		return "classes/form";
	}

	@PostMapping
	public String create(@Valid @ModelAttribute("classRoomForm") ClassRoomForm classRoomForm,
			BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			model.addAttribute("pageTitle", "新增班級");
			model.addAttribute("formAction", "/classes");
			model.addAttribute("submitLabel", "新增");
			return "classes/form";
		}

		ClassRoom classRoom = classRoomService.create(classRoomForm);
		redirectAttributes.addFlashAttribute("message", "已新增班級：" + classRoom.getDisplayName());
		return "redirect:/classes/" + classRoom.getId();
	}

	@GetMapping("/{id}")
	public String detail(@PathVariable Long id, Model model) {
		List<ClassStudent> classStudents = classStudentService.findActiveByClassRoomId(id);
		var exams = examService.findByClassRoomId(id);
		var scoredExams = exams.stream()
				.filter(exam -> exam.getFullScore() > 0)
				.toList();
		var practiceExams = exams.stream()
				.filter(exam -> exam.getFullScore() == 0)
				.toList();
		var homeworks = homeworkService.findByClassRoomId(id);
		model.addAttribute("pageTitle", "班級資料");
		model.addAttribute("classRoom", classRoomService.findById(id));
		model.addAttribute("classStudents", classStudents);
		model.addAttribute("studentCount", classStudents.size());
		model.addAttribute("availableStudents", classStudentService.findAvailableStudents(id));
		model.addAttribute("exams", exams);
		model.addAttribute("scoredExams", scoredExams);
		model.addAttribute("practiceExams", practiceExams);
		model.addAttribute("statsByExamId", scoreService.calculateStatsByExam(exams));
		model.addAttribute("homeworks", homeworks);
		model.addAttribute("homeworkCompletionRates", homeworkService.calculateCompletionRates(homeworks));
		model.addAttribute("attendanceStats", studentAttendanceService.calculateStatsByClassRoomId(id));
		model.addAttribute("attendanceTableRows",
				buildAttendanceTableRows(classStudents, studentAttendanceService.findByClassRoomId(id)));
		return "classes/detail";
	}

	@GetMapping("/{id}/edit")
	public String editForm(@PathVariable Long id, Model model) {
		ClassRoom classRoom = classRoomService.findById(id);
		model.addAttribute("pageTitle", "編輯班級");
		model.addAttribute("classRoom", classRoom);
		model.addAttribute("classRoomForm", ClassRoomForm.from(classRoom));
		model.addAttribute("formAction", "/classes/" + id);
		model.addAttribute("submitLabel", "儲存");
		return "classes/form";
	}

	@PostMapping("/{id}")
	public String update(@PathVariable Long id,
			@Valid @ModelAttribute("classRoomForm") ClassRoomForm classRoomForm,
			BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			model.addAttribute("pageTitle", "編輯班級");
			model.addAttribute("classRoom", classRoomService.findById(id));
			model.addAttribute("formAction", "/classes/" + id);
			model.addAttribute("submitLabel", "儲存");
			return "classes/form";
		}

		ClassRoom classRoom = classRoomService.update(id, classRoomForm);
		redirectAttributes.addFlashAttribute("message", "已更新班級：" + classRoom.getDisplayName());
		return "redirect:/classes/" + id;
	}

	@PostMapping("/{id}/deactivate")
	public String deactivate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		ClassRoom classRoom = classRoomService.findById(id);
		classRoomService.deactivate(id);
		redirectAttributes.addFlashAttribute("message", "已停用班級：" + classRoom.getDisplayName());
		return "redirect:/classes";
	}

	@PostMapping("/{id}/activate")
	public String activate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		ClassRoom classRoom = classRoomService.findById(id);
		classRoomService.activate(id);
		redirectAttributes.addFlashAttribute("message", "已啟用班級：" + classRoom.getDisplayName());
		return "redirect:/classes";
	}

	@PostMapping("/{id}/delete")
	public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		ClassRoom classRoom = classRoomService.findById(id);
		classRoomService.delete(id);
		redirectAttributes.addFlashAttribute("message", "已刪除班級：" + classRoom.getDisplayName());
		return "redirect:/classes";
	}

	@PostMapping("/{id}/students")
	public String addStudent(@PathVariable Long id, @RequestParam Long studentId,
			RedirectAttributes redirectAttributes) {
		try {
			classStudentService.addStudent(id, studentId);
			redirectAttributes.addFlashAttribute("message", "已加入學生");
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return "redirect:/classes/" + id;
	}

	@PostMapping("/{id}/students/{classStudentId}/remove")
	public String removeStudent(@PathVariable Long id, @PathVariable Long classStudentId,
			RedirectAttributes redirectAttributes) {
		try {
			classStudentService.removeStudent(id, classStudentId);
			redirectAttributes.addFlashAttribute("message", "已刪除班級學生紀錄");
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return "redirect:/classes/" + id;
	}

	private List<ScheduleRow> buildScheduleRows(Map<String, Map<String, List<ScheduledClass>>> scheduleGrid,
			List<String> visibleScheduleWeekdays) {
		List<ScheduleRow> scheduleRows = new ArrayList<>();
		for (String timeSlot : SCHEDULE_TIME_SLOTS) {
			Map<String, List<ScheduledClass>> classesByWeekday = scheduleGrid.get(timeSlot);
			if (!hasScheduledClass(classesByWeekday, visibleScheduleWeekdays)) {
				continue;
			}
			List<ScheduleCell> cells = new ArrayList<>();
			for (String weekday : visibleScheduleWeekdays) {
				cells.add(new ScheduleCell(weekday, classesByWeekday.get(weekday)));
			}
			scheduleRows.add(new ScheduleRow(timeSlot, cells));
		}
		return scheduleRows;
	}

	private Map<String, Map<String, List<ScheduledClass>>> buildScheduleGrid(List<ClassRoom> classRooms) {
		Map<String, Map<String, List<ScheduledClass>>> scheduleGrid = new LinkedHashMap<>();
		for (String timeSlot : SCHEDULE_TIME_SLOTS) {
			Map<String, List<ScheduledClass>> classesByWeekday = new LinkedHashMap<>();
			for (String weekday : SCHEDULE_WEEKDAYS) {
				classesByWeekday.put(weekday, new ArrayList<>());
			}
			scheduleGrid.put(timeSlot, classesByWeekday);
		}

		for (ClassRoom classRoom : classRooms) {
			for (ClassSchedule schedule : classRoom.getEffectiveSchedules()) {
				if (!hasCompleteSchedule(schedule) || !SCHEDULE_WEEKDAYS.contains(schedule.getWeekday())) {
					continue;
				}
				scheduleGrid.get(findScheduleTimeSlot(schedule)).get(schedule.getWeekday())
						.add(new ScheduledClass(classRoom, schedule));
			}
		}

		Comparator<ScheduledClass> byStartTimeThenName = Comparator
				.comparing(ScheduledClass::getStartTime, Comparator.nullsLast(LocalTime::compareTo))
				.thenComparing(ScheduledClass::getClassName, Comparator.nullsLast(String::compareTo));
		for (Map<String, List<ScheduledClass>> classesByWeekday : scheduleGrid.values()) {
			for (List<ScheduledClass> scheduledClasses : classesByWeekday.values()) {
				scheduledClasses.sort(byStartTimeThenName);
			}
		}
		return scheduleGrid;
	}

	private List<AttendanceTableRow> buildAttendanceTableRows(List<ClassStudent> classStudents,
			List<StudentAttendance> attendances) {
		Map<LocalDate, Map<Long, StudentAttendance>> attendancesByDate = new LinkedHashMap<>();
		for (StudentAttendance attendance : attendances) {
			attendancesByDate.computeIfAbsent(attendance.getAttendanceDate(), date -> new LinkedHashMap<>())
					.put(attendance.getStudent().getId(), attendance);
		}

		List<AttendanceTableRow> rows = new ArrayList<>();
		for (Map.Entry<LocalDate, Map<Long, StudentAttendance>> entry : attendancesByDate.entrySet()) {
			List<StudentAttendance> rowAttendances = new ArrayList<>();
			for (ClassStudent classStudent : classStudents) {
				rowAttendances.add(entry.getValue().get(classStudent.getStudent().getId()));
			}
			rows.add(new AttendanceTableRow(entry.getKey(), rowAttendances));
		}
		return rows;
	}

	private String findScheduleTimeSlot(ClassSchedule schedule) {
		if (schedule.getStartTime().isBefore(NOON)) {
			return "早上";
		}
		if (schedule.getStartTime().isBefore(EVENING)) {
			return "下午";
		}
		return "晚上";
	}

	private boolean hasCompleteSchedule(ClassSchedule schedule) {
		return hasText(schedule.getWeekday()) && schedule.getStartTime() != null && schedule.getEndTime() != null;
	}

	private boolean hasText(String value) {
		return value != null && !value.trim().isEmpty();
	}

	private List<String> findVisibleScheduleWeekdays(Map<String, Map<String, List<ScheduledClass>>> scheduleGrid) {
		int firstScheduledWeekday = -1;
		int lastScheduledWeekday = -1;
		for (int i = 0; i < SCHEDULE_WEEKDAYS.size(); i++) {
			String weekday = SCHEDULE_WEEKDAYS.get(i);
			if (hasScheduledClassOnWeekday(scheduleGrid, weekday)) {
				if (firstScheduledWeekday < 0) {
					firstScheduledWeekday = i;
				}
				lastScheduledWeekday = i;
			}
		}
		if (firstScheduledWeekday < 0) {
			return List.of();
		}
		return SCHEDULE_WEEKDAYS.subList(firstScheduledWeekday, lastScheduledWeekday + 1);
	}

	private boolean hasScheduledClassOnWeekday(Map<String, Map<String, List<ScheduledClass>>> scheduleGrid,
			String weekday) {
		return scheduleGrid.values().stream()
				.map(classesByWeekday -> classesByWeekday.get(weekday))
				.anyMatch(classes -> classes != null && !classes.isEmpty());
	}

	private boolean hasScheduledClass(Map<String, List<ScheduledClass>> classesByWeekday, List<String> visibleWeekdays) {
		return visibleWeekdays.stream()
				.map(classesByWeekday::get)
				.anyMatch(classes -> !classes.isEmpty());
	}

	public static class ScheduleRow {

		private final String timeSlot;
		private final List<ScheduleCell> cells;

		public ScheduleRow(String timeSlot, List<ScheduleCell> cells) {
			this.timeSlot = timeSlot;
			this.cells = cells;
		}

		public String getTimeSlot() {
			return timeSlot;
		}

		public List<ScheduleCell> getCells() {
			return cells;
		}
	}

	public static class ScheduleCell {

		private final String weekday;
		private final List<ScheduledClass> classes;

		public ScheduleCell(String weekday, List<ScheduledClass> classes) {
			this.weekday = weekday;
			this.classes = classes;
		}

		public String getWeekday() {
			return weekday;
		}

		public List<ScheduledClass> getClasses() {
			return classes;
		}
	}

	public static class ScheduledClass {

		private final ClassRoom classRoom;
		private final ClassSchedule schedule;

		public ScheduledClass(ClassRoom classRoom, ClassSchedule schedule) {
			this.classRoom = classRoom;
			this.schedule = schedule;
		}

		public Long getClassRoomId() {
			return classRoom.getId();
		}

		public String getClassName() {
			return classRoom.getDisplayName();
		}

		public String getTeacherName() {
			return classRoom.getTeacherName();
		}

		public String getTimeRangeText() {
			return schedule.getTimeRangeText();
		}

		public Long getSubjectKey() {
			return classRoom.getSubject() == null ? null : classRoom.getSubject().getId();
		}

		public Long getTeacherKey() {
			return classRoom.getTeacher() == null ? null : classRoom.getTeacher().getId();
		}

		public String getGradeKey() {
			return classRoom.getGrade();
		}

		public LocalTime getStartTime() {
			return schedule.getStartTime();
		}
	}

	public static class AttendanceTableRow {

		private final LocalDate date;
		private final List<StudentAttendance> attendances;

		public AttendanceTableRow(LocalDate date, List<StudentAttendance> attendances) {
			this.date = date;
			this.attendances = attendances;
		}

		public LocalDate getDate() {
			return date;
		}

		public List<StudentAttendance> getAttendances() {
			return attendances;
		}
	}
}
