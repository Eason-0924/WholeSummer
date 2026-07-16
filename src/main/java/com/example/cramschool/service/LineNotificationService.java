package com.example.cramschool.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.config.LineProperties;
import com.example.cramschool.entity.LineNotificationLog;
import com.example.cramschool.entity.ParentLineBinding;
import com.example.cramschool.entity.Student;
import com.example.cramschool.entity.StudentAttendance;
import com.example.cramschool.entity.StudentLeaveRequest;
import com.example.cramschool.entity.TeacherPermissionType;
import com.example.cramschool.repository.LineNotificationLogRepository;
import com.example.cramschool.repository.ParentLineBindingRepository;
import com.example.cramschool.repository.StudentRepository;

@Service
@Transactional
public class LineNotificationService {

	private static final DateTimeFormatter DISPLAY_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

	private final StudentRepository studentRepository;
	private final ParentLineBindingRepository parentLineBindingRepository;
	private final LineNotificationLogRepository lineNotificationLogRepository;
	private final TeacherPermissionService teacherPermissionService;
	private final LineMessageService lineMessageService;
	private final LineProperties lineProperties;

	public LineNotificationService(StudentRepository studentRepository,
			ParentLineBindingRepository parentLineBindingRepository,
			LineNotificationLogRepository lineNotificationLogRepository,
			TeacherPermissionService teacherPermissionService, LineMessageService lineMessageService,
			LineProperties lineProperties) {
		this.studentRepository = studentRepository;
		this.parentLineBindingRepository = parentLineBindingRepository;
		this.lineNotificationLogRepository = lineNotificationLogRepository;
		this.teacherPermissionService = teacherPermissionService;
		this.lineMessageService = lineMessageService;
		this.lineProperties = lineProperties;
	}

	public boolean isLineEnabled() {
		return lineProperties != null && lineProperties.isEnabled();
	}

	@Transactional(readOnly = true)
	public List<ParentLineBinding> findBoundParents(Long studentId) {
		Student student = findStudent(studentId);
		return parentLineBindingRepository.findByStudentAndStatus(student, ParentLineBinding.STATUS_BOUND);
	}

	@Transactional(readOnly = true)
	public List<LineNotificationLog> findRecentLogs(Long studentId) {
		return lineNotificationLogRepository.findTop10ByStudentOrderByCreatedAtDesc(findStudent(studentId));
	}

	public int sendTestNotification(Long studentId, Long currentTeacherId) {
		teacherPermissionService.requirePermission(currentTeacherId, TeacherPermissionType.STUDENT_UPDATE,
				"權限不足，無法發送 LINE 測試通知");
		Student student = findStudent(studentId);
		List<ParentLineBinding> bindings = parentLineBindingRepository
				.findByStudentAndStatus(student, ParentLineBinding.STATUS_BOUND);
		String content = buildTestMessage(student);
		if (bindings.isEmpty()) {
			saveLog(student, null, LineNotificationLog.STATUS_FAILED, "LINE 測試通知",
					content, "尚無已綁定家長", null);
			throw new IllegalArgumentException("此學生尚無已綁定的 LINE 家長");
		}

		int successCount = 0;
		for (ParentLineBinding binding : bindings) {
			var result = lineMessageService.pushText(binding.getLineUserId(), content);
			saveLog(student, binding.getLineUserId(),
					result.success() ? LineNotificationLog.STATUS_SENT : LineNotificationLog.STATUS_FAILED,
					"LINE 測試通知", content, result.errorMessage(), result.providerMessageId());
			if (result.success()) {
				successCount++;
			}
		}
		return successCount;
	}

	@Transactional(readOnly = true)
	public String describeTestNotification(Long studentId) {
		Student student = findStudent(studentId);
		String recipients = parentLineBindingRepository.findByStudentAndStatus(student, ParentLineBinding.STATUS_BOUND)
				.stream().map(binding -> studentNameSuffix(student.getChineseName())
						+ (binding.getRelation() == null || binding.getRelation().isBlank() ? "家長" : binding.getRelation()))
				.collect(java.util.stream.Collectors.joining("、"));
		return "發送給 " + (recipients.isBlank() ? "無符合家長" : recipients) + "【LINE 測試通知】";
	}

	public int refreshParentDisplayNames(Long studentId, Long currentTeacherId) {
		teacherPermissionService.requirePermission(currentTeacherId, TeacherPermissionType.STUDENT_UPDATE,
				"權限不足，無法更新 LINE 家長名稱");
		Student student = findStudent(studentId);
		List<ParentLineBinding> bindings = parentLineBindingRepository
				.findByStudentAndStatus(student, ParentLineBinding.STATUS_BOUND);
		int updatedCount = 0;
		for (ParentLineBinding binding : bindings) {
			var displayName = lineMessageService.getProfileDisplayName(binding.getLineUserId());
			if (displayName.isPresent()) {
				binding.setLineDisplayName(displayName.get());
				parentLineBindingRepository.save(binding);
				updatedCount++;
			}
		}
		return updatedCount;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void sendStudentLeaveSubmittedNotification(StudentLeaveRequest leaveRequest) {
		sendStudentLeaveNotification(leaveRequest,
				"STUDENT_LEAVE_SUBMITTED",
				"LINE 請假申請確認",
				buildStudentLeaveSubmittedMessage(leaveRequest));
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void sendStudentLeaveApprovedNotification(StudentLeaveRequest leaveRequest) {
		sendStudentLeaveNotification(leaveRequest,
				"STUDENT_LEAVE_APPROVED",
				"LINE 請假審核通知",
				buildStudentLeaveApprovedMessage(leaveRequest));
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void sendCardCheckInNotification(StudentAttendance attendance) {
		if (attendance == null || attendance.getId() == null || attendance.getStudent() == null) {
			return;
		}
		sendAttendanceNotification(attendance,
				"ATTENDANCE_CHECK_IN",
				"LINE 到班通知",
				"到班",
				attendance.getCheckInTime());
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void sendLateArrivalReminder(Student student, Long referenceId,
			String className, LocalDateTime classStartTime) {
		sendLateArrivalReminders(List.of(new LateArrivalReminder(student, referenceId, className, classStartTime)));
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void sendLateArrivalReminders(List<LateArrivalReminder> reminders) {
		if (reminders == null || reminders.isEmpty()) {
			return;
		}
		Map<String, List<LateArrivalReminder>> remindersByParentAndTime = new LinkedHashMap<>();
		for (LateArrivalReminder reminder : reminders) {
			if (!isUnsentLateArrivalReminder(reminder)) {
				continue;
			}
			List<ParentLineBinding> bindings = parentLineBindingRepository.findByStudentAndStatus(
					reminder.student(), ParentLineBinding.STATUS_BOUND);
			if (bindings.isEmpty()) {
				saveStudentNotificationLog(reminder.student(), "ATTENDANCE_LATE_REMINDER", "CLASS_SCHEDULE_OCCURRENCE",
						reminder.referenceId(), "LINE 遲到提醒",
						buildLateArrivalReminderMessage(reminder.student(), null, reminder.className(), reminder.classStartTime()),
						LineNotificationLog.STATUS_SKIPPED, "尚無已綁定家長", null);
			}
			for (ParentLineBinding binding : bindings) {
				String key = binding.getLineUserId() + "|" + reminder.classStartTime();
				remindersByParentAndTime.computeIfAbsent(key, ignored -> new ArrayList<>())
						.add(reminder.withBinding(binding));
			}
		}
		for (List<LateArrivalReminder> parentReminders : remindersByParentAndTime.values()) {
			sendGroupedLateArrivalReminder(parentReminders);
		}
	}

	private boolean isUnsentLateArrivalReminder(LateArrivalReminder reminder) {
		if (reminder == null || reminder.student() == null || reminder.student().getId() == null
				|| reminder.referenceId() == null) {
			return false;
		}
		String notificationType = "ATTENDANCE_LATE_REMINDER";
		String referenceType = "CLASS_SCHEDULE_OCCURRENCE";
		if (lineNotificationLogRepository.existsByStudentAndNotificationTypeAndReferenceTypeAndReferenceId(
				reminder.student(), notificationType, referenceType, reminder.referenceId())) {
			return false;
		}
		return true;
	}

	private void sendGroupedLateArrivalReminder(List<LateArrivalReminder> reminders) {
		if (reminders == null || reminders.isEmpty()) {
			return;
		}
		String title = "LINE 遲到提醒";
		ParentLineBinding binding = reminders.get(0).binding();
		String content = buildGroupedLateArrivalReminderMessage(reminders, binding);
		var result = lineMessageService.pushText(binding.getLineUserId(), content);
		for (LateArrivalReminder reminder : reminders) {
			saveStudentNotificationLog(reminder.student(), "ATTENDANCE_LATE_REMINDER", "CLASS_SCHEDULE_OCCURRENCE",
					reminder.referenceId(), title, content,
					result.success() ? LineNotificationLog.STATUS_SENT : LineNotificationLog.STATUS_FAILED,
					result.errorMessage(), result.providerMessageId());
		}
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void sendCardCheckOutNotification(StudentAttendance attendance) {
		if (attendance == null || attendance.getId() == null || attendance.getStudent() == null) {
			return;
		}
		sendAttendanceNotification(attendance, "ATTENDANCE_CHECK_OUT", "LINE 簽退通知",
				"簽退", attendance.getCheckOutTime());
	}

	private Student findStudent(Long studentId) {
		return studentRepository.findById(studentId)
				.orElseThrow(() -> new IllegalArgumentException("找不到學生資料"));
	}

	private void sendStudentLeaveNotification(StudentLeaveRequest leaveRequest, String notificationType,
			String title, String content) {
		if (leaveRequest == null || leaveRequest.getId() == null || leaveRequest.getStudent() == null) {
			return;
		}
		Student student = leaveRequest.getStudent();
		if (lineNotificationLogRepository.existsByStudentAndNotificationTypeAndReferenceTypeAndReferenceId(
				student, notificationType, "STUDENT_LEAVE_REQUEST", leaveRequest.getId())) {
			return;
		}
		String lineUserId = leaveRequest.getRequesterLineUserId();
		if (lineUserId == null || lineUserId.isBlank()) {
			saveStudentNotificationLog(student, lineUserId, notificationType, "STUDENT_LEAVE_REQUEST",
					leaveRequest.getId(), title, content, LineNotificationLog.STATUS_SKIPPED,
					"缺少請假申請 LINE 使用者", null);
			return;
		}
		var result = lineMessageService.pushText(lineUserId, content);
		saveStudentNotificationLog(student, lineUserId, notificationType, "STUDENT_LEAVE_REQUEST",
				leaveRequest.getId(), title, content,
				result.success() ? LineNotificationLog.STATUS_SENT : LineNotificationLog.STATUS_FAILED,
				result.errorMessage(), result.providerMessageId());
	}

	private void sendAttendanceNotification(StudentAttendance attendance, String notificationType,
			String title, String statusText, LocalDateTime eventTime) {
		Student student = attendance.getStudent();
		if (lineNotificationLogRepository.existsByStudentAndNotificationTypeAndReferenceTypeAndReferenceId(
				student, notificationType, "STUDENT_ATTENDANCE", attendance.getId())) {
			return;
		}
		List<ParentLineBinding> bindings = parentLineBindingRepository
				.findByStudentAndStatus(student, ParentLineBinding.STATUS_BOUND);
		if (bindings.isEmpty()) {
			saveAttendanceLog(attendance, notificationType, title,
					buildAttendanceMessage(attendance, null, statusText, eventTime),
					LineNotificationLog.STATUS_SKIPPED, "尚無已綁定家長", null);
			return;
		}

		int successCount = 0;
		String firstError = null;
		String firstProviderMessageId = null;
		for (ParentLineBinding binding : bindings) {
			String content = buildAttendanceMessage(attendance, binding, statusText, eventTime);
			var result = lineMessageService.pushText(binding.getLineUserId(), content);
			if (result.success()) {
				successCount++;
				if (firstProviderMessageId == null) {
					firstProviderMessageId = result.providerMessageId();
				}
			} else if (firstError == null) {
				firstError = result.errorMessage();
			}
		}
		String status = successCount > 0 ? LineNotificationLog.STATUS_SENT : LineNotificationLog.STATUS_FAILED;
		String error = successCount == bindings.size()
				? null
				: "成功 " + successCount + " / " + bindings.size()
						+ (firstError == null ? "" : "；" + firstError);
		saveAttendanceLog(attendance, notificationType, title,
				buildAttendanceMessage(attendance, null, statusText, eventTime),
				status, error, firstProviderMessageId);
	}

	private String buildAttendanceMessage(StudentAttendance attendance, ParentLineBinding binding,
			String statusText, LocalDateTime eventTime) {
		Student student = attendance.getStudent();
		String recipient = binding == null || binding.getRelation() == null || binding.getRelation().isBlank()
				? ""
				: studentNameSuffix(student.getChineseName()) + binding.getRelation();
		String greeting = recipient.isBlank() ? "" : recipient + "您好：\n";
		String timeText = eventTime == null ? "-" : eventTime.format(DISPLAY_TIME_FORMAT);
		String className = attendance.getCourseDisplayText() == null || attendance.getCourseDisplayText().isBlank()
				? (attendance.getClassRoom() == null ? "無" : attendance.getClassRoom().getDisplayName())
				: attendance.getCourseDisplayText();
		return "【Whole Summer " + statusText + "通知】\n\n"
				+ greeting
				+ "學生：" + student.getDisplayName() + "\n"
				+ "狀態：" + statusText + "\n"
				+ "時間：" + timeText + "\n"
				+ "課程：" + className;
	}

	private String buildLateArrivalReminderMessage(Student student, ParentLineBinding binding,
			String className, LocalDateTime classStartTime) {
		String recipient = binding == null || binding.getRelation() == null || binding.getRelation().isBlank()
				? ""
				: studentNameSuffix(student.getChineseName()) + binding.getRelation();
		String greeting = recipient.isBlank() ? "" : recipient + "您好：\n";
		String timeText = classStartTime == null ? "-" : classStartTime.format(DISPLAY_TIME_FORMAT);
		String displayClassName = className == null || className.isBlank() ? "-" : className;
		return "【Whole Summer 遲到提醒】\n\n"
				+ greeting
				+ "學生：" + student.getDisplayName() + "\n"
				+ "狀態：尚未到班\n"
				+ "上課時間：" + timeText + "\n"
				+ "課程：" + displayClassName + "\n\n"
				+ "系統尚未收到學生到班刷卡紀錄，請協助確認學生狀況。";
	}

	private String buildGroupedLateArrivalReminderMessage(List<LateArrivalReminder> reminders,
			ParentLineBinding binding) {
		String greeting = binding == null || binding.getRelation() == null || binding.getRelation().isBlank()
				? "家長您好：\n"
				: binding.getRelation() + "您好：\n";
		String timeText = reminders.get(0).classStartTime() == null ? "-"
				: reminders.get(0).classStartTime().format(DISPLAY_TIME_FORMAT);
		String details = reminders.stream()
				.map(reminder -> "學生：" + reminder.student().getDisplayName()
						+ "\n課程：" + (reminder.className() == null || reminder.className().isBlank() ? "-" : reminder.className()))
				.collect(java.util.stream.Collectors.joining("\n\n"));
		return "【Whole Summer 遲到提醒】\n\n" + greeting
				+ "以下學生於 " + timeText + " 尚未到班：\n\n" + details
				+ "\n\n系統尚未收到學生到班刷卡紀錄，請協助確認學生狀況。";
	}

	private String buildStudentLeaveSubmittedMessage(StudentLeaveRequest leaveRequest) {
		return "【Whole Summer 請假申請確認】\n\n"
				+ "已收到您的請假申請。\n\n"
				+ studentLeaveDetailText(leaveRequest)
				+ "\n\n若有疑問可直接詢問告知";
	}

	private String buildStudentLeaveApprovedMessage(StudentLeaveRequest leaveRequest) {
		return "【Whole Summer 請假審核通知】\n\n"
				+ "補習班教師已確認請假紀錄。\n\n"
				+ studentLeaveDetailText(leaveRequest);
	}

	private String studentLeaveDetailText(StudentLeaveRequest leaveRequest) {
		Student student = leaveRequest.getStudent();
		String className = leaveRequest.getClassRoom() == null ? "-" : leaveRequest.getClassRoom().getDisplayName();
		String startText = leaveRequest.getScheduledStartAt() == null
				? "-"
				: leaveRequest.getScheduledStartAt().format(DISPLAY_TIME_FORMAT);
		String endText = leaveRequest.getScheduledEndAt() == null
				? "-"
				: leaveRequest.getScheduledEndAt().format(DateTimeFormatter.ofPattern("HH:mm"));
		String reason = leaveRequest.getReason() == null || leaveRequest.getReason().isBlank()
				? "-"
				: leaveRequest.getReason();
		return "學生：" + (student == null ? "-" : student.getDisplayName()) + "\n"
				+ "課程：" + className + "\n"
				+ "時間：" + startText + "-" + endText + "\n"
				+ "請假原因：" + reason;
	}

	private void saveAttendanceLog(StudentAttendance attendance, String notificationType, String title,
			String content, String status, String errorMessage, String providerMessageId) {
		LineNotificationLog log = new LineNotificationLog();
		log.setStudent(attendance.getStudent());
		log.setNotificationType(notificationType);
		log.setReferenceType("STUDENT_ATTENDANCE");
		log.setReferenceId(attendance.getId());
		log.setTitle(title);
		log.setContent(content);
		log.setStatus(status);
		log.setErrorMessage(errorMessage);
		log.setProviderMessageId(providerMessageId);
		log.setSentAt(LocalDateTime.now());
		lineNotificationLogRepository.save(log);
	}

	private void saveStudentNotificationLog(Student student, String notificationType, String referenceType,
			Long referenceId, String title, String content, String status, String errorMessage,
			String providerMessageId) {
		saveStudentNotificationLog(student, null, notificationType, referenceType, referenceId, title,
				content, status, errorMessage, providerMessageId);
	}

	private void saveStudentNotificationLog(Student student, String lineUserId, String notificationType,
			String referenceType, Long referenceId, String title, String content, String status, String errorMessage,
			String providerMessageId) {
		LineNotificationLog log = new LineNotificationLog();
		log.setStudent(student);
		log.setLineUserId(lineUserId);
		log.setNotificationType(notificationType);
		log.setReferenceType(referenceType);
		log.setReferenceId(referenceId);
		log.setTitle(title);
		log.setContent(content);
		log.setStatus(status);
		log.setErrorMessage(errorMessage);
		log.setProviderMessageId(providerMessageId);
		log.setSentAt(LocalDateTime.now());
		lineNotificationLogRepository.save(log);
	}

	private String buildTestMessage(Student student) {
		return "【Whole Summer 測試通知】\n\n"
				+ "學生：" + student.getDisplayName() + "\n"
				+ "時間：" + LocalDateTime.now().format(DISPLAY_TIME_FORMAT) + "\n\n"
				+ "這是一則 LINE 家長通知測試。收到此訊息代表綁定與推播功能正常。";
	}

	private String studentNameSuffix(String name) {
		if (name == null || name.isBlank()) {
			return "";
		}
		String normalized = name.trim();
		return normalized.length() <= 2 ? normalized : normalized.substring(normalized.length() - 2);
	}

	private void saveLog(Student student, String lineUserId, String status, String title,
			String content, String errorMessage, String providerMessageId) {
		LineNotificationLog log = new LineNotificationLog();
		log.setStudent(student);
		log.setLineUserId(lineUserId);
		log.setNotificationType("TEST");
		log.setReferenceType("MANUAL_TEST");
		log.setTitle(title);
		log.setContent(content);
		log.setStatus(status);
		log.setErrorMessage(errorMessage);
		log.setProviderMessageId(providerMessageId);
		log.setSentAt(LocalDateTime.now());
		lineNotificationLogRepository.save(log);
	}

	public record LateArrivalReminder(Student student, Long referenceId, String className,
			LocalDateTime classStartTime, ParentLineBinding binding) {
		public LateArrivalReminder(Student student, Long referenceId, String className, LocalDateTime classStartTime) {
			this(student, referenceId, className, classStartTime, null);
		}

		LateArrivalReminder withBinding(ParentLineBinding binding) {
			return new LateArrivalReminder(student, referenceId, className, classStartTime, binding);
		}
	}
}
