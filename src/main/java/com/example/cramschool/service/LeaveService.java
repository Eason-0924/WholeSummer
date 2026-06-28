package com.example.cramschool.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.dto.TeacherCourseOption;
import com.example.cramschool.entity.ClassRoom;
import com.example.cramschool.entity.ClassSchedule;
import com.example.cramschool.entity.LeaveStatus;
import com.example.cramschool.entity.Teacher;
import com.example.cramschool.entity.TeacherLeave;
import com.example.cramschool.repository.ClassRoomRepository;
import com.example.cramschool.repository.ClassScheduleRepository;
import com.example.cramschool.repository.TeacherLeaveRepository;
import com.example.cramschool.repository.TeacherRepository;

@Service
@Transactional
public class LeaveService {

	private static final Map<DayOfWeek, String> WEEKDAY_NAMES = Map.of(
			DayOfWeek.MONDAY, "星期一",
			DayOfWeek.TUESDAY, "星期二",
			DayOfWeek.WEDNESDAY, "星期三",
			DayOfWeek.THURSDAY, "星期四",
			DayOfWeek.FRIDAY, "星期五",
			DayOfWeek.SATURDAY, "星期六",
			DayOfWeek.SUNDAY, "星期日");

	private final TeacherLeaveRepository teacherLeaveRepository;
	private final TeacherRepository teacherRepository;
	private final ClassScheduleRepository classScheduleRepository;
	private final ClassRoomRepository classRoomRepository;
	private final TeacherAttendanceService teacherAttendanceService;
	private final MakeUpClassService makeUpClassService;

	public LeaveService(TeacherLeaveRepository teacherLeaveRepository,
			TeacherRepository teacherRepository,
			ClassScheduleRepository classScheduleRepository,
			ClassRoomRepository classRoomRepository,
			TeacherAttendanceService teacherAttendanceService,
			MakeUpClassService makeUpClassService) {
		this.teacherLeaveRepository = teacherLeaveRepository;
		this.teacherRepository = teacherRepository;
		this.classScheduleRepository = classScheduleRepository;
		this.classRoomRepository = classRoomRepository;
		this.teacherAttendanceService = teacherAttendanceService;
		this.makeUpClassService = makeUpClassService;
	}

	@Transactional(readOnly = true)
	public List<TeacherLeave> findByTeacherAndDate(Long teacherId, LocalDate leaveDate) {
		return teacherLeaveRepository.findByTeacherIdAndLeaveDateOrderByIdDesc(
				teacherId, leaveDate == null ? LocalDate.now() : leaveDate);
	}

	@Transactional(readOnly = true)
	public List<TeacherCourseOption> findCourseOptions(Long teacherId) {
		return classRoomRepository.findByTeacherIdAndActiveTrueOrderByGradeAscIdAsc(teacherId).stream()
				.flatMap(classRoom -> classRoom.getEffectiveSchedules().stream()
						.map(schedule -> new TeacherCourseOption(
								schedule.getId(),
								teacherId,
								schedule.getWeekday(),
								schedule.getTimeRangeText(),
								classRoom.getDisplayName())))
				.sorted(Comparator.comparing(TeacherCourseOption::weekday)
						.thenComparing(TeacherCourseOption::timeRangeText)
						.thenComparing(TeacherCourseOption::className))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<TeacherCourseOption> findAllCourseOptions() {
		return classRoomRepository.findByActiveTrue().stream()
				.flatMap(classRoom -> classRoom.getEffectiveSchedules().stream()
						.map(schedule -> new TeacherCourseOption(
								schedule.getId(),
								classRoom.getTeacher() == null ? null : classRoom.getTeacher().getId(),
								schedule.getWeekday(),
								schedule.getTimeRangeText(),
								classRoom.getTeacherName() + "｜" + classRoom.getDisplayName())))
				.sorted(Comparator.comparing(TeacherCourseOption::className)
						.thenComparing(TeacherCourseOption::weekday)
						.thenComparing(TeacherCourseOption::timeRangeText))
				.toList();
	}

	public TeacherLeave createLeave(Long currentTeacherId, LocalDate leaveDate,
			Long courseScheduleId, String reason) {
		if (leaveDate == null) {
			throw new IllegalArgumentException("請選擇請假日期");
		}
		String normalizedReason = reason == null ? null : reason.trim();
		if (normalizedReason != null && normalizedReason.length() > 255) {
			throw new IllegalArgumentException("請假原因不可超過 255 個字");
		}

		Teacher teacher = teacherRepository.findById(currentTeacherId)
				.orElseThrow(() -> new IllegalArgumentException("找不到教師資料"));
		if (courseScheduleId == null) {
			return createFullDayLeave(currentTeacherId, leaveDate, normalizedReason, teacher);
		}
		return createCourseLeave(currentTeacherId, leaveDate, courseScheduleId, normalizedReason, teacher);
	}

	private TeacherLeave createFullDayLeave(Long teacherId, LocalDate leaveDate,
			String normalizedReason, Teacher teacher) {
		List<ClassSchedule> dailySchedules = findDailySchedules(teacherId, leaveDate);
		if (dailySchedules.isEmpty()) {
			throw new IllegalArgumentException("當日無課程，無法建立請假");
		}
		TeacherLeave firstSavedLeave = null;
		for (ClassSchedule schedule : dailySchedules) {
			if (hasApprovedLeave(teacherId, leaveDate, schedule)) {
				continue;
			}
			TeacherLeave savedLeave = saveLeave(teacher, leaveDate, schedule, normalizedReason);
			makeUpClassService.createRequiredMakeUpFromLeave(savedLeave);
			if (firstSavedLeave == null) {
				firstSavedLeave = savedLeave;
			}
		}
		if (firstSavedLeave == null) {
			throw new IllegalArgumentException("當日所有課程皆已登記請假");
		}
		teacherAttendanceService.recordLeave(teacherId, leaveDate, buildAttendanceNote(normalizedReason));
		return firstSavedLeave;
	}

	private TeacherLeave createCourseLeave(Long currentTeacherId, LocalDate leaveDate,
			Long courseScheduleId, String normalizedReason, Teacher teacher) {
		ClassSchedule courseSchedule = findOwnedCourseSchedule(currentTeacherId, courseScheduleId);
		if (!WEEKDAY_NAMES.get(leaveDate.getDayOfWeek()).equals(courseSchedule.getWeekday())) {
			throw new IllegalArgumentException("請假課程不屬於請假日期當天");
		}
		if (hasApprovedLeave(currentTeacherId, leaveDate, courseSchedule)) {
			throw new IllegalArgumentException("該日期與課程已登記請假");
		}

		TeacherLeave savedLeave = saveLeave(teacher, leaveDate, courseSchedule, normalizedReason);
		teacherAttendanceService.recordLeave(currentTeacherId, leaveDate, buildAttendanceNote(normalizedReason));
		makeUpClassService.createRequiredMakeUpFromLeave(savedLeave);
		return savedLeave;
	}

	private TeacherLeave saveLeave(Teacher teacher, LocalDate leaveDate,
			ClassSchedule courseSchedule, String normalizedReason) {
		TeacherLeave leave = new TeacherLeave();
		leave.setTeacher(teacher);
		leave.setLeaveDate(leaveDate);
		leave.setCourseSchedule(courseSchedule);
		leave.setReason(normalizedReason == null || normalizedReason.isBlank() ? null : normalizedReason);
		leave.setStatus(LeaveStatus.APPROVED);
		return teacherLeaveRepository.save(leave);
	}

	private String buildAttendanceNote(String normalizedReason) {
		if (normalizedReason == null || normalizedReason.isBlank()) {
			return "請假";
		}
		return "請假：" + normalizedReason;
	}

	private List<ClassSchedule> findDailySchedules(Long teacherId, LocalDate leaveDate) {
		String weekday = WEEKDAY_NAMES.get(leaveDate.getDayOfWeek());
		return classRoomRepository.findByTeacherIdAndActiveTrueOrderByGradeAscIdAsc(teacherId).stream()
				.flatMap(classRoom -> classRoom.getEffectiveSchedules().stream())
				.filter(schedule -> weekday.equals(schedule.getWeekday()))
				.toList();
	}

	private ClassSchedule findOwnedCourseSchedule(Long teacherId, Long courseScheduleId) {
		if (courseScheduleId == null) {
			return null;
		}
		ClassSchedule schedule = classScheduleRepository.findById(courseScheduleId)
				.orElseThrow(() -> new IllegalArgumentException("找不到請假課程"));
		ClassRoom classRoom = schedule.getClassRoom();
		Teacher teacher = classRoom == null ? null : classRoom.getTeacher();
		if (teacher == null || !teacher.getId().equals(teacherId)) {
			throw new IllegalArgumentException("只能針對自己的課程請假");
		}
		return schedule;
	}

	private boolean hasApprovedLeave(Long teacherId, LocalDate leaveDate, ClassSchedule courseSchedule) {
		if (courseSchedule != null) {
			return teacherLeaveRepository.existsByTeacherIdAndLeaveDateAndCourseScheduleIdAndStatus(
					teacherId, leaveDate, courseSchedule.getId(), LeaveStatus.APPROVED);
		}
		return teacherLeaveRepository.findByTeacherIdAndLeaveDateOrderByIdDesc(teacherId, leaveDate).stream()
				.anyMatch(leave -> leave.getStatus() == LeaveStatus.APPROVED
						&& leave.getCourseSchedule() == null);
	}
}
