package com.example.cramschool.service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.dto.WeeklyScheduleDto;
import com.example.cramschool.entity.ParentLineBinding;
import com.example.cramschool.entity.ScheduleType;
import com.example.cramschool.entity.Student;
import com.example.cramschool.repository.ClassStudentRepository;
import com.example.cramschool.repository.ParentLineBindingRepository;

@Service
@Transactional(readOnly = true)
public class LineScheduleService {

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
	private static final DateTimeFormatter SHORT_DATE_FORMATTER = DateTimeFormatter.ofPattern("M/d");
	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
	private static final String UNBOUND_MESSAGE = "您尚未綁定學生資料。\n請先輸入：\n綁定 綁定碼\n例如：\n綁定 123456\n若尚未取得綁定碼，請洽補習班櫃台。";
	private static final String FOOTER = "若課表有異動，請以補習班最新通知為準。";

	private final ParentLineBindingRepository parentLineBindingRepository;
	private final ClassStudentRepository classStudentRepository;
	private final WeeklyScheduleService weeklyScheduleService;
	private final Clock clock;

	@Autowired
	public LineScheduleService(ParentLineBindingRepository parentLineBindingRepository,
			ClassStudentRepository classStudentRepository, WeeklyScheduleService weeklyScheduleService) {
		this(parentLineBindingRepository, classStudentRepository, weeklyScheduleService, Clock.systemDefaultZone());
	}

	LineScheduleService(ParentLineBindingRepository parentLineBindingRepository,
			ClassStudentRepository classStudentRepository, WeeklyScheduleService weeklyScheduleService, Clock clock) {
		this.parentLineBindingRepository = parentLineBindingRepository;
		this.classStudentRepository = classStudentRepository;
		this.weeklyScheduleService = weeklyScheduleService;
		this.clock = clock;
	}

	public String buildWeeklyScheduleReply(String lineUserId) {
		if (lineUserId == null || lineUserId.isBlank()) {
			return UNBOUND_MESSAGE;
		}
		List<Student> students = parentLineBindingRepository
				.findByLineUserIdAndStatusOrderByStudentChineseNameAsc(lineUserId, ParentLineBinding.STATUS_BOUND)
				.stream()
				.map(ParentLineBinding::getStudent)
				.filter(student -> student != null && student.isActive())
				.distinct()
				.toList();
		if (students.isEmpty()) {
			return UNBOUND_MESSAGE;
		}

		LocalDate weekStart = LocalDate.now(clock).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		List<StudentWeeklySchedules> weeklySchedules = students.stream()
				.map(student -> new StudentWeeklySchedules(student, findStudentWeeklySchedules(student, weekStart)))
				.toList();
		return weeklySchedules.size() == 1
				? formatSingleStudent(weeklySchedules.getFirst(), weekStart)
				: formatMultipleStudents(weeklySchedules, weekStart);
	}

	private List<WeeklyScheduleDto> findStudentWeeklySchedules(Student student, LocalDate weekStart) {
		Set<Long> classRoomIds = classStudentRepository.findByStudentIdAndActiveTrue(student.getId())
				.stream()
				.map(classStudent -> classStudent.getClassRoom() == null ? null : classStudent.getClassRoom().getId())
				.filter(id -> id != null)
				.collect(Collectors.toSet());
		if (classRoomIds.isEmpty()) {
			return List.of();
		}
		return weeklyScheduleService.findWeeklySchedules(weekStart, null, true, null, null)
				.stream()
				.filter(schedule -> schedule.getClassRoomId() != null && classRoomIds.contains(schedule.getClassRoomId()))
				.filter(schedule -> schedule.getScheduleType() != ScheduleType.CANCELLED)
				.sorted(Comparator.comparing(WeeklyScheduleDto::getStartTime)
						.thenComparing(WeeklyScheduleDto::getClassName))
				.toList();
	}

	private String formatSingleStudent(StudentWeeklySchedules studentSchedules, LocalDate weekStart) {
		Student student = studentSchedules.student();
		List<WeeklyScheduleDto> schedules = studentSchedules.schedules();
		if (schedules.isEmpty()) {
			return student.getDisplayName() + "本週目前沒有排定課程。\n若您認為資料有誤，請洽補習班櫃台。";
		}

		StringBuilder builder = new StringBuilder();
		builder.append(student.getDisplayName()).append(" 本週課表\n")
				.append(formatFullDate(weekStart)).append(" - ")
				.append(formatFullDate(weekStart.plusDays(6))).append("\n");
		LocalDate previousDate = null;
		for (WeeklyScheduleDto schedule : schedules) {
			if (!schedule.getCourseDate().equals(previousDate)) {
				builder.append(formatShortDate(schedule.getCourseDate())).append("\n");
				previousDate = schedule.getCourseDate();
			}
			appendSingleSchedule(builder, schedule);
		}
		builder.append(FOOTER);
		return builder.toString();
	}

	private String formatMultipleStudents(List<StudentWeeklySchedules> weeklySchedules, LocalDate weekStart) {
		StringBuilder builder = new StringBuilder();
		builder.append("您已綁定 ").append(weeklySchedules.size()).append(" 位學生，以下為本週課表：\n")
				.append(formatFullDate(weekStart)).append(" - ")
				.append(formatFullDate(weekStart.plusDays(6))).append("\n");
		for (StudentWeeklySchedules studentSchedules : weeklySchedules) {
			builder.append("【").append(studentSchedules.student().getDisplayName()).append("】\n");
			if (studentSchedules.schedules().isEmpty()) {
				builder.append("本週目前沒有排定課程。\n");
				continue;
			}
			for (WeeklyScheduleDto schedule : studentSchedules.schedules()) {
				builder.append(formatShortDate(schedule.getCourseDate())).append(" ")
						.append(formatTimeRange(schedule)).append(" ")
						.append(scheduleLineTitle(schedule));
				String typeText = scheduleTypeText(schedule);
				if (!typeText.isBlank()) {
					builder.append("（").append(typeText).append("）");
				}
				builder.append("\n");
			}
		}
		builder.append(FOOTER);
		return builder.toString();
	}

	private void appendSingleSchedule(StringBuilder builder, WeeklyScheduleDto schedule) {
		builder.append(formatTimeRange(schedule)).append(" ")
				.append(scheduleLineTitle(schedule)).append("\n");
		if (!isBlank(schedule.getTeacherName())) {
			builder.append("教師：").append(schedule.getTeacherName().trim()).append("\n");
		}
		String typeText = scheduleTypeText(schedule);
		if (!typeText.isBlank()) {
			builder.append("類型：").append(typeText).append("\n");
		}
	}

	private String scheduleLineTitle(WeeklyScheduleDto schedule) {
		String courseName = isBlank(schedule.getCourseName()) ? "" : schedule.getCourseName().trim();
		String className = isBlank(schedule.getClassName()) ? "" : schedule.getClassName().trim();
		if (courseName.isBlank()) {
			return className;
		}
		if (className.isBlank() || className.contains(courseName)) {
			return className.isBlank() ? courseName : className;
		}
		return courseName + " " + className;
	}

	private String scheduleTypeText(WeeklyScheduleDto schedule) {
		if (schedule.getScheduleType() == ScheduleType.MAKE_UP) {
			return "補課";
		}
		if (schedule.getScheduleType() == ScheduleType.RESCHEDULED) {
			return "調課";
		}
		return "";
	}

	private String formatFullDate(LocalDate date) {
		return DATE_FORMATTER.format(date) + "（" + weekdayText(date) + "）";
	}

	private String formatShortDate(LocalDate date) {
		return SHORT_DATE_FORMATTER.format(date) + "（" + weekdayText(date) + "）";
	}

	private String formatTimeRange(WeeklyScheduleDto schedule) {
		LocalDateTime start = schedule.getStartTime();
		LocalDateTime end = schedule.getEndTime();
		return TIME_FORMATTER.format(start) + "-" + TIME_FORMATTER.format(end);
	}

	private String weekdayText(LocalDate date) {
		return switch (date.getDayOfWeek()) {
			case MONDAY -> "一";
			case TUESDAY -> "二";
			case WEDNESDAY -> "三";
			case THURSDAY -> "四";
			case FRIDAY -> "五";
			case SATURDAY -> "六";
			case SUNDAY -> "日";
		};
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private record StudentWeeklySchedules(Student student, List<WeeklyScheduleDto> schedules) {
	}
}
