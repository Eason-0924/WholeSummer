package com.example.cramschool.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.dto.AvailableLessonRequest;
import com.example.cramschool.dto.AvailableLessonResponse;
import com.example.cramschool.dto.AvailableLessonView;
import com.example.cramschool.dto.CreateLiffLeaveRequest;
import com.example.cramschool.dto.LiffLeaveRequestResponse;
import com.example.cramschool.dto.LiffMeResponse;
import com.example.cramschool.dto.LiffStudentResponse;
import com.example.cramschool.dto.LiffStudentView;
import com.example.cramschool.dto.VerifiedLineUser;
import com.example.cramschool.dto.WeeklyScheduleDto;
import com.example.cramschool.entity.ClassSchedule;
import com.example.cramschool.entity.ClassStudent;
import com.example.cramschool.entity.ParentLineBinding;
import com.example.cramschool.entity.ScheduleType;
import com.example.cramschool.entity.Student;
import com.example.cramschool.entity.StudentLeaveRequest;
import com.example.cramschool.entity.StudentLeaveStatus;
import com.example.cramschool.repository.ClassScheduleRepository;
import com.example.cramschool.repository.ClassStudentRepository;
import com.example.cramschool.repository.ParentLineBindingRepository;
import com.example.cramschool.repository.StudentLeaveRequestRepository;
import com.example.cramschool.repository.StudentRepository;

@Service
@Transactional
public class LineStudentLeaveService {

	private static final int DEFAULT_LOOKAHEAD_DAYS = 90;
	private static final List<StudentLeaveStatus> ACTIVE_LEAVE_STATUSES = List.of(
			StudentLeaveStatus.PENDING, StudentLeaveStatus.APPROVED);
	private static final Map<String, String> REASON_LABELS = Map.of(
			"SICK", "病假",
			"PERSONAL", "事假",
			"SCHOOL", "學校活動",
			"OTHER", "其他");

	private final LineLiffAuthService lineLiffAuthService;
	private final ParentLineBindingRepository parentLineBindingRepository;
	private final StudentRepository studentRepository;
	private final ClassStudentRepository classStudentRepository;
	private final ClassScheduleRepository classScheduleRepository;
	private final StudentLeaveRequestRepository studentLeaveRequestRepository;
	private final WeeklyScheduleService weeklyScheduleService;
	private final LineNotificationService lineNotificationService;
	private final WebPushEventNotificationService webPushEventNotificationService;

	public LineStudentLeaveService(LineLiffAuthService lineLiffAuthService,
			ParentLineBindingRepository parentLineBindingRepository,
			StudentRepository studentRepository,
			ClassStudentRepository classStudentRepository,
			ClassScheduleRepository classScheduleRepository,
			StudentLeaveRequestRepository studentLeaveRequestRepository,
			WeeklyScheduleService weeklyScheduleService,
			LineNotificationService lineNotificationService,
			WebPushEventNotificationService webPushEventNotificationService) {
		this.lineLiffAuthService = lineLiffAuthService;
		this.parentLineBindingRepository = parentLineBindingRepository;
		this.studentRepository = studentRepository;
		this.classStudentRepository = classStudentRepository;
		this.classScheduleRepository = classScheduleRepository;
		this.studentLeaveRequestRepository = studentLeaveRequestRepository;
		this.weeklyScheduleService = weeklyScheduleService;
		this.lineNotificationService = lineNotificationService;
		this.webPushEventNotificationService = webPushEventNotificationService;
	}

	@Transactional(readOnly = true)
	public LiffMeResponse currentUser(String idToken) {
		VerifiedLineUser user = lineLiffAuthService.verifyIdToken(idToken);
		List<ParentLineBinding> bindings = findBoundBindings(user.lineUserId());
		return new LiffMeResponse(!bindings.isEmpty(), user.lineUserId(), user.displayName(), bindings.size());
	}

	@Transactional(readOnly = true)
	public LiffStudentResponse findStudents(String idToken) {
		VerifiedLineUser user = lineLiffAuthService.verifyIdToken(idToken);
		List<LiffStudentView> students = findBoundBindings(user.lineUserId()).stream()
				.map(ParentLineBinding::getStudent)
				.filter(student -> student != null)
				.collect(Collectors.toMap(Student::getId, student -> student, (first, ignored) -> first,
						LinkedHashMap::new))
				.values()
				.stream()
				.map(student -> new LiffStudentView(
						student.getId(),
						student.getDisplayName(),
						student.getGrade(),
						student.isActive()))
				.toList();
		return new LiffStudentResponse(students);
	}

	@Transactional(readOnly = true)
	public AvailableLessonResponse findAvailableLessons(Long studentId, AvailableLessonRequest request) {
		VerifiedLineUser user = lineLiffAuthService.verifyIdToken(request.idToken());
		Student student = findBoundStudent(user.lineUserId(), studentId);
		LocalDate fromDate = request.fromDate() == null ? LocalDate.now() : request.fromDate();
		LocalDate toDate = request.toDate() == null ? fromDate.plusDays(DEFAULT_LOOKAHEAD_DAYS) : request.toDate();
		if (toDate.isBefore(fromDate)) {
			throw new IllegalArgumentException("查詢結束日期不可早於開始日期");
		}
		return new AvailableLessonResponse(availableLessons(student, fromDate, toDate));
	}

	public LiffLeaveRequestResponse createLeaveRequest(CreateLiffLeaveRequest request) {
		VerifiedLineUser user = lineLiffAuthService.verifyIdToken(request.idToken());
		Student student = findBoundStudent(user.lineUserId(), request.studentId());
		if (!student.isActive()) {
			throw new IllegalArgumentException("此學生目前未啟用，無法建立請假申請");
		}
		if (request.courseDate() == null) {
			throw new IllegalArgumentException("請選擇請假課程日期");
		}
		AvailableLessonView lesson = availableLessons(student, request.courseDate(), request.courseDate()).stream()
				.filter(candidate -> request.classRoomId() != null
						&& request.classRoomId().equals(candidate.classRoomId()))
				.filter(candidate -> request.scheduleId() != null
						&& request.scheduleId().equals(candidate.scheduleId()))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("找不到可請假的課程"));
		if (lesson.leaveStatus() != null) {
			throw new IllegalArgumentException("此課程已送出請假申請，請勿重複送出");
		}
		if (!lesson.courseDate().equals(request.courseDate())) {
			throw new IllegalArgumentException("請假課程日期不正確");
		}
		if (!LocalDateTime.of(lesson.courseDate(), lesson.startTime()).isAfter(LocalDateTime.now())) {
			throw new IllegalArgumentException("已開始或已結束的課程不可由 LIFF 請假");
		}
		if (studentLeaveRequestRepository.existsActiveRequest(student.getId(), lesson.courseDate(),
				lesson.classRoomId(), lesson.scheduleId(), ACTIVE_LEAVE_STATUSES)) {
			throw new IllegalArgumentException("此課程已送出請假申請，請勿重複送出");
		}

		ClassSchedule schedule = classScheduleRepository.findById(lesson.scheduleId())
				.orElseThrow(() -> new IllegalArgumentException("找不到請假課程"));
		ParentLineBinding binding = findBinding(user.lineUserId(), student.getId());
		StudentLeaveRequest leaveRequest = new StudentLeaveRequest();
		leaveRequest.setStudent(student);
		leaveRequest.setClassRoom(schedule.getClassRoom());
		leaveRequest.setClassSchedule(schedule);
		leaveRequest.setCourseDate(lesson.courseDate());
		leaveRequest.setScheduledStartAt(LocalDateTime.of(lesson.courseDate(), lesson.startTime()));
		leaveRequest.setScheduledEndAt(LocalDateTime.of(lesson.courseDate(), lesson.endTime()));
		leaveRequest.setReason(buildReason(request.reasonType(), request.note()));
		leaveRequest.setRequesterLineUserId(user.lineUserId());
		leaveRequest.setRequesterDisplayName(user.displayName());
		leaveRequest.setParentRelation(binding.getRelation());
		StudentLeaveRequest saved = studentLeaveRequestRepository.save(leaveRequest);
		lineNotificationService.sendStudentLeaveSubmittedNotification(saved);
		webPushEventNotificationService.notifyStudentLeaveSubmitted(
				student.getDisplayName(), schedule.getClassRoom().getDisplayName());
		return new LiffLeaveRequestResponse(true, saved.getId(), saved.getStatus().name(),
				"已收到請假申請，待補習班確認。");
	}

	private List<AvailableLessonView> availableLessons(Student student, LocalDate fromDate, LocalDate toDate) {
		Set<Long> activeClassIds = classStudentRepository.findByStudentIdAndActiveTrue(student.getId()).stream()
				.map(ClassStudent::getClassRoom)
				.filter(classRoom -> classRoom != null && classRoom.isActive())
				.map(classRoom -> classRoom.getId())
				.collect(Collectors.toSet());
		if (activeClassIds.isEmpty()) {
			return List.of();
		}
		Map<LessonKey, StudentLeaveStatus> leaveStatuses = studentLeaveRequestRepository
				.findByStudentIdAndCourseDateBetweenOrderByCourseDateAscScheduledStartAtAsc(
						student.getId(), fromDate, toDate)
				.stream()
				.filter(request -> request.getClassSchedule() != null)
				.filter(request -> ACTIVE_LEAVE_STATUSES.contains(request.getStatus()))
				.collect(Collectors.toMap(
						request -> new LessonKey(request.getClassSchedule().getId(), request.getCourseDate()),
						StudentLeaveRequest::getStatus,
						(first, ignored) -> first,
						LinkedHashMap::new));

		Map<LessonKey, AvailableLessonView> lessons = new LinkedHashMap<>();
		LocalDate weekCursor = weeklyScheduleService.weekStart(fromDate);
		LocalDate lastWeek = weeklyScheduleService.weekStart(toDate);
		while (!weekCursor.isAfter(lastWeek)) {
			for (WeeklyScheduleDto row : weeklyScheduleService.findWeeklySchedules(weekCursor, null, true, null, null)) {
				if (row.getCourseDate() == null || row.getCourseDate().isBefore(fromDate)
						|| row.getCourseDate().isAfter(toDate)) {
					continue;
				}
				if (row.getScheduleType() == ScheduleType.CANCELLED || row.getClassRoomId() == null
						|| row.getScheduleId() == null || row.getStartTime() == null || row.getEndTime() == null) {
					continue;
				}
				if (!activeClassIds.contains(row.getClassRoomId())) {
					continue;
				}
				LessonKey key = new LessonKey(row.getScheduleId(), row.getCourseDate());
				StudentLeaveStatus leaveStatus = leaveStatuses.get(key);
				lessons.putIfAbsent(key, new AvailableLessonView(
						row.getClassRoomId(),
						row.getScheduleId(),
						row.getClassName(),
						row.getCourseDate(),
						row.getStartTime().toLocalTime(),
						row.getEndTime().toLocalTime(),
						row.getTeacherName(),
						leaveStatus == null ? null : leaveStatus.name()));
			}
			weekCursor = weekCursor.plusWeeks(1);
		}
		return lessons.values()
				.stream()
				.sorted(Comparator.comparing(AvailableLessonView::courseDate)
						.thenComparing(AvailableLessonView::startTime)
						.thenComparing(AvailableLessonView::className))
				.toList();
	}

	private Student findBoundStudent(String lineUserId, Long studentId) {
		if (studentId == null) {
			throw new IllegalArgumentException("請選擇學生");
		}
		boolean bound = findBoundBindings(lineUserId).stream()
				.anyMatch(binding -> binding.getStudent() != null && studentId.equals(binding.getStudent().getId()));
		if (!bound) {
			throw new IllegalArgumentException("無法替非綁定學生請假");
		}
		return studentRepository.findById(studentId)
				.orElseThrow(() -> new IllegalArgumentException("找不到學生資料"));
	}

	private ParentLineBinding findBinding(String lineUserId, Long studentId) {
		return findBoundBindings(lineUserId).stream()
				.filter(binding -> binding.getStudent() != null && studentId.equals(binding.getStudent().getId()))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("尚未綁定家長帳號"));
	}

	private List<ParentLineBinding> findBoundBindings(String lineUserId) {
		return parentLineBindingRepository.findByLineUserIdAndStatusOrderByStudentChineseNameAsc(
				lineUserId, ParentLineBinding.STATUS_BOUND);
	}

	private String buildReason(String reasonType, String note) {
		String normalizedType = reasonType == null || reasonType.isBlank() ? "OTHER" : reasonType.trim().toUpperCase();
		String reasonLabel = REASON_LABELS.get(normalizedType);
		if (reasonLabel == null) {
			throw new IllegalArgumentException("請選擇有效的請假原因");
		}
		String normalizedNote = note == null ? "" : note.trim();
		String reason = normalizedNote.isBlank() ? reasonLabel : reasonLabel + "：" + normalizedNote;
		if (reason.length() > 500) {
			throw new IllegalArgumentException("請假原因與備註合計不可超過 500 個字");
		}
		return reason;
	}

	private record LessonKey(Long scheduleId, LocalDate courseDate) {
	}
}
