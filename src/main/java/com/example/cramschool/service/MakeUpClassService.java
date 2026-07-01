package com.example.cramschool.service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.dto.MakeUpAvailableSlot;
import com.example.cramschool.dto.MakeUpCalendarDate;
import com.example.cramschool.dto.MakeUpCalendarDay;
import com.example.cramschool.dto.MakeUpRequestView;
import com.example.cramschool.dto.MakeUpSlotStatus;
import com.example.cramschool.dto.MakeUpSlotOption;
import com.example.cramschool.entity.ClassRoom;
import com.example.cramschool.entity.ClassSchedule;
import com.example.cramschool.entity.MakeUpClassRequest;
import com.example.cramschool.entity.MakeUpSourceType;
import com.example.cramschool.entity.MakeUpStatus;
import com.example.cramschool.entity.ScheduleType;
import com.example.cramschool.entity.TeacherAttendance;
import com.example.cramschool.entity.TeacherLeave;
import com.example.cramschool.repository.ClassScheduleRepository;
import com.example.cramschool.repository.MakeUpClassRequestRepository;
import com.example.cramschool.service.ScheduleConflictService.ScheduleConflictContext;

@Service
@Transactional
public class MakeUpClassService {

	private static final int SEARCH_DAYS = 30;
	private static final int SLOT_STEP_MINUTES = 30;
	private static final LocalTime WEEKDAY_START = LocalTime.of(14, 0);
	private static final LocalTime WEEKDAY_END = LocalTime.of(22, 0);
	private static final LocalTime WEEKEND_START = LocalTime.of(9, 0);
	private static final LocalTime WEEKEND_END = LocalTime.of(22, 0);

	private final MakeUpClassRequestRepository makeUpClassRequestRepository;
	private final ScheduleConflictService scheduleConflictService;
	private final ClassScheduleRepository classScheduleRepository;
	private final Map<CalendarCacheKey, List<MakeUpCalendarDay>> calendarCache = new ConcurrentHashMap<>();
	private final Map<DailySlotCacheKey, List<MakeUpSlotOption>> dailySlotCache = new ConcurrentHashMap<>();

	public MakeUpClassService(MakeUpClassRequestRepository makeUpClassRequestRepository,
			ScheduleConflictService scheduleConflictService,
			ClassScheduleRepository classScheduleRepository) {
		this.makeUpClassRequestRepository = makeUpClassRequestRepository;
		this.scheduleConflictService = scheduleConflictService;
		this.classScheduleRepository = classScheduleRepository;
	}

	public MakeUpClassRequest createRequiredMakeUpFromLeave(TeacherLeave leave) {
		return createRequiredMakeUp(
				leave.getCourseSchedule(),
				leave.getLeaveDate(),
				leave.getTeacher().getId(),
				MakeUpSourceType.LEAVE,
				leave.getId(),
				leave.getReason());
	}

	public MakeUpClassRequest createRequiredMakeUpFromAbsence(TeacherAttendance attendance) {
		if (attendance.getMatchedCourseId() == null) {
			return null;
		}
		ClassSchedule schedule = classScheduleRepository.findById(attendance.getMatchedCourseId())
				.orElse(null);
		return createRequiredMakeUp(
				schedule,
				attendance.getDate(),
				attendance.getTeacher().getId(),
				MakeUpSourceType.ABSENCE,
				attendance.getId(),
				attendance.getNote());
	}

	public MakeUpClassRequest createPendingRescheduleRequest(ClassSchedule schedule, LocalDate originalDate,
			Long currentTeacherId, String reason) {
		return createRequiredMakeUp(
				schedule,
				originalDate,
				currentTeacherId,
				MakeUpSourceType.RESCHEDULE,
				originalDate == null ? null : originalDate.toEpochDay(),
				reason);
	}

	public long countPendingForHome(Long teacherId, boolean director) {
		return director
				? makeUpClassRequestRepository.countByStatus(MakeUpStatus.PENDING)
				: makeUpClassRequestRepository.countByTeacherIdAndStatus(teacherId, MakeUpStatus.PENDING);
	}

	@Transactional(readOnly = true)
	public List<MakeUpClassRequest> findPendingForHome(Long teacherId, boolean director) {
		return director
				? makeUpClassRequestRepository.findByStatusOrderByOriginalCourseDateAscIdAsc(MakeUpStatus.PENDING)
				: makeUpClassRequestRepository.findByTeacherIdAndStatusOrderByOriginalCourseDateAscIdAsc(
						teacherId, MakeUpStatus.PENDING);
	}

	@Async
	public void warmUpPendingCalendarCache(List<MakeUpClassRequest> requests) {
		if (requests == null || requests.isEmpty()) {
			return;
		}
		LocalDate baseDate = LocalDate.now();
		List<LocalDate> dates = buildCalendarDateRange(baseDate);
		for (MakeUpClassRequest request : requests) {
			if (request == null || request.getId() == null
					|| request.getStatus() != MakeUpStatus.PENDING
							&& request.getStatus() != MakeUpStatus.SCHEDULED) {
				continue;
			}
			for (LocalDate date : dates) {
				DailySlotCacheKey cacheKey = new DailySlotCacheKey(request.getId(), date);
				dailySlotCache.computeIfAbsent(cacheKey, ignored -> calculateDailySlotOptions(request, date));
			}
		}
	}

	@Transactional(readOnly = true)
	public List<MakeUpClassRequest> findPendingRequests(Long teacherId, boolean director) {
		return director
				? makeUpClassRequestRepository.findByStatusOrderByOriginalCourseDateAscIdAsc(MakeUpStatus.PENDING)
				: makeUpClassRequestRepository.findByTeacherIdAndStatusOrderByOriginalCourseDateAscIdAsc(
						teacherId, MakeUpStatus.PENDING);
	}

	@Transactional(readOnly = true)
	public List<MakeUpClassRequest> findScheduledRequests(Long teacherId, boolean director) {
		return director
				? makeUpClassRequestRepository.findByStatusOrderBySelectedMakeUpStartAscIdAsc(MakeUpStatus.SCHEDULED)
				: makeUpClassRequestRepository.findByTeacherIdAndStatusOrderBySelectedMakeUpStartAscIdAsc(
						teacherId, MakeUpStatus.SCHEDULED);
	}

	@Transactional(readOnly = true)
	public MakeUpRequestView findPendingView(Long requestId, Long teacherId, boolean director) {
		MakeUpClassRequest request = findAllowedRequest(requestId, teacherId, director);
		if (request.getStatus() != MakeUpStatus.PENDING) {
			throw new IllegalArgumentException("此補課需求目前不需安排");
		}
		return new MakeUpRequestView(request, buildCalendarDates(request, LocalDate.now()));
	}

	@Transactional(readOnly = true)
	public MakeUpRequestView findScheduledView(Long requestId, Long teacherId, boolean director) {
		MakeUpClassRequest request = findAllowedRequest(requestId, teacherId, director);
		if (request.getStatus() != MakeUpStatus.SCHEDULED) {
			throw new IllegalArgumentException("此補課紀錄目前不可編輯");
		}
		return new MakeUpRequestView(request, buildCalendarDates(request, LocalDate.now()));
	}

	@Transactional(readOnly = true)
	public List<MakeUpRequestView> findPendingViews(Long teacherId, boolean director) {
		return findPendingRequests(teacherId, director).stream()
				.map(request -> new MakeUpRequestView(request, buildCalendarDates(request, LocalDate.now())))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<MakeUpCalendarDate> findCalendarDates(Long requestId, Long teacherId, boolean director) {
		MakeUpClassRequest request = findAllowedSchedulableRequest(requestId, teacherId, director);
		return buildCalendarDates(request, LocalDate.now());
	}

	@Transactional(readOnly = true)
	public List<MakeUpSlotOption> findSlotOptions(Long requestId, LocalDate date,
			Long teacherId, boolean director) {
		MakeUpClassRequest request = findAllowedSchedulableRequest(requestId, teacherId, director);
		LocalDate targetDate = date == null ? LocalDate.now().plusDays(1) : date;
		DailySlotCacheKey cacheKey = new DailySlotCacheKey(requestId, targetDate);
		return dailySlotCache.computeIfAbsent(cacheKey, ignored -> calculateDailySlotOptions(request, targetDate));
	}

	@Transactional
	public void scheduleMakeUpClass(Long makeUpRequestId, LocalDateTime start,
			LocalDateTime end, Long currentTeacherId, boolean director, boolean allowTeacherConflict) {
		MakeUpClassRequest request = findAllowedPendingRequest(makeUpRequestId, currentTeacherId, director);
		scheduleMakeUpClass(request, start, end, currentTeacherId, allowTeacherConflict);
	}

	@Transactional
	public void rescheduleMakeUpClass(Long makeUpRequestId, LocalDateTime start,
			LocalDateTime end, Long currentTeacherId, boolean director, boolean allowTeacherConflict) {
		MakeUpClassRequest request = findAllowedScheduledRequest(makeUpRequestId, currentTeacherId, director);
		deleteSelectedMakeUpSchedule(request);
		classScheduleRepository.flush();
		scheduleMakeUpClass(request, start, end, currentTeacherId, allowTeacherConflict);
	}

	@Transactional
	public void reopenScheduledMakeUp(Long makeUpRequestId, Long currentTeacherId, boolean director) {
		MakeUpClassRequest request = findAllowedScheduledRequest(makeUpRequestId, currentTeacherId, director);
		deleteSelectedMakeUpSchedule(request);
		request.setStatus(MakeUpStatus.PENDING);
		clearSelectedMakeUp(request);
		makeUpClassRequestRepository.save(request);
		clearMakeUpCaches(makeUpRequestId);
	}

	@Transactional
	public void ignoreScheduledMakeUp(Long makeUpRequestId, Long currentTeacherId, boolean director) {
		MakeUpClassRequest request = findAllowedScheduledRequest(makeUpRequestId, currentTeacherId, director);
		deleteSelectedMakeUpSchedule(request);
		request.setStatus(MakeUpStatus.CANCELLED);
		clearSelectedMakeUp(request);
		makeUpClassRequestRepository.save(request);
		clearMakeUpCaches(makeUpRequestId);
	}

	private void scheduleMakeUpClass(MakeUpClassRequest request, LocalDateTime start,
			LocalDateTime end, Long currentTeacherId, boolean allowTeacherConflict) {
		ClassSchedule originalSchedule = request.getOriginalCourseSchedule();
		if (originalSchedule == null || request.getClassRoom() == null || request.getTeacher() == null) {
			throw new IllegalArgumentException("補課需求缺少原課程資料");
		}
		if (start == null || end == null || !end.isAfter(start)) {
			throw new IllegalArgumentException("補課時間不正確");
		}
		validateMakeUpDuration(originalSchedule, start, end);
		Long classId = request.getClassRoom().getId();
		Long teacherId = request.getTeacher().getId();
		boolean studentConflict = scheduleConflictService.hasStudentClassConflict(classId, start, end)
				|| scheduleConflictService.hasMakeUpConflict(classId, null, start, end)
				|| scheduleConflictService.hasRescheduleConflict(classId, null, start, end);
		boolean teacherConflict = scheduleConflictService.hasTeacherConflict(teacherId, start, end)
				|| scheduleConflictService.hasMakeUpConflict(null, teacherId, start, end)
				|| scheduleConflictService.hasRescheduleConflict(null, teacherId, start, end);
		if (studentConflict || (teacherConflict && !allowTeacherConflict)) {
			throw new IllegalArgumentException("此補課時段已有衝突，請重新選擇");
		}

		ClassSchedule makeUpSchedule = new ClassSchedule();
		makeUpSchedule.setClassRoom(request.getClassRoom());
		makeUpSchedule.setWeekday(weekdayName(start.toLocalDate().getDayOfWeek()));
		makeUpSchedule.setStartTime(start.toLocalTime());
		makeUpSchedule.setEndTime(end.toLocalTime());
		makeUpSchedule.setScheduleType(ScheduleType.MAKE_UP);
		makeUpSchedule.setOriginalSchedule(originalSchedule);
		makeUpSchedule.setCourseDate(start.toLocalDate());
		makeUpSchedule.setScheduledStartAt(start);
		makeUpSchedule.setScheduledEndAt(end);
		makeUpSchedule.setCreatedByTeacherId(currentTeacherId);
		classScheduleRepository.save(makeUpSchedule);

		request.setStatus(MakeUpStatus.SCHEDULED);
		request.setSelectedMakeUpStart(start);
		request.setSelectedMakeUpEnd(end);
		request.setSelectedByTeacherId(currentTeacherId);
		request.setSelectedAt(LocalDateTime.now());
		makeUpClassRequestRepository.save(request);
		clearMakeUpCaches(request.getId());
	}

	private void validateMakeUpDuration(ClassSchedule originalSchedule, LocalDateTime start, LocalDateTime end) {
		Duration originalDuration = Duration.between(originalSchedule.getStartTime(), originalSchedule.getEndTime());
		Duration selectedDuration = Duration.between(start, end);
		if (!selectedDuration.equals(originalDuration)) {
			throw new IllegalArgumentException("補課時長需與原課程相同");
		}
	}

	private MakeUpClassRequest findAllowedPendingRequest(Long requestId, Long teacherId, boolean director) {
		MakeUpClassRequest request = findAllowedRequest(requestId, teacherId, director);
		if (request.getStatus() != MakeUpStatus.PENDING) {
			throw new IllegalArgumentException("此補課需求目前不需安排");
		}
		return request;
	}

	private MakeUpClassRequest findAllowedScheduledRequest(Long requestId, Long teacherId, boolean director) {
		MakeUpClassRequest request = findAllowedRequest(requestId, teacherId, director);
		if (request.getStatus() != MakeUpStatus.SCHEDULED) {
			throw new IllegalArgumentException("此補課紀錄目前不可編輯");
		}
		return request;
	}

	private MakeUpClassRequest findAllowedSchedulableRequest(Long requestId, Long teacherId, boolean director) {
		MakeUpClassRequest request = findAllowedRequest(requestId, teacherId, director);
		if (request.getStatus() != MakeUpStatus.PENDING && request.getStatus() != MakeUpStatus.SCHEDULED) {
			throw new IllegalArgumentException("此補課需求目前不需安排");
		}
		return request;
	}

	private MakeUpClassRequest findAllowedRequest(Long requestId, Long teacherId, boolean director) {
		MakeUpClassRequest request = makeUpClassRequestRepository.findById(requestId)
				.orElseThrow(() -> new IllegalArgumentException("找不到補課需求"));
		if (!director && (request.getTeacher() == null || !teacherId.equals(request.getTeacher().getId()))) {
			throw new IllegalArgumentException("無權查看此補課需求");
		}
		return request;
	}

	private void deleteSelectedMakeUpSchedule(MakeUpClassRequest request) {
		if (request.getOriginalCourseSchedule() == null
				|| request.getSelectedMakeUpStart() == null
				|| request.getSelectedMakeUpEnd() == null) {
			return;
		}
		classScheduleRepository
				.findFirstByOriginalScheduleIdAndScheduleTypeAndScheduledStartAtAndScheduledEndAt(
						request.getOriginalCourseSchedule().getId(),
						ScheduleType.MAKE_UP,
						request.getSelectedMakeUpStart(),
						request.getSelectedMakeUpEnd())
				.ifPresent(classScheduleRepository::delete);
	}

	private void clearSelectedMakeUp(MakeUpClassRequest request) {
		request.setSelectedMakeUpStart(null);
		request.setSelectedMakeUpEnd(null);
		request.setSelectedByTeacherId(null);
		request.setSelectedAt(null);
	}

	private List<MakeUpCalendarDate> buildCalendarDates(MakeUpClassRequest request, LocalDate baseDate) {
		return buildCalendarDateRange(baseDate).stream()
				.map(date -> buildCalendarDate(request, date))
				.toList();
	}

	private List<LocalDate> buildCalendarDateRange(LocalDate baseDate) {
		List<LocalDate> dates = new ArrayList<>();
		LocalDate startDate = baseDate.plusDays(1);
		for (int dayOffset = 0; dayOffset < SEARCH_DAYS; dayOffset++) {
			dates.add(startDate.plusDays(dayOffset));
		}
		return dates;
	}

	private MakeUpCalendarDate buildCalendarDate(MakeUpClassRequest request, LocalDate date) {
		if (request == null || request.getId() == null) {
			return new MakeUpCalendarDate(date, "secondary", "尚未計算", 0, 0, 0, false);
		}
		List<MakeUpSlotOption> slots = dailySlotCache.get(new DailySlotCacheKey(request.getId(), date));
		if (slots == null) {
			return new MakeUpCalendarDate(date, "secondary", "背景計算中", 0, 0, 0, false);
		}
		long availableCount = countSlotsByStatus(slots, MakeUpSlotStatus.AVAILABLE);
		long teacherConflictCount = countSlotsByStatus(slots, MakeUpSlotStatus.TEACHER_CONFLICT);
		long studentConflictCount = countSlotsByStatus(slots, MakeUpSlotStatus.STUDENT_CONFLICT);
		if (availableCount > 0) {
			return new MakeUpCalendarDate(date, "success", MakeUpSlotStatus.AVAILABLE.getDisplayName(),
					availableCount, teacherConflictCount, studentConflictCount, true);
		}
		if (teacherConflictCount > 0) {
			return new MakeUpCalendarDate(date, "warning", MakeUpSlotStatus.TEACHER_CONFLICT.getDisplayName(),
					availableCount, teacherConflictCount, studentConflictCount, true);
		}
		return new MakeUpCalendarDate(date, "danger", MakeUpSlotStatus.STUDENT_CONFLICT.getDisplayName(),
				availableCount, teacherConflictCount, studentConflictCount, true);
	}

	private long countSlotsByStatus(List<MakeUpSlotOption> slots, MakeUpSlotStatus status) {
		return slots.stream()
				.filter(slot -> status.getCssClass().equals(slot.statusClass()))
				.count();
	}

	@Transactional(readOnly = true)
	public List<MakeUpCalendarDay> buildCalendarDays(MakeUpClassRequest request) {
		return buildCalendarDays(request, LocalDate.now());
	}

	private List<MakeUpCalendarDay> buildCalendarDays(MakeUpClassRequest request, LocalDate baseDate) {
		if (request == null || request.getId() == null) {
			return List.of();
		}
		CalendarCacheKey cacheKey = new CalendarCacheKey(request.getId(), baseDate);
		return calendarCache.computeIfAbsent(cacheKey, ignored -> calculateCalendarDays(request, baseDate));
	}

	private List<MakeUpCalendarDay> calculateCalendarDays(MakeUpClassRequest request, LocalDate baseDate) {
		ClassSchedule originalSchedule = request.getOriginalCourseSchedule();
		if (originalSchedule == null
				|| originalSchedule.getStartTime() == null
				|| originalSchedule.getEndTime() == null
				|| !originalSchedule.getEndTime().isAfter(originalSchedule.getStartTime())) {
			return List.of();
		}
		Duration duration = Duration.between(originalSchedule.getStartTime(), originalSchedule.getEndTime());
		Long classId = request.getClassRoom() == null ? null : request.getClassRoom().getId();
		Long teacherId = request.getTeacher() == null ? null : request.getTeacher().getId();
		ScheduleConflictContext conflictContext = scheduleConflictService.buildContext(classId, teacherId);
		Long excludedMakeUpScheduleId = selectedMakeUpScheduleId(request);
		List<MakeUpCalendarDay> days = new ArrayList<>();
		LocalDate startDate = baseDate.plusDays(1);
		for (int dayOffset = 0; dayOffset < SEARCH_DAYS; dayOffset++) {
			LocalDate date = startDate.plusDays(dayOffset);
			List<MakeUpAvailableSlot> slots = new ArrayList<>();
			LocalTime candidateStart = businessStart(date);
			LocalTime businessEnd = businessEnd(date);
			while (!candidateStart.plus(duration).isAfter(businessEnd)) {
				LocalDateTime start = LocalDateTime.of(date, candidateStart);
				LocalDateTime end = start.plus(duration);
				slots.add(new MakeUpAvailableSlot(start, end,
						resolveSlotStatus(
								conflictContext,
								request.getId(),
								excludedMakeUpScheduleId,
								classId,
								teacherId,
								start,
								end)));
				candidateStart = candidateStart.plusMinutes(SLOT_STEP_MINUTES);
			}
			days.add(new MakeUpCalendarDay(date, slots));
		}
		return days;
	}

	private List<MakeUpSlotOption> calculateDailySlotOptions(MakeUpClassRequest request, LocalDate date) {
		ClassSchedule originalSchedule = request.getOriginalCourseSchedule();
		if (originalSchedule == null
				|| originalSchedule.getStartTime() == null
				|| originalSchedule.getEndTime() == null
				|| !originalSchedule.getEndTime().isAfter(originalSchedule.getStartTime())) {
			return List.of();
		}
		Duration duration = Duration.between(originalSchedule.getStartTime(), originalSchedule.getEndTime());
		Long classId = request.getClassRoom() == null ? null : request.getClassRoom().getId();
		Long teacherId = request.getTeacher() == null ? null : request.getTeacher().getId();
		ScheduleConflictContext conflictContext = scheduleConflictService.buildContext(classId, teacherId);
		Long excludedMakeUpScheduleId = selectedMakeUpScheduleId(request);
		List<MakeUpSlotOption> slots = new ArrayList<>();
		LocalTime candidateStart = businessStart(date);
		LocalTime businessEnd = businessEnd(date);
		while (!candidateStart.plus(duration).isAfter(businessEnd)) {
			LocalDateTime start = LocalDateTime.of(date, candidateStart);
			LocalDateTime end = start.plus(duration);
			MakeUpSlotStatus status = resolveSlotStatus(
					conflictContext,
					request.getId(),
					excludedMakeUpScheduleId,
					classId,
					teacherId,
					start,
					end);
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

	private MakeUpSlotStatus resolveSlotStatus(ScheduleConflictContext conflictContext,
			Long excludedRequestId, Long excludedMakeUpScheduleId, Long classId, Long teacherId,
			LocalDateTime start, LocalDateTime end) {
		if (scheduleConflictService.hasStudentClassConflict(conflictContext, start, end)
				|| scheduleConflictService.hasMakeUpConflict(
						conflictContext, classId, null, start, end, excludedRequestId)
				|| scheduleConflictService.hasRescheduleConflict(
						classId, null, start, end, excludedMakeUpScheduleId)) {
			return MakeUpSlotStatus.STUDENT_CONFLICT;
		}
		if (scheduleConflictService.hasTeacherConflict(conflictContext, start, end)
				|| scheduleConflictService.hasMakeUpConflict(
						conflictContext, null, teacherId, start, end, excludedRequestId)
				|| scheduleConflictService.hasRescheduleConflict(
						null, teacherId, start, end, excludedMakeUpScheduleId)) {
			return MakeUpSlotStatus.TEACHER_CONFLICT;
		}
		return MakeUpSlotStatus.AVAILABLE;
	}

	private Long selectedMakeUpScheduleId(MakeUpClassRequest request) {
		if (request == null || request.getOriginalCourseSchedule() == null
				|| request.getSelectedMakeUpStart() == null
				|| request.getSelectedMakeUpEnd() == null) {
			return null;
		}
		return classScheduleRepository
				.findFirstByOriginalScheduleIdAndScheduleTypeAndScheduledStartAtAndScheduledEndAt(
						request.getOriginalCourseSchedule().getId(),
						ScheduleType.MAKE_UP,
						request.getSelectedMakeUpStart(),
						request.getSelectedMakeUpEnd())
				.map(ClassSchedule::getId)
				.orElse(null);
	}

	private MakeUpClassRequest createRequiredMakeUp(ClassSchedule schedule, LocalDate originalDate,
			Long teacherId, MakeUpSourceType sourceType, Long sourceRecordId, String note) {
		if (schedule == null || schedule.getId() == null || originalDate == null || teacherId == null) {
			return null;
		}
		if (makeUpClassRequestRepository.existsByOriginalCourseScheduleIdAndTeacherIdAndSourceTypeAndSourceRecordId(
				schedule.getId(), teacherId, sourceType, sourceRecordId)) {
			return null;
		}
		MakeUpClassRequest request = new MakeUpClassRequest();
		request.setOriginalCourseSchedule(schedule);
		request.setOriginalCourseDate(originalDate);
		request.setTeacher(schedule.getClassRoom() == null ? null : schedule.getClassRoom().getTeacher());
		if (request.getTeacher() == null || !teacherId.equals(request.getTeacher().getId())) {
			return null;
		}
		ClassRoom classRoom = schedule.getClassRoom();
		request.setClassRoom(classRoom);
		request.setSourceType(sourceType);
		request.setSourceRecordId(sourceRecordId);
		request.setStatus(MakeUpStatus.PENDING);
		request.setNote(note == null || note.isBlank() ? null : note.trim());
		return makeUpClassRequestRepository.save(request);
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

	private void clearMakeUpCaches(Long requestId) {
		calendarCache.keySet().removeIf(key -> key.requestId().equals(requestId));
		dailySlotCache.keySet().removeIf(key -> key.requestId().equals(requestId));
	}

	private record CalendarCacheKey(Long requestId, LocalDate baseDate) {
	}

	private record DailySlotCacheKey(Long requestId, LocalDate date) {
	}
}
