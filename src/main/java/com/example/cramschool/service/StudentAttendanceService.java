package com.example.cramschool.service;

import java.time.DayOfWeek;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.dto.AttendanceStats;
import com.example.cramschool.dto.CardCheckInRequest;
import com.example.cramschool.dto.CardCheckInResponse;
import com.example.cramschool.entity.AttendanceStatus;
import com.example.cramschool.entity.ClassRoom;
import com.example.cramschool.entity.ClassSchedule;
import com.example.cramschool.entity.ClassStudent;
import com.example.cramschool.entity.ScheduleType;
import com.example.cramschool.entity.Student;
import com.example.cramschool.entity.StudentAttendance;
import com.example.cramschool.entity.Teacher;
import com.example.cramschool.entity.TeacherStatus;
import com.example.cramschool.form.StudentAttendanceEntryForm;
import com.example.cramschool.form.StudentAttendanceForm;
import com.example.cramschool.repository.ClassStudentRepository;
import com.example.cramschool.repository.StudentAttendanceRepository;
import com.example.cramschool.repository.StudentRepository;
import com.example.cramschool.repository.TeacherRepository;

@Service
@Transactional
public class StudentAttendanceService {

	public record MonthlyStudentAttendanceRate(Long studentId, String studentSlug, String studentName,
			double presentRate, double lateRate, double absentRate, long presentCount, long lateCount,
			long absentCount, long totalCount) {
	}

	public record StudentAttendanceDetail(Long studentId, String studentName, LocalDate attendanceDate,
			String className, String classTime, String statusLabel, String statusBadgeClass, String note) {
	}

	private static final Map<DayOfWeek, String> WEEKDAY_NAMES = Map.of(
			DayOfWeek.MONDAY, "星期一",
			DayOfWeek.TUESDAY, "星期二",
			DayOfWeek.WEDNESDAY, "星期三",
			DayOfWeek.THURSDAY, "星期四",
			DayOfWeek.FRIDAY, "星期五",
			DayOfWeek.SATURDAY, "星期六",
			DayOfWeek.SUNDAY, "星期日");
	private static final DateTimeFormatter ARRIVAL_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

	private final StudentAttendanceRepository studentAttendanceRepository;
	private final StudentRepository studentRepository;
	private final ClassRoomService classRoomService;
	private final ClassStudentService classStudentService;
	private final ClassStudentRepository classStudentRepository;
	private final TeacherRepository teacherRepository;
	private final TeacherAttendanceService teacherAttendanceService;
	private final WeeklyScheduleService weeklyScheduleService;
	private final LineNotificationService lineNotificationService;
	private Clock clock = Clock.systemDefaultZone();

	public StudentAttendanceService(StudentAttendanceRepository studentAttendanceRepository,
			StudentRepository studentRepository, ClassRoomService classRoomService,
			ClassStudentService classStudentService, ClassStudentRepository classStudentRepository,
			TeacherRepository teacherRepository, TeacherAttendanceService teacherAttendanceService,
			WeeklyScheduleService weeklyScheduleService, LineNotificationService lineNotificationService) {
		this.studentAttendanceRepository = studentAttendanceRepository;
		this.studentRepository = studentRepository;
		this.classRoomService = classRoomService;
		this.classStudentService = classStudentService;
		this.classStudentRepository = classStudentRepository;
		this.teacherRepository = teacherRepository;
		this.teacherAttendanceService = teacherAttendanceService;
		this.weeklyScheduleService = weeklyScheduleService;
		this.lineNotificationService = lineNotificationService;
	}

	@Transactional(readOnly = true)
	public List<StudentAttendance> findByStudentId(Long studentId) {
		return studentAttendanceRepository.findByStudentIdOrderByAttendanceDateDescIdDesc(studentId);
	}

	@Transactional(readOnly = true)
	public List<StudentAttendance> findByClassRoomId(Long classRoomId) {
		return studentAttendanceRepository.findByClassRoomIdOrderByAttendanceDateDescStudentChineseNameAsc(classRoomId);
	}

	@Transactional(readOnly = true)
	public List<StudentAttendance> findAllForAnalysis() {
		return studentAttendanceRepository.findAllByOrderByAttendanceDateDescIdDesc();
	}

	@Transactional(readOnly = true)
	public StudentAttendanceForm buildForm(Long classRoomId, LocalDate attendanceDate) {
		LocalDate targetDate = attendanceDate == null ? LocalDate.now() : attendanceDate;
		Map<Long, StudentAttendance> attendancesByStudentId = new LinkedHashMap<>();
		for (StudentAttendance attendance : studentAttendanceRepository
				.findByClassRoomIdAndAttendanceDateOrderByStudentChineseNameAsc(classRoomId, targetDate)) {
			attendancesByStudentId.put(attendance.getStudent().getId(), attendance);
		}

		StudentAttendanceForm form = new StudentAttendanceForm();
		form.setAttendanceDate(targetDate);
		for (ClassStudent classStudent : classStudentService.findActiveByClassRoomId(classRoomId)) {
			Student student = classStudent.getStudent();
			StudentAttendance attendance = attendancesByStudentId.get(student.getId());
			StudentAttendanceEntryForm entry = new StudentAttendanceEntryForm();
			entry.setStudentId(student.getId());
			entry.setStudentName(student.getDisplayName());
			entry.setStudentGrade(student.getGrade());
			if (attendance != null) {
				entry.setStatus(attendance.getStatus());
				entry.setNote(attendance.getNote());
				entry.setCheckInTime(attendance.getCheckInTime() == null ? null : attendance.getCheckInTime().toLocalTime());
				entry.setCheckOutTime(attendance.getCheckOutTime() == null ? null : attendance.getCheckOutTime().toLocalTime());
			}
			form.getEntries().add(entry);
		}
		return form;
	}

	@Transactional(readOnly = true)
	public boolean isClassDay(Long classRoomId, LocalDate attendanceDate) {
		LocalDate targetDate = attendanceDate == null ? LocalDate.now() : attendanceDate;
		return actualClassDaySchedules(classRoomId, targetDate).stream()
				.anyMatch(schedule -> schedule.getScheduleType() != ScheduleType.CANCELLED);
	}

	@Transactional(readOnly = true)
	public LocalDate resolveClassDay(Long classRoomId, LocalDate attendanceDate) {
		LocalDate targetDate = attendanceDate == null ? LocalDate.now() : attendanceDate;
		if (isClassDay(classRoomId, targetDate)) {
			return targetDate;
		}
		List<DayOfWeek> classDays = classDays(classRoomId);
		if (classDays.isEmpty()) {
			return targetDate;
		}
		return shiftToClassDay(targetDate, classDays, 1, true);
	}

	@Transactional(readOnly = true)
	public LocalDate previousClassDay(Long classRoomId, LocalDate attendanceDate) {
		LocalDate targetDate = attendanceDate == null ? LocalDate.now() : attendanceDate;
		return shiftToActualClassDay(classRoomId, targetDate.minusDays(1), -1);
	}

	@Transactional(readOnly = true)
	public LocalDate nextClassDay(Long classRoomId, LocalDate attendanceDate) {
		LocalDate targetDate = attendanceDate == null ? LocalDate.now() : attendanceDate;
		return shiftToActualClassDay(classRoomId, targetDate.plusDays(1), 1);
	}

	@Transactional(readOnly = true)
	public List<Integer> classDayValues(Long classRoomId) {
		return classDays(classRoomId).stream()
				.map(DayOfWeek::getValue)
				.toList();
	}

	public void saveAttendance(Long classRoomId, StudentAttendanceForm form) {
		ClassRoom classRoom = classRoomService.findById(classRoomId);
		LocalDate attendanceDate = form.getAttendanceDate() == null ? LocalDate.now() : form.getAttendanceDate();
		if (!isClassDay(classRoomId, attendanceDate)) {
			throw new IllegalArgumentException("此日期不是班級上課日，無法儲存點名");
		}
		for (StudentAttendanceEntryForm entry : form.getEntries()) {
			Student student = studentRepository.findById(entry.getStudentId())
					.orElseThrow(() -> new IllegalArgumentException("找不到學生資料"));
			StudentAttendance attendance = studentAttendanceRepository
					.findByClassRoomIdAndStudentIdAndAttendanceDate(classRoomId, entry.getStudentId(), attendanceDate)
					.orElseGet(StudentAttendance::new);
			boolean hadCheckInTime = attendance.getCheckInTime() != null;
			boolean hadCheckOutTime = attendance.getCheckOutTime() != null;
			attendance.setClassRoom(classRoom);
			attendance.setStudent(student);
			attendance.setAttendanceDate(attendanceDate);
			attendance.setStatus(entry.getStatus() == null ? AttendanceStatus.PRESENT : entry.getStatus());
			attendance.setNote(entry.getNote());
			applyManualAttendanceTimes(attendance, entry, attendanceDate);
			StudentAttendance savedAttendance = studentAttendanceRepository.save(attendance);
			sendManualAttendanceNotifications(savedAttendance, !hadCheckInTime, !hadCheckOutTime);
		}
	}

	private void applyManualAttendanceTimes(StudentAttendance attendance, StudentAttendanceEntryForm entry,
			LocalDate attendanceDate) {
		AttendanceStatus status = entry.getStatus() == null ? AttendanceStatus.PRESENT : entry.getStatus();
		if (status == AttendanceStatus.ABSENT || status == AttendanceStatus.LEAVE) {
			attendance.setCheckInTime(null);
			attendance.setCheckOutTime(null);
			return;
		}
		attendance.setCheckInTime(entry.getCheckInTime() == null
				? null
				: LocalDateTime.of(attendanceDate, entry.getCheckInTime()));
		attendance.setCheckOutTime(entry.getCheckOutTime() == null
				? null
				: LocalDateTime.of(attendanceDate, entry.getCheckOutTime()));
	}

	private void sendManualAttendanceNotifications(StudentAttendance attendance,
			boolean canSendCheckIn, boolean canSendCheckOut) {
		if (attendance == null) {
			return;
		}
		if (canSendCheckIn && attendance.getCheckInTime() != null) {
			sendCardCheckInNotification(attendance);
		}
		if (canSendCheckOut && attendance.getCheckOutTime() != null) {
			sendCardCheckOutNotification(attendance);
		}
	}

	public CardCheckInResponse cardCheckIn(CardCheckInRequest request) {
		String normalizedCardId = CardIdNormalizer.normalize(request == null ? null : request.getCardId());
		if (normalizedCardId == null) {
			CardCheckInResponse response = CardCheckInResponse.fail("INVALID_CARD_ID", "請輸入卡號");
			response.setCardId(request == null ? null : request.getCardId());
			return response;
		}
		Optional<Student> studentOptional = studentRepository.findByCardId(normalizedCardId);
		if (studentOptional.isEmpty()) {
			return teacherCardCheckIn(normalizedCardId, request);
		}

		Student student = studentOptional.get();
		if (!"ACTIVE".equals(student.getCardStatus())) {
			CardCheckInResponse response = CardCheckInResponse.fail("CARD_DISABLED", student.getDisplayName() + " 的卡片已停用");
			response.setStudentId(student.getId());
			response.setStudentName(student.getDisplayName());
			response.setCardId(normalizedCardId);
			return response;
		}

		LocalDateTime cardTime = LocalDateTime.now(clock);
		LocalDate attendanceDate = cardTime.toLocalDate();
		Optional<StudentAttendance> openAttendance = studentAttendanceRepository
				.findByStudentIdAndAttendanceDateOrderByIdDesc(student.getId(), attendanceDate)
				.stream()
				.filter(attendance -> attendance.getCheckInTime() != null && attendance.getCheckOutTime() == null)
				.findFirst();
		if (openAttendance.isPresent()) {
			StudentAttendance attendance = openAttendance.get();
			attendance.setCheckOutTime(cardTime);
			attendance.setCheckMethod("CARD");
			attendance.setDeviceName(normalizeDeviceName(request == null ? null : request.getDeviceName()));
			attendance.setCardId(normalizedCardId);
			studentAttendanceRepository.save(attendance);
			sendCardCheckOutNotification(attendance);

			CardCheckInResponse response = CardCheckInResponse.studentCheckOut(
					student.getId(), student.getDisplayName(), attendance.getClassRoom().getDisplayName(),
					attendance.getCheckInTime(), cardTime);
			response.setCardId(normalizedCardId);
			return response;
		}

		Optional<CardClassMatch> matchOptional = findTodayCardClass(student, cardTime);
		if (matchOptional.isEmpty()) {
			CardCheckInResponse response = CardCheckInResponse.fail("NO_CLASS_TODAY",
					student.getDisplayName() + " 今日沒有需要點名的課程");
			response.setStudentId(student.getId());
			response.setStudentName(student.getDisplayName());
			response.setCardId(normalizedCardId);
			return response;
		}

		CardClassMatch match = matchOptional.get();
		Optional<StudentAttendance> existingAttendance = studentAttendanceRepository
				.findByClassRoomIdAndStudentIdAndAttendanceDate(match.classRoom().getId(), student.getId(), attendanceDate);
		if (existingAttendance.isPresent()) {
			CardCheckInResponse response = CardCheckInResponse.fail("DUPLICATE_CHECK_IN",
					student.getDisplayName() + "已完成本堂課點名與簽退，請勿重複刷卡");
			response.setStudentId(student.getId());
			response.setStudentName(student.getDisplayName());
			response.setClassName(match.classRoom().getDisplayName());
			response.setCardId(normalizedCardId);
			response.setCheckInTime(existingAttendance.get().getCheckInTime());
			response.setCheckOutTime(existingAttendance.get().getCheckOutTime());
			return response;
		}

		StudentAttendance attendance = new StudentAttendance();
		attendance.setStudent(student);
		attendance.setClassRoom(match.classRoom());
		attendance.setAttendanceDate(attendanceDate);
		attendance.setStatus(resolveCardAttendanceStatus(match.schedule(), cardTime));
		attendance.setNote("到班時間：" + cardTime.toLocalTime().format(ARRIVAL_TIME_FORMATTER));
		attendance.setCheckMethod("CARD");
		attendance.setDeviceName(normalizeDeviceName(request == null ? null : request.getDeviceName()));
		attendance.setCardId(normalizedCardId);
		attendance.setCheckInTime(cardTime);
		studentAttendanceRepository.save(attendance);
		sendCardCheckInNotification(attendance);

		CardCheckInResponse response = CardCheckInResponse.success(
				student.getId(), student.getDisplayName(), match.classRoom().getDisplayName(), cardTime);
		response.setCardId(normalizedCardId);
		return response;
	}

	private CardCheckInResponse teacherCardCheckIn(String normalizedCardId, CardCheckInRequest request) {
		Optional<Teacher> teacherOptional = teacherRepository.findByCardId(normalizedCardId);
		if (teacherOptional.isEmpty()) {
			CardCheckInResponse response = CardCheckInResponse.fail("CARD_NOT_BOUND", "此卡尚未綁定學生或教師");
			response.setCardId(normalizedCardId);
			return response;
		}
		Teacher teacher = teacherOptional.get();
		if (teacher.getStatus() != TeacherStatus.ACTIVE || !"ACTIVE".equals(teacher.getCardStatus())) {
			CardCheckInResponse response = CardCheckInResponse.fail("CARD_DISABLED", teacher.getDisplayName() + " 的卡片已停用");
			response.setTeacherId(teacher.getId());
			response.setTeacherName(teacher.getDisplayName());
			response.setPersonType("TEACHER");
			response.setCardId(normalizedCardId);
			return response;
		}
		LocalDateTime checkInTime = LocalDateTime.now(clock);
		try {
			var attendance = teacherAttendanceService.cardClock(teacher, normalizedCardId,
					request == null ? null : request.getDeviceName(), checkInTime);
			boolean clockedOut = attendance.getClockOutTime() != null;
			CardCheckInResponse response = CardCheckInResponse.teacherSuccess(
					teacher.getId(),
					teacher.getDisplayName(),
					clockedOut ? "CLOCKED_OUT" : "CLOCKED_IN",
					teacher.getDisplayName() + (clockedOut ? " 下班打卡成功" : " 上班打卡成功"),
					checkInTime);
			response.setCardId(normalizedCardId);
			return response;
		} catch (IllegalStateException ex) {
			CardCheckInResponse response = CardCheckInResponse.fail("DUPLICATE_CHECK_IN", ex.getMessage());
			response.setTeacherId(teacher.getId());
			response.setTeacherName(teacher.getDisplayName());
			response.setPersonType("TEACHER");
			response.setCardId(normalizedCardId);
			return response;
		}
	}

	@Transactional(readOnly = true)
	public AttendanceStats calculateStatsByClassRoomId(Long classRoomId) {
		return calculateStats(findByClassRoomId(classRoomId));
	}

	@Transactional(readOnly = true)
	public List<MonthlyStudentAttendanceRate> calculateStudentAttendanceRates(YearMonth month) {
		YearMonth targetMonth = month;
		Map<Long, StudentMonthlyAttendanceCounter> counters = new LinkedHashMap<>();
		for (Student student : studentRepository.findByActiveTrueOrderByChineseNameAsc()) {
			counters.put(student.getId(), new StudentMonthlyAttendanceCounter(
					student.getId(), student.getUrlSlug(), student.getDisplayName()));
		}
		for (StudentAttendance attendance : studentAttendanceRepository.findAll()) {
			if (targetMonth != null && !YearMonth.from(attendance.getAttendanceDate()).equals(targetMonth)) {
				continue;
			}
			StudentMonthlyAttendanceCounter counter = counters.computeIfAbsent(
					attendance.getStudent().getId(),
					studentId -> new StudentMonthlyAttendanceCounter(
							attendance.getStudent().getId(),
							attendance.getStudent().getUrlSlug(),
							attendance.getStudent().getDisplayName()));
			counter.add(attendance.getStatus());
		}
		return counters.values().stream()
				.map(StudentMonthlyAttendanceCounter::toRate)
				.filter(rate -> rate.totalCount() > 0)
				.sorted(Comparator.comparingDouble(MonthlyStudentAttendanceRate::presentRate).reversed()
						.thenComparingDouble(MonthlyStudentAttendanceRate::absentRate)
						.thenComparing(MonthlyStudentAttendanceRate::studentName, Comparator.nullsLast(String::compareTo)))
				.toList();
	}

	@Transactional(readOnly = true)
	public Map<Long, List<StudentAttendanceDetail>> buildStudentAttendanceDetails(YearMonth month) {
		YearMonth targetMonth = month;
		Map<Long, List<StudentAttendanceDetail>> detailsByStudentId = new LinkedHashMap<>();
		for (StudentAttendance attendance : studentAttendanceRepository.findAllByOrderByAttendanceDateDescIdDesc()) {
			if (targetMonth != null && !YearMonth.from(attendance.getAttendanceDate()).equals(targetMonth)) {
				continue;
			}
			Student student = attendance.getStudent();
			if (student == null) {
				continue;
			}
			detailsByStudentId.computeIfAbsent(student.getId(), ignored -> new java.util.ArrayList<>())
					.add(new StudentAttendanceDetail(
							student.getId(),
							student.getDisplayName(),
							attendance.getAttendanceDate(),
							attendance.getClassRoom() == null ? "-" : attendance.getClassRoom().getDisplayName(),
							attendance.getClassRoom() == null ? "-" : attendance.getClassRoom().getTimeRangeText(),
							statusLabel(attendance.getStatus()),
							statusBadgeClass(attendance.getStatus()),
							formatAttendanceNote(attendance)));
		}
		return detailsByStudentId;
	}

	private AttendanceStats calculateStats(List<StudentAttendance> attendances) {
		AttendanceStats stats = new AttendanceStats();
		stats.setTotalCount(attendances.size());
		for (StudentAttendance attendance : attendances) {
			if (attendance.getStatus() == AttendanceStatus.PRESENT) {
				stats.setPresentCount(stats.getPresentCount() + 1);
			} else if (attendance.getStatus() == AttendanceStatus.LATE) {
				stats.setLateCount(stats.getLateCount() + 1);
			} else if (attendance.getStatus() == AttendanceStatus.ABSENT) {
				stats.setAbsentCount(stats.getAbsentCount() + 1);
			} else if (attendance.getStatus() == AttendanceStatus.LEAVE) {
				stats.setLeaveCount(stats.getLeaveCount() + 1);
			}
		}
		return stats;
	}

	private String statusLabel(AttendanceStatus status) {
		if (status == null) {
			return "缺席";
		}
		return switch (status) {
			case PRESENT -> "出席";
			case LATE -> "遲到";
			case ABSENT, LEAVE -> "缺席";
		};
	}

	private String statusBadgeClass(AttendanceStatus status) {
		if (status == null) {
			return "text-bg-danger";
		}
		return switch (status) {
			case PRESENT -> "text-bg-success";
			case LATE -> "text-bg-warning text-dark";
			case ABSENT, LEAVE -> "text-bg-danger";
		};
	}

	private String formatAttendanceNote(StudentAttendance attendance) {
		String note = attendance == null ? null : attendance.getNote();
		boolean hasNote = note != null && !note.isBlank();
		if (attendance != null && attendance.getStatus() == AttendanceStatus.LEAVE) {
			return hasNote ? "請假：" + note.trim() : "請假";
		}
		return hasNote ? note.trim() : "-";
	}

	private List<DayOfWeek> classDays(Long classRoomId) {
		ClassRoom classRoom = classRoomService.findById(classRoomId);
		return classRoom.getEffectiveSchedules().stream()
				.map(schedule -> weekday(schedule.getWeekday()))
				.filter(day -> day != null)
				.distinct()
				.toList();
	}

	private LocalDate shiftToClassDay(LocalDate date, List<DayOfWeek> classDays, int direction, boolean includeDate) {
		LocalDate candidate = date;
		if (!includeDate) {
			candidate = candidate.plusDays(direction);
		}
		for (int i = 0; i < 7; i += 1) {
			if (classDays.contains(candidate.getDayOfWeek())) {
				return candidate;
			}
			candidate = candidate.plusDays(direction);
		}
		return date;
	}

	private LocalDate shiftToActualClassDay(Long classRoomId, LocalDate date, int direction) {
		LocalDate candidate = date;
		for (int i = 0; i < 60; i += 1) {
			if (isClassDay(classRoomId, candidate)) {
				return candidate;
			}
			candidate = candidate.plusDays(direction);
		}
		return date;
	}

	private List<com.example.cramschool.dto.WeeklyScheduleDto> actualClassDaySchedules(
			Long classRoomId, LocalDate targetDate) {
		if (weeklyScheduleService == null) {
			return fallbackClassDaySchedules(classRoomId, targetDate);
		}
		return weeklyScheduleService.findWeeklySchedules(targetDate, null, true, null, classRoomId).stream()
				.filter(schedule -> targetDate.equals(schedule.getCourseDate()))
				.toList();
	}

	private List<com.example.cramschool.dto.WeeklyScheduleDto> fallbackClassDaySchedules(
			Long classRoomId, LocalDate targetDate) {
		ClassRoom classRoom = classRoomService.findById(classRoomId);
		String weekday = WEEKDAY_NAMES.get(targetDate.getDayOfWeek());
		return classRoom.getEffectiveSchedules().stream()
				.filter(schedule -> weekday.equals(schedule.getWeekday()))
				.map(schedule -> new com.example.cramschool.dto.WeeklyScheduleDto(
						schedule.getId(),
						null,
						classRoom.getId(),
						classRoom.getSubjectName(),
						classRoom.getDisplayName(),
						classRoom.getTeacherName(),
						targetDate,
						LocalDateTime.of(targetDate, schedule.getStartTime()),
						LocalDateTime.of(targetDate, schedule.getEndTime()),
						ScheduleType.NORMAL,
						null,
						null,
						classRoom.getSubject() == null ? "未指定" : String.valueOf(classRoom.getSubject().getId()),
						classRoom.getTeacher() == null ? "未指定" : String.valueOf(classRoom.getTeacher().getId()),
						classRoom.getGrade() == null || classRoom.getGrade().isBlank() ? "未指定" : classRoom.getGrade()))
				.toList();
	}

	private DayOfWeek weekday(String weekdayName) {
		for (Map.Entry<DayOfWeek, String> entry : WEEKDAY_NAMES.entrySet()) {
			if (entry.getValue().equals(weekdayName)) {
				return entry.getKey();
			}
		}
		return null;
	}

	void setClock(Clock clock) {
		this.clock = clock == null ? Clock.systemDefaultZone() : clock;
	}

	private Optional<CardClassMatch> findTodayCardClass(Student student, LocalDateTime checkInTime) {
		String weekdayName = WEEKDAY_NAMES.get(checkInTime.getDayOfWeek());
		return classStudentRepository.findByStudentIdAndActiveTrue(student.getId()).stream()
				.map(ClassStudent::getClassRoom)
				.filter(ClassRoom::isActive)
				.flatMap(classRoom -> classRoom.getEffectiveSchedules().stream()
						.filter(schedule -> weekdayName.equals(schedule.getWeekday()))
						.filter(schedule -> isWithinCardCheckInWindow(schedule, checkInTime))
						.map(schedule -> new CardClassMatch(classRoom, schedule, distanceFromSchedule(schedule, checkInTime))))
				.min(Comparator.comparingLong(CardClassMatch::distanceSeconds));
	}

	private boolean isWithinCardCheckInWindow(ClassSchedule schedule, LocalDateTime checkInTime) {
		LocalDate date = checkInTime.toLocalDate();
		LocalDateTime start = LocalDateTime.of(date, schedule.getStartTime()).minusMinutes(30);
		LocalDateTime end = LocalDateTime.of(date, schedule.getEndTime()).plusMinutes(30);
		return !checkInTime.isBefore(start) && !checkInTime.isAfter(end);
	}

	private long distanceFromSchedule(ClassSchedule schedule, LocalDateTime checkInTime) {
		LocalDate date = checkInTime.toLocalDate();
		LocalTime checkInLocalTime = checkInTime.toLocalTime();
		if (!checkInLocalTime.isBefore(schedule.getStartTime()) && !checkInLocalTime.isAfter(schedule.getEndTime())) {
			return 0;
		}
		LocalDateTime start = LocalDateTime.of(date, schedule.getStartTime());
		LocalDateTime end = LocalDateTime.of(date, schedule.getEndTime());
		return Math.min(
				Math.abs(ChronoUnit.SECONDS.between(checkInTime, start)),
				Math.abs(ChronoUnit.SECONDS.between(checkInTime, end)));
	}

	private AttendanceStatus resolveCardAttendanceStatus(ClassSchedule schedule, LocalDateTime checkInTime) {
		if (schedule != null && schedule.getStartTime() != null
				&& checkInTime.toLocalTime().isAfter(schedule.getStartTime().plusMinutes(5))) {
			return AttendanceStatus.LATE;
		}
		return AttendanceStatus.PRESENT;
	}

	private void sendCardCheckInNotification(StudentAttendance attendance) {
		if (lineNotificationService == null) {
			return;
		}
		try {
			lineNotificationService.sendCardCheckInNotification(attendance);
		} catch (RuntimeException ignored) {
			// 點名結果以刷卡成功為主，LINE 發送失敗會由通知紀錄保留，不阻斷櫃台流程。
		}
	}

	private void sendCardCheckOutNotification(StudentAttendance attendance) {
		if (lineNotificationService == null) {
			return;
		}
		try {
			lineNotificationService.sendCardCheckOutNotification(attendance);
		} catch (RuntimeException ignored) {
			// 點名結果以刷卡成功為主，LINE 發送失敗會由通知紀錄保留，不阻斷櫃台流程。
		}
	}

	private String normalizeDeviceName(String deviceName) {
		if (deviceName == null || deviceName.isBlank()) {
			return null;
		}
		String normalized = deviceName.trim();
		return normalized.length() > 100 ? normalized.substring(0, 100) : normalized;
	}

	private record CardClassMatch(ClassRoom classRoom, ClassSchedule schedule, long distanceSeconds) {
	}

	private static class StudentMonthlyAttendanceCounter {

		private final Long studentId;
		private final String studentSlug;
		private final String studentName;
		private long presentCount;
		private long lateCount;
		private long absentCount;

		StudentMonthlyAttendanceCounter(Long studentId, String studentSlug, String studentName) {
			this.studentId = studentId;
			this.studentSlug = studentSlug;
			this.studentName = studentName;
		}

		void add(AttendanceStatus status) {
			if (status == AttendanceStatus.PRESENT) {
				presentCount++;
				return;
			}
			if (status == AttendanceStatus.LATE) {
				lateCount++;
				return;
			}
			if (status == AttendanceStatus.ABSENT || status == AttendanceStatus.LEAVE) {
				absentCount++;
			}
		}

		MonthlyStudentAttendanceRate toRate() {
			long totalCount = presentCount + lateCount + absentCount;
			double presentRate = totalCount == 0 ? 0 : presentCount * 100.0 / totalCount;
			double lateRate = totalCount == 0 ? 0 : lateCount * 100.0 / totalCount;
			double absentRate = totalCount == 0 ? 0 : absentCount * 100.0 / totalCount;
			return new MonthlyStudentAttendanceRate(studentId, studentSlug, studentName,
					presentRate, lateRate, absentRate, presentCount, lateCount, absentCount, totalCount);
		}
	}
}
