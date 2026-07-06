package com.example.cramschool.service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.dto.MakeUpAvailableSlot;
import com.example.cramschool.dto.MakeUpSlotOption;
import com.example.cramschool.dto.MakeUpSlotStatus;
import com.example.cramschool.dto.WeeklyScheduleDto;
import com.example.cramschool.entity.ClassRoom;
import com.example.cramschool.entity.ClassSchedule;
import com.example.cramschool.entity.MakeUpClassRequest;
import com.example.cramschool.entity.MakeUpSourceType;
import com.example.cramschool.entity.MakeUpStatus;
import com.example.cramschool.entity.ScheduleType;
import com.example.cramschool.repository.ClassRoomRepository;
import com.example.cramschool.repository.ClassScheduleRepository;
import com.example.cramschool.repository.MakeUpClassRequestRepository;

@Service
@Transactional(readOnly = true)
public class WeeklyScheduleService {

	private static final int SEARCH_DAYS = 30;
	private static final int SLOT_STEP_MINUTES = 30;
	private static final LocalTime WEEKDAY_START = LocalTime.of(14, 0);
	private static final LocalTime WEEKDAY_END = LocalTime.of(22, 0);
	private static final LocalTime WEEKEND_START = LocalTime.of(9, 0);
	private static final LocalTime WEEKEND_END = LocalTime.of(22, 0);

	private final ClassScheduleRepository classScheduleRepository;
	private final ClassRoomRepository classRoomRepository;
	private final MakeUpClassRequestRepository makeUpClassRequestRepository;
	private final ScheduleConflictService scheduleConflictService;
	private final MakeUpClassService makeUpClassService;

	public WeeklyScheduleService(ClassScheduleRepository classScheduleRepository,
			ClassRoomRepository classRoomRepository,
			MakeUpClassRequestRepository makeUpClassRequestRepository,
			ScheduleConflictService scheduleConflictService,
			MakeUpClassService makeUpClassService) {
		this.classScheduleRepository = classScheduleRepository;
		this.classRoomRepository = classRoomRepository;
		this.makeUpClassRequestRepository = makeUpClassRequestRepository;
		this.scheduleConflictService = scheduleConflictService;
		this.makeUpClassService = makeUpClassService;
	}

	public List<WeeklyScheduleDto> findWeeklySchedules(LocalDate date, Long currentTeacherId,
			boolean director, Long teacherFilterId, Long classFilterId) {
		LocalDate weekStart = weekStart(date == null ? LocalDate.now() : date);
		LocalDateTime start = weekStart.atStartOfDay();
		LocalDateTime end = weekStart.plusDays(6).atTime(23, 59, 59);
		List<ClassSchedule> eventSchedules = director
				? classScheduleRepository.findByScheduledStartAtBetweenOrderByScheduledStartAtAsc(start, end)
				: classScheduleRepository.findByClassRoomTeacherIdAndScheduledStartAtBetweenOrderByScheduledStartAtAsc(
						currentTeacherId, start, end);
		eventSchedules = eventSchedules.stream()
				.filter(schedule -> teacherFilterId == null || schedule.getClassRoom() != null
						&& schedule.getClassRoom().getTeacher() != null
						&& teacherFilterId.equals(schedule.getClassRoom().getTeacher().getId()))
				.filter(schedule -> classFilterId == null || schedule.getClassRoom() != null
						&& classFilterId.equals(schedule.getClassRoom().getId()))
				.toList();
		Set<String> cancelledOccurrences = eventSchedules.stream()
				.filter(schedule -> schedule.getScheduleType() == ScheduleType.CANCELLED)
				.filter(schedule -> schedule.getOriginalSchedule() != null && schedule.getCourseDate() != null)
				.map(schedule -> occurrenceKey(schedule.getOriginalSchedule().getId(), schedule.getCourseDate()))
				.collect(Collectors.toSet());
		Set<String> eventCancelledOccurrences = Set.copyOf(cancelledOccurrences);
		List<MakeUpClassRequest> blockedOriginalRequests = findBlockedOriginalRequests(
				weekStart, currentTeacherId, director, teacherFilterId, classFilterId);
		cancelledOccurrences.addAll(blockedOriginalRequests.stream()
				.filter(request -> request.getOriginalCourseSchedule() != null && request.getOriginalCourseDate() != null)
				.map(request -> occurrenceKey(request.getOriginalCourseSchedule().getId(), request.getOriginalCourseDate()))
				.collect(Collectors.toSet()));

		List<WeeklyScheduleDto> rows = new ArrayList<>();
		rows.addAll(buildNormalOccurrences(weekStart, currentTeacherId, director,
				teacherFilterId, classFilterId, cancelledOccurrences));
		rows.addAll(blockedOriginalRequests.stream()
				.filter(request -> request.getOriginalCourseSchedule() != null)
				.filter(request -> !eventCancelledOccurrences.contains(
						occurrenceKey(request.getOriginalCourseSchedule().getId(), request.getOriginalCourseDate())))
				.map(this::toBlockedOriginalDto)
				.toList());
		rows.addAll(eventSchedules.stream()
				.map(this::toEventDto)
				.toList());
		return rows.stream()
				.sorted(Comparator.comparing(WeeklyScheduleDto::getStartTime)
						.thenComparing(WeeklyScheduleDto::getScheduleType)
						.thenComparing(WeeklyScheduleDto::getClassName))
				.toList();
	}

	private List<MakeUpClassRequest> findBlockedOriginalRequests(LocalDate weekStart, Long currentTeacherId,
			boolean director, Long teacherFilterId, Long classFilterId) {
		LocalDate weekEnd = weekStart.plusDays(6);
		List<MakeUpClassRequest> requests = new ArrayList<>();
		if (director) {
			requests.addAll(makeUpClassRequestRepository.findByStatusOrderByOriginalCourseDateAscIdAsc(MakeUpStatus.PENDING));
			requests.addAll(makeUpClassRequestRepository.findByStatusOrderByOriginalCourseDateAscIdAsc(MakeUpStatus.SCHEDULED));
		} else {
			requests.addAll(makeUpClassRequestRepository.findByTeacherIdAndStatusOrderByOriginalCourseDateAscIdAsc(
					currentTeacherId, MakeUpStatus.PENDING));
			requests.addAll(makeUpClassRequestRepository.findByTeacherIdAndStatusOrderByOriginalCourseDateAscIdAsc(
					currentTeacherId, MakeUpStatus.SCHEDULED));
		}
		return requests.stream()
				.filter(request -> request.getOriginalCourseDate() != null
						&& !request.getOriginalCourseDate().isBefore(weekStart)
						&& !request.getOriginalCourseDate().isAfter(weekEnd))
				.filter(request -> teacherFilterId == null || request.getTeacher() != null
						&& teacherFilterId.equals(request.getTeacher().getId()))
				.filter(request -> classFilterId == null || request.getClassRoom() != null
						&& classFilterId.equals(request.getClassRoom().getId()))
				.filter(request -> request.getOriginalCourseSchedule() != null
						&& request.getOriginalCourseSchedule().getStartTime() != null
						&& request.getOriginalCourseSchedule().getEndTime() != null)
				.filter(this::isActualBlockedOriginalRequest)
				.collect(Collectors.groupingBy(
						request -> occurrenceKey(
								request.getOriginalCourseSchedule().getId(), request.getOriginalCourseDate()),
						java.util.LinkedHashMap::new,
						Collectors.minBy(Comparator.comparingInt(this::blockedOriginalPriority))))
				.values()
				.stream()
				.flatMap(Optional::stream)
				.toList();
	}

	private int blockedOriginalPriority(MakeUpClassRequest request) {
		return request.getSourceType() == MakeUpSourceType.ABSENCE ? 1 : 0;
	}

	private boolean isActualBlockedOriginalRequest(MakeUpClassRequest request) {
		ClassSchedule schedule = request.getOriginalCourseSchedule();
		LocalDate date = request.getOriginalCourseDate();
		if (schedule == null || date == null) {
			return false;
		}
		ScheduleType scheduleType = schedule.getScheduleType();
		if (scheduleType == ScheduleType.CANCELLED) {
			return false;
		}
		if (scheduleType == ScheduleType.MAKE_UP || scheduleType == ScheduleType.RESCHEDULED) {
			LocalDate eventDate = schedule.getScheduledStartAt() == null
					? schedule.getCourseDate()
					: schedule.getScheduledStartAt().toLocalDate();
			return date.equals(eventDate);
		}
		return true;
	}

	@Transactional
	public void rescheduleClass(Long originalScheduleId, LocalDate originalDate,
			LocalDateTime newStart, String reason, Long currentTeacherId,
			boolean director, boolean allowTeacherConflict) {
		ClassSchedule original = classScheduleRepository.findById(originalScheduleId)
				.orElseThrow(() -> new IllegalArgumentException("找不到原課程"));
		if (original.getScheduleType() != ScheduleType.NORMAL || original.getClassRoom() == null) {
			throw new IllegalArgumentException("只能調整原課程");
		}
		if (!director && (original.getClassRoom().getTeacher() == null
				|| !currentTeacherId.equals(original.getClassRoom().getTeacher().getId()))) {
			throw new IllegalArgumentException("無權調整此課程");
		}
		if (originalDate == null || newStart == null) {
			throw new IllegalArgumentException("請選擇調課日期與新時間");
		}
		LocalDateTime originalStart = LocalDateTime.of(originalDate, original.getStartTime());
		LocalDateTime originalEnd = LocalDateTime.of(originalDate, original.getEndTime());
		if (!originalStart.isAfter(LocalDateTime.now())) {
			throw new IllegalArgumentException("已開始或已結束的課程不可調課");
		}
		Duration duration = Duration.between(original.getStartTime(), original.getEndTime());
		LocalDateTime newEnd = newStart.plus(duration);
		Long classId = original.getClassRoom().getId();
		Long teacherId = original.getClassRoom().getTeacher() == null ? null
				: original.getClassRoom().getTeacher().getId();
		boolean studentConflict = scheduleConflictService.hasStudentClassConflict(classId, newStart, newEnd)
				|| scheduleConflictService.hasMakeUpConflict(classId, null, newStart, newEnd)
				|| scheduleConflictService.hasRescheduleConflict(classId, null, newStart, newEnd);
		boolean teacherConflict = scheduleConflictService.hasTeacherConflict(teacherId, newStart, newEnd)
				|| scheduleConflictService.hasMakeUpConflict(null, teacherId, newStart, newEnd)
				|| scheduleConflictService.hasRescheduleConflict(null, teacherId, newStart, newEnd);
		if (studentConflict || (teacherConflict && !allowTeacherConflict)) {
			throw new IllegalArgumentException("新時段已有衝突，請重新選擇");
		}

		String normalizedReason = reason == null || reason.isBlank() ? null : reason.trim();
		ClassSchedule cancelled = eventSchedule(original, ScheduleType.CANCELLED,
				originalStart, originalEnd, normalizedReason, currentTeacherId);
		ClassSchedule rescheduled = eventSchedule(original, ScheduleType.RESCHEDULED,
				newStart, newEnd, normalizedReason, currentTeacherId);
		classScheduleRepository.save(cancelled);
		classScheduleRepository.save(rescheduled);
	}

	public List<LocalDate> buildRescheduleCalendarDates(LocalDate baseDate) {
		List<LocalDate> dates = new ArrayList<>();
		LocalDate startDate = (baseDate == null ? LocalDate.now() : baseDate).plusDays(1);
		for (int dayOffset = 0; dayOffset < SEARCH_DAYS; dayOffset++) {
			dates.add(startDate.plusDays(dayOffset));
		}
		return dates;
	}

	public List<MakeUpSlotOption> findRescheduleSlotOptions(Long originalScheduleId, LocalDate date,
			Long currentTeacherId, boolean director) {
		ClassSchedule original = classScheduleRepository.findById(originalScheduleId)
				.orElseThrow(() -> new IllegalArgumentException("找不到原課程"));
		if (original.getClassRoom() == null || original.getClassRoom().getTeacher() == null) {
			throw new IllegalArgumentException("原課程缺少教師資料");
		}
		if (!director && !currentTeacherId.equals(original.getClassRoom().getTeacher().getId())) {
			throw new IllegalArgumentException("無權調整此課程");
		}
		if (date == null || original.getStartTime() == null || original.getEndTime() == null
				|| !original.getEndTime().isAfter(original.getStartTime())) {
			return List.of();
		}
		Duration duration = Duration.between(original.getStartTime(), original.getEndTime());
		Long classId = original.getClassRoom().getId();
		Long teacherId = original.getClassRoom().getTeacher().getId();
		List<MakeUpSlotOption> slots = new ArrayList<>();
		LocalTime candidateStart = businessStart(date);
		LocalTime businessEnd = businessEnd(date);
		while (!candidateStart.plus(duration).isAfter(businessEnd)) {
			LocalDateTime start = LocalDateTime.of(date, candidateStart);
			LocalDateTime end = start.plus(duration);
			MakeUpSlotStatus status = resolveRescheduleSlotStatus(classId, teacherId, start, end);
			MakeUpAvailableSlot slot = new MakeUpAvailableSlot(start, end, status);
			slots.add(new MakeUpSlotOption(
					start.toString(),
					end.toString(),
					slot.getTimeText(),
					status.getDisplayName(),
					status.getCssClass(),
					status == MakeUpSlotStatus.TEACHER_CONFLICT
							? scheduleConflictService.teacherConflictDetails(teacherId, start, end)
							: List.of()));
			candidateStart = candidateStart.plusMinutes(SLOT_STEP_MINUTES);
		}
		return slots;
	}

	@Transactional
	public void createPendingReschedule(Long originalScheduleId, LocalDate originalDate,
			String reason, Long currentTeacherId, boolean director) {
		ClassSchedule original = classScheduleRepository.findById(originalScheduleId)
				.orElseThrow(() -> new IllegalArgumentException("找不到原課程"));
		if (original.getClassRoom() == null || original.getClassRoom().getTeacher() == null) {
			throw new IllegalArgumentException("原課程缺少教師資料");
		}
		if (!director && !currentTeacherId.equals(original.getClassRoom().getTeacher().getId())) {
			throw new IllegalArgumentException("無權調整此課程");
		}
		if (originalDate == null) {
			throw new IllegalArgumentException("請選擇原上課日期");
		}
		if (!LocalDateTime.of(originalDate, original.getStartTime()).isAfter(LocalDateTime.now())) {
			throw new IllegalArgumentException("已開始或已結束的課程不可調課");
		}
		makeUpClassService.createPendingRescheduleRequest(original, originalDate,
				original.getClassRoom().getTeacher().getId(), reason);
	}

	public LocalDate weekStart(LocalDate date) {
		return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
	}

	private List<WeeklyScheduleDto> buildNormalOccurrences(LocalDate weekStart, Long currentTeacherId,
			boolean director, Long teacherFilterId, Long classFilterId, Set<String> cancelledOccurrences) {
		List<ClassRoom> classRooms = director ? classRoomRepository.findByActiveTrue()
				: classRoomRepository.findByTeacherIdAndActiveTrue(currentTeacherId);
		return classRooms.stream()
				.filter(classRoom -> teacherFilterId == null || classRoom.getTeacher() != null
						&& teacherFilterId.equals(classRoom.getTeacher().getId()))
				.filter(classRoom -> classFilterId == null || classFilterId.equals(classRoom.getId()))
				.flatMap(classRoom -> classRoom.getEffectiveSchedules().stream()
						.map(schedule -> toNormalDto(classRoom, schedule, weekStart)))
				.filter(row -> !cancelledOccurrences.contains(occurrenceKey(row.getScheduleId(), row.getCourseDate())))
				.toList();
	}

	private WeeklyScheduleDto toNormalDto(ClassRoom classRoom, ClassSchedule schedule, LocalDate weekStart) {
		LocalDate courseDate = weekStart.plusDays(dayOffset(schedule.getWeekday()));
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
				subjectKey(classRoom),
				teacherKey(classRoom),
				gradeKey(classRoom),
				schedule.isWeeklyExam());
	}

	private WeeklyScheduleDto toEventDto(ClassSchedule schedule) {
		ClassRoom classRoom = schedule.getClassRoom();
		return new WeeklyScheduleDto(
				schedule.getId(),
				schedule.getOriginalSchedule() == null ? null : schedule.getOriginalSchedule().getId(),
				classRoom == null ? null : classRoom.getId(),
				classRoom == null ? "" : classRoom.getSubjectName(),
				classRoom == null ? "未指定班級" : classRoom.getDisplayName(),
				classRoom == null ? "未指定教師" : classRoom.getTeacherName(),
				schedule.getCourseDate(),
				schedule.getScheduledStartAt(),
				schedule.getScheduledEndAt(),
				schedule.getScheduleType(),
				null,
				schedule.getRescheduleReason(),
				subjectKey(classRoom),
				teacherKey(classRoom),
				gradeKey(classRoom));
	}

	private WeeklyScheduleDto toBlockedOriginalDto(MakeUpClassRequest request) {
		ClassSchedule original = request.getOriginalCourseSchedule();
		ClassRoom classRoom = request.getClassRoom() != null ? request.getClassRoom() : original.getClassRoom();
		LocalDate courseDate = request.getOriginalCourseDate();
		return new WeeklyScheduleDto(
				original.getId(),
				original.getId(),
				classRoom == null ? null : classRoom.getId(),
				classRoom == null ? "" : classRoom.getSubjectName(),
				classRoom == null ? "未指定班級" : classRoom.getDisplayName(),
				request.getTeacher() == null ? (classRoom == null ? "未指定教師" : classRoom.getTeacherName())
						: request.getTeacher().getDisplayName(),
				courseDate,
				LocalDateTime.of(courseDate, original.getStartTime()),
				LocalDateTime.of(courseDate, original.getEndTime()),
				ScheduleType.CANCELLED,
				request.getNote(),
				request.getNote(),
				subjectKey(classRoom),
				request.getTeacher() == null ? teacherKey(classRoom) : String.valueOf(request.getTeacher().getId()),
				gradeKey(classRoom));
	}

	private String subjectKey(ClassRoom classRoom) {
		return classRoom == null || classRoom.getSubject() == null
				? "未指定"
				: String.valueOf(classRoom.getSubject().getId());
	}

	private String teacherKey(ClassRoom classRoom) {
		return classRoom == null || classRoom.getTeacher() == null
				? "未指定"
				: String.valueOf(classRoom.getTeacher().getId());
	}

	private String gradeKey(ClassRoom classRoom) {
		return classRoom == null || classRoom.getGrade() == null || classRoom.getGrade().isBlank()
				? "未指定"
				: classRoom.getGrade();
	}

	private ClassSchedule eventSchedule(ClassSchedule original, ScheduleType scheduleType,
			LocalDateTime start, LocalDateTime end, String reason, Long currentTeacherId) {
		ClassSchedule schedule = new ClassSchedule();
		schedule.setClassRoom(original.getClassRoom());
		schedule.setWeekday(weekdayName(start.getDayOfWeek()));
		schedule.setStartTime(start.toLocalTime());
		schedule.setEndTime(end.toLocalTime());
		schedule.setScheduleType(scheduleType);
		schedule.setOriginalSchedule(original);
		schedule.setCourseDate(start.toLocalDate());
		schedule.setScheduledStartAt(start);
		schedule.setScheduledEndAt(end);
		schedule.setRescheduleReason(reason);
		schedule.setCreatedByTeacherId(currentTeacherId);
		return schedule;
	}

	private String occurrenceKey(Long scheduleId, LocalDate date) {
		return scheduleId + "@" + date;
	}

	private MakeUpSlotStatus resolveRescheduleSlotStatus(Long classId, Long teacherId,
			LocalDateTime start, LocalDateTime end) {
		if (scheduleConflictService.hasStudentClassConflict(classId, start, end)
				|| scheduleConflictService.hasMakeUpConflict(classId, null, start, end)
				|| scheduleConflictService.hasRescheduleConflict(classId, null, start, end)) {
			return MakeUpSlotStatus.STUDENT_CONFLICT;
		}
		if (scheduleConflictService.hasTeacherConflict(teacherId, start, end)
				|| scheduleConflictService.hasMakeUpConflict(null, teacherId, start, end)
				|| scheduleConflictService.hasRescheduleConflict(null, teacherId, start, end)) {
			return MakeUpSlotStatus.TEACHER_CONFLICT;
		}
		return MakeUpSlotStatus.AVAILABLE;
	}

	private LocalTime businessStart(LocalDate date) {
		return isWeekend(date) ? WEEKEND_START : WEEKDAY_START;
	}

	private LocalTime businessEnd(LocalDate date) {
		return isWeekend(date) ? WEEKEND_END : WEEKDAY_END;
	}

	private boolean isWeekend(LocalDate date) {
		return date.getDayOfWeek() == DayOfWeek.SATURDAY
				|| date.getDayOfWeek() == DayOfWeek.SUNDAY;
	}

	private int dayOffset(String weekday) {
		return switch (weekday) {
			case "星期一" -> 0;
			case "星期二" -> 1;
			case "星期三" -> 2;
			case "星期四" -> 3;
			case "星期五" -> 4;
			case "星期六" -> 5;
			case "星期日" -> 6;
			default -> 0;
		};
	}

	private String weekdayName(DayOfWeek dayOfWeek) {
		return switch (dayOfWeek) {
			case MONDAY -> "星期一";
			case TUESDAY -> "星期二";
			case WEDNESDAY -> "星期三";
			case THURSDAY -> "星期四";
			case FRIDAY -> "星期五";
			case SATURDAY -> "星期六";
			case SUNDAY -> "星期日";
		};
	}
}
