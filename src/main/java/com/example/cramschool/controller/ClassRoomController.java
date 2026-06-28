package com.example.cramschool.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
import com.example.cramschool.dto.WeeklyScheduleDto;
import com.example.cramschool.entity.ClassRoom;
import com.example.cramschool.entity.ClassSchedule;
import com.example.cramschool.entity.ClassStudent;
import com.example.cramschool.entity.ScheduleType;
import com.example.cramschool.entity.StudentAttendance;
import com.example.cramschool.entity.TeacherPermissionType;
import com.example.cramschool.form.ClassRoomForm;
import com.example.cramschool.service.ClassStudentService;
import com.example.cramschool.service.ClassRoomService;
import com.example.cramschool.service.ClassStatisticsExportService;
import com.example.cramschool.service.ExamService;
import com.example.cramschool.service.HomeworkService;
import com.example.cramschool.service.ScoreService;
import com.example.cramschool.service.StudentAttendanceService;
import com.example.cramschool.service.SubjectService;
import com.example.cramschool.service.TeacherPermissionService;
import com.example.cramschool.service.WeeklyScheduleService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/classes")
public class ClassRoomController {

	private static final List<String> SCHEDULE_WEEKDAYS = SchoolOptions.WEEKDAYS;
	private static final List<String> SCHEDULE_TIME_SLOTS = List.of("早上", "下午", "晚上");
	private static final LocalTime NOON = LocalTime.NOON;
	private static final LocalTime EVENING = LocalTime.of(18, 0);

	private final ClassRoomService classRoomService;
	private final ClassStatisticsExportService classStatisticsExportService;
	private final ClassStudentService classStudentService;
	private final SubjectService subjectService;
	private final ExamService examService;
	private final ScoreService scoreService;
	private final HomeworkService homeworkService;
	private final StudentAttendanceService studentAttendanceService;
	private final TeacherPermissionService teacherPermissionService;
	private final WeeklyScheduleService weeklyScheduleService;

	public ClassRoomController(ClassRoomService classRoomService, ClassStatisticsExportService classStatisticsExportService,
			ClassStudentService classStudentService,
			SubjectService subjectService, ExamService examService, ScoreService scoreService,
			HomeworkService homeworkService, StudentAttendanceService studentAttendanceService,
			TeacherPermissionService teacherPermissionService,
			WeeklyScheduleService weeklyScheduleService) {
		this.classRoomService = classRoomService;
		this.classStatisticsExportService = classStatisticsExportService;
		this.classStudentService = classStudentService;
		this.subjectService = subjectService;
		this.examService = examService;
		this.scoreService = scoreService;
		this.homeworkService = homeworkService;
		this.studentAttendanceService = studentAttendanceService;
		this.teacherPermissionService = teacherPermissionService;
		this.weeklyScheduleService = weeklyScheduleService;
	}

	@ModelAttribute
	public void addOptions(Model model) {
		model.addAttribute("gradeOptions", SchoolOptions.CLASS_GRADES);
		model.addAttribute("weekdayOptions", SchoolOptions.WEEKDAYS);
		model.addAttribute("subjectOptions", subjectService.findActiveSubjects());
		model.addAttribute("teacherOptions", subjectService.findActiveTeachers());
	}

	@GetMapping
	public String list(Model model, HttpSession session) {
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
		Long currentTeacherId = currentTeacherId(session);
		boolean director = hasPermission(session, TeacherPermissionType.MANAGE_ALL_ATTENDANCE);
		List<WeeklyScheduleDto> weeklySchedules = weeklyScheduleService.findWeeklySchedules(
				LocalDate.now(), currentTeacherId, director, director ? null : currentTeacherId, null);
		Map<String, Map<String, List<ScheduledClass>>> weeklyScheduleGrid = buildWeeklyScheduleGrid(weeklySchedules);
		List<String> visibleWeeklyScheduleWeekdays = findVisibleScheduleWeekdays(weeklyScheduleGrid);
		model.addAttribute("weeklyScheduleWeekdays", visibleWeeklyScheduleWeekdays);
		model.addAttribute("weeklyScheduleRows", buildScheduleRows(weeklyScheduleGrid, visibleWeeklyScheduleWeekdays));
		model.addAttribute("rescheduleCourseOptions", buildRescheduleCourseOptions(
				activeClasses, currentTeacherId, director, LocalDate.now()));
		model.addAttribute("rescheduleCalendarDates", weeklyScheduleService.buildRescheduleCalendarDates(LocalDate.now()));
		return "classes/list";
	}

	@GetMapping("/new")
	public String newForm(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
		if (!hasPermission(session, TeacherPermissionType.CLASS_CREATE)) {
			redirectAttributes.addFlashAttribute("errorMessage", "權限不足，無法新增班級");
			return "redirect:/classes";
		}
		model.addAttribute("pageTitle", "新增班級");
		model.addAttribute("classRoomForm", ClassRoomForm.newForm());
		model.addAttribute("formAction", "/classes");
		model.addAttribute("submitLabel", "新增");
		return "classes/form";
	}

	@PostMapping
	public String create(@Valid @ModelAttribute("classRoomForm") ClassRoomForm classRoomForm,
			BindingResult bindingResult, Model model, HttpSession session,
			RedirectAttributes redirectAttributes) {
		if (!hasPermission(session, TeacherPermissionType.CLASS_CREATE)) {
			redirectAttributes.addFlashAttribute("errorMessage", "權限不足，無法新增班級");
			return "redirect:/classes";
		}
		if (bindingResult.hasErrors()) {
			model.addAttribute("pageTitle", "新增班級");
			model.addAttribute("formAction", "/classes");
			model.addAttribute("submitLabel", "新增");
			return "classes/form";
		}

		ClassRoom classRoom = classRoomService.create(classRoomForm, currentTeacherId(session));
		redirectAttributes.addFlashAttribute("message", "已新增班級：" + classRoom.getDisplayName());
		return redirectToClassRoom(classRoom);
	}

	@GetMapping("/{slug}")
	public String detail(@PathVariable String slug, Model model) {
		ClassRoom classRoom = classRoomService.findByUrlSlugOrId(slug);
		if (classRoom.getUrlSlug() != null && !slug.equals(classRoom.getUrlSlug())) {
			return redirectToClassRoom(classRoom);
		}
		Long classRoomId = classRoom.getId();
		List<ClassStudent> classStudents = classStudentService.findActiveByClassRoomId(classRoomId);
		var exams = examService.findByClassRoomId(classRoomId);
		var scoredExams = exams.stream()
				.filter(exam -> exam.getFullScore() > 0)
				.toList();
		var practiceExams = exams.stream()
				.filter(exam -> exam.getFullScore() == 0)
				.toList();
		var homeworks = homeworkService.findByClassRoomId(classRoomId);
		model.addAttribute("pageTitle", "班級資料");
		model.addAttribute("classRoom", classRoom);
		model.addAttribute("classStudents", classStudents);
		model.addAttribute("studentCount", classStudents.size());
		model.addAttribute("availableStudents", classStudentService.findAvailableStudents(classRoomId));
		model.addAttribute("exams", exams);
		model.addAttribute("scoredExams", scoredExams);
		model.addAttribute("practiceExams", practiceExams);
		model.addAttribute("statsByExamId", scoreService.calculateStatsByExam(exams));
		model.addAttribute("homeworks", homeworks);
		model.addAttribute("homeworkCompletionRates", homeworkService.calculateCompletionRates(homeworks));
		model.addAttribute("attendanceStats", studentAttendanceService.calculateStatsByClassRoomId(classRoomId));
		model.addAttribute("attendanceTableRows",
				buildAttendanceTableRows(classStudents, studentAttendanceService.findByClassRoomId(classRoomId)));
		return "classes/detail";
	}

	@GetMapping("/{slug}/edit")
	public String editForm(@PathVariable String slug, HttpSession session, Model model,
			RedirectAttributes redirectAttributes) {
		ClassRoom classRoom = classRoomService.findByUrlSlugOrId(slug);
		if (!hasPermission(session, TeacherPermissionType.CLASS_UPDATE)) {
			redirectAttributes.addFlashAttribute("errorMessage", "權限不足，無法變更班級資料");
			return redirectToClassRoom(classRoom);
		}
		model.addAttribute("pageTitle", "編輯班級");
		model.addAttribute("classRoom", classRoom);
		model.addAttribute("classRoomForm", ClassRoomForm.from(classRoom));
		model.addAttribute("formAction", "/classes/" + classRoom.getUrlSlug());
		model.addAttribute("submitLabel", "儲存");
		return "classes/form";
	}

	@PostMapping("/{slug}")
	public String update(@PathVariable String slug,
			@Valid @ModelAttribute("classRoomForm") ClassRoomForm classRoomForm,
			BindingResult bindingResult, Model model, HttpSession session,
			RedirectAttributes redirectAttributes) {
		ClassRoom existingClassRoom = classRoomService.findByUrlSlugOrId(slug);
		if (!hasPermission(session, TeacherPermissionType.CLASS_UPDATE)) {
			redirectAttributes.addFlashAttribute("errorMessage", "權限不足，無法變更班級資料");
			return redirectToClassRoom(existingClassRoom);
		}
		if (bindingResult.hasErrors()) {
			model.addAttribute("pageTitle", "編輯班級");
			model.addAttribute("classRoom", existingClassRoom);
			model.addAttribute("formAction", "/classes/" + existingClassRoom.getUrlSlug());
			model.addAttribute("submitLabel", "儲存");
			return "classes/form";
		}

		ClassRoom classRoom = classRoomService.update(existingClassRoom.getId(), classRoomForm, currentTeacherId(session));
		redirectAttributes.addFlashAttribute("message", "已更新班級：" + classRoom.getDisplayName());
		return redirectToClassRoom(classRoom);
	}

	@PostMapping("/{slug}/deactivate")
	public String deactivate(@PathVariable String slug, HttpSession session, RedirectAttributes redirectAttributes) {
		if (!hasPermission(session, TeacherPermissionType.CLASS_UPDATE)) {
			redirectAttributes.addFlashAttribute("errorMessage", "權限不足，無法變更班級資料");
			return "redirect:/classes";
		}
		ClassRoom classRoom = classRoomService.findByUrlSlugOrId(slug);
		classRoomService.deactivate(classRoom.getId(), currentTeacherId(session));
		redirectAttributes.addFlashAttribute("message", "已停用班級：" + classRoom.getDisplayName());
		return "redirect:/classes";
	}

	@PostMapping("/{slug}/activate")
	public String activate(@PathVariable String slug, HttpSession session, RedirectAttributes redirectAttributes) {
		if (!hasPermission(session, TeacherPermissionType.CLASS_UPDATE)) {
			redirectAttributes.addFlashAttribute("errorMessage", "權限不足，無法變更班級資料");
			return "redirect:/classes";
		}
		ClassRoom classRoom = classRoomService.findByUrlSlugOrId(slug);
		classRoomService.activate(classRoom.getId(), currentTeacherId(session));
		redirectAttributes.addFlashAttribute("message", "已啟用班級：" + classRoom.getDisplayName());
		return "redirect:/classes";
	}

	@PostMapping("/{slug}/delete")
	public String delete(@PathVariable String slug, HttpSession session, RedirectAttributes redirectAttributes) {
		if (!hasPermission(session, TeacherPermissionType.CLASS_UPDATE)) {
			redirectAttributes.addFlashAttribute("errorMessage", "權限不足，無法變更班級資料");
			return "redirect:/classes";
		}
		ClassRoom classRoom = classRoomService.findByUrlSlugOrId(slug);
		classRoomService.delete(classRoom.getId(), currentTeacherId(session));
		redirectAttributes.addFlashAttribute("message", "已刪除班級：" + classRoom.getDisplayName());
		return "redirect:/classes";
	}

	@PostMapping("/{slug}/export")
	public String exportStatistics(@PathVariable String slug,
			@RequestParam String section,
			@RequestParam String range,
			RedirectAttributes redirectAttributes) {
		ClassRoom classRoom = classRoomService.findByUrlSlugOrId(slug);
		try {
			classStatisticsExportService.exportAndOpenFolder(classRoom.getId(), section, range);
			redirectAttributes.addFlashAttribute("message", "已匯出班級統計資料並開啟資料夾");
		} catch (IllegalArgumentException | java.io.UncheckedIOException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return redirectToClassRoom(classRoom);
	}

	@PostMapping("/{slug}/students")
	public String addStudent(@PathVariable String slug, @RequestParam Long studentId,
			HttpSession session, RedirectAttributes redirectAttributes) {
		ClassRoom classRoom = classRoomService.findByUrlSlugOrId(slug);
		if (!hasPermission(session, TeacherPermissionType.CLASS_UPDATE)) {
			redirectAttributes.addFlashAttribute("errorMessage", "權限不足，無法變更班級資料");
			return redirectToClassRoom(classRoom);
		}
		try {
			classStudentService.addStudent(classRoom.getId(), studentId, currentTeacherId(session));
			redirectAttributes.addFlashAttribute("message", "已加入學生");
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return redirectToClassRoom(classRoom);
	}

	@PostMapping("/{slug}/students/{classStudentId}/remove")
	public String removeStudent(@PathVariable String slug, @PathVariable Long classStudentId,
			HttpSession session, RedirectAttributes redirectAttributes) {
		ClassRoom classRoom = classRoomService.findByUrlSlugOrId(slug);
		if (!hasPermission(session, TeacherPermissionType.CLASS_UPDATE)) {
			redirectAttributes.addFlashAttribute("errorMessage", "權限不足，無法變更班級資料");
			return redirectToClassRoom(classRoom);
		}
		try {
			classStudentService.removeStudent(classRoom.getId(), classStudentId, currentTeacherId(session));
			redirectAttributes.addFlashAttribute("message", "已刪除班級學生紀錄");
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return redirectToClassRoom(classRoom);
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

	private Map<String, Map<String, List<ScheduledClass>>> buildWeeklyScheduleGrid(List<WeeklyScheduleDto> schedules) {
		Map<String, Map<String, List<ScheduledClass>>> scheduleGrid = new LinkedHashMap<>();
		for (String timeSlot : SCHEDULE_TIME_SLOTS) {
			Map<String, List<ScheduledClass>> classesByWeekday = new LinkedHashMap<>();
			for (String weekday : SCHEDULE_WEEKDAYS) {
				classesByWeekday.put(weekday, new ArrayList<>());
			}
			scheduleGrid.put(timeSlot, classesByWeekday);
		}
		for (WeeklyScheduleDto schedule : schedules) {
			String weekday = SCHEDULE_WEEKDAYS.get(schedule.getCourseDate().getDayOfWeek().getValue() - 1);
			ScheduledClass scheduledClass = new ScheduledClass(schedule);
			scheduleGrid.get(findScheduleTimeSlot(schedule.getStartTime().toLocalTime())).get(weekday)
					.add(scheduledClass);
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

	private List<WeeklyScheduleDto> buildRescheduleCourseOptions(List<ClassRoom> activeClasses,
			Long currentTeacherId, boolean director, LocalDate baseDate) {
		LocalDate today = baseDate == null ? LocalDate.now() : baseDate;
		return activeClasses.stream()
				.filter(classRoom -> director || classRoom.getTeacher() != null
						&& currentTeacherId != null
						&& currentTeacherId.equals(classRoom.getTeacher().getId()))
				.flatMap(classRoom -> classRoom.getEffectiveSchedules().stream()
						.filter(this::hasCompleteSchedule)
						.map(schedule -> toRescheduleCourseOption(classRoom, schedule, today)))
				.sorted(Comparator.comparing(WeeklyScheduleDto::getStartTime)
						.thenComparing(WeeklyScheduleDto::getClassName, Comparator.nullsLast(String::compareTo)))
				.toList();
	}

	private WeeklyScheduleDto toRescheduleCourseOption(ClassRoom classRoom, ClassSchedule schedule, LocalDate today) {
		LocalDate courseDate = nextCourseDate(schedule, today);
		return new WeeklyScheduleDto(
				schedule.getId(),
				null,
				classRoom.getId(),
				classRoom.getSubjectName(),
				classRoom.getDisplayName(),
				classRoom.getTeacherName(),
				courseDate,
				LocalDateTime.of(courseDate, schedule.getStartTime()),
				LocalDateTime.of(courseDate, schedule.getEndTime()),
				ScheduleType.NORMAL,
				null,
				null,
				classRoom.getSubject() == null ? "未指定" : String.valueOf(classRoom.getSubject().getId()),
				classRoom.getTeacher() == null ? "未指定" : String.valueOf(classRoom.getTeacher().getId()),
				classRoom.getGrade() == null || classRoom.getGrade().isBlank() ? "未指定" : classRoom.getGrade());
	}

	private LocalDate nextCourseDate(ClassSchedule schedule, LocalDate today) {
		int weekdayIndex = SCHEDULE_WEEKDAYS.indexOf(schedule.getWeekday());
		if (weekdayIndex < 0) {
			return today;
		}
		int todayIndex = today.getDayOfWeek().getValue() - 1;
		int daysUntilCourse = (weekdayIndex - todayIndex + 7) % 7;
		LocalDate courseDate = today.plusDays(daysUntilCourse);
		if (LocalDateTime.of(courseDate, schedule.getStartTime()).isAfter(LocalDateTime.now())) {
			return courseDate;
		}
		return courseDate.plusDays(7);
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
		return findScheduleTimeSlot(schedule.getStartTime());
	}

	private String findScheduleTimeSlot(LocalTime startTime) {
		if (startTime.isBefore(NOON)) {
			return "早上";
		}
		if (startTime.isBefore(EVENING)) {
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

	private boolean hasPermission(HttpSession session, TeacherPermissionType permissionType) {
		return teacherPermissionService.hasPermission(currentTeacherId(session), permissionType);
	}

	private Long currentTeacherId(HttpSession session) {
		Object teacherId = session.getAttribute(AuthController.TEACHER_ID_SESSION_KEY);
		return teacherId instanceof Long id ? id : null;
	}

	private String redirectToClassRoom(ClassRoom classRoom) {
		return "redirect:/classes/" + (classRoom.getUrlSlug() == null ? classRoom.getId() : classRoom.getUrlSlug());
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
		private final WeeklyScheduleDto weeklySchedule;

		public ScheduledClass(ClassRoom classRoom, ClassSchedule schedule) {
			this.classRoom = classRoom;
			this.schedule = schedule;
			this.weeklySchedule = null;
		}

		public ScheduledClass(WeeklyScheduleDto weeklySchedule) {
			this.classRoom = null;
			this.schedule = null;
			this.weeklySchedule = weeklySchedule;
		}

		public String getClassRoomSlug() {
			return classRoom == null ? "" : classRoom.getUrlSlug();
		}

		public String getClassName() {
			return weeklySchedule == null ? classRoom.getDisplayName() : weeklySchedule.getClassName();
		}

		public String getTeacherName() {
			return weeklySchedule == null ? classRoom.getTeacherName() : weeklySchedule.getTeacherName();
		}

		public String getTimeRangeText() {
			if (weeklySchedule == null) {
				return schedule.getTimeRangeText();
			}
			return weeklySchedule.getStartTime().toLocalTime() + " ~ " + weeklySchedule.getEndTime().toLocalTime();
		}

		public Long getSubjectKey() {
			if (weeklySchedule != null) {
				return null;
			}
			return classRoom.getSubject() == null ? null : classRoom.getSubject().getId();
		}

		public String getSubjectColorKey() {
			return weeklySchedule == null
					? (classRoom.getSubject() == null ? "未指定" : String.valueOf(classRoom.getSubject().getId()))
					: weeklySchedule.getSubjectKey();
		}

		public Long getTeacherKey() {
			if (weeklySchedule != null) {
				return null;
			}
			return classRoom.getTeacher() == null ? null : classRoom.getTeacher().getId();
		}

		public String getTeacherColorKey() {
			return weeklySchedule == null
					? (classRoom.getTeacher() == null ? "未指定" : String.valueOf(classRoom.getTeacher().getId()))
					: weeklySchedule.getTeacherKey();
		}

		public String getGradeKey() {
			if (weeklySchedule != null) {
				return weeklySchedule.getGradeKey();
			}
			return classRoom.getGrade();
		}

		public LocalTime getStartTime() {
			return weeklySchedule == null ? schedule.getStartTime() : weeklySchedule.getStartTime().toLocalTime();
		}

		public boolean isWeekly() {
			return weeklySchedule != null;
		}

		public String getScheduleTypeName() {
			return weeklySchedule == null ? "NORMAL" : weeklySchedule.getScheduleType().name();
		}

		public String getScheduleTypeDisplayName() {
			return weeklySchedule == null ? "原課程" : weeklySchedule.getScheduleTypeDisplayName();
		}

		public Long getScheduleId() {
			return weeklySchedule == null ? schedule.getId() : weeklySchedule.getScheduleId();
		}

		public LocalDate getCourseDate() {
			return weeklySchedule == null ? null : weeklySchedule.getCourseDate();
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
