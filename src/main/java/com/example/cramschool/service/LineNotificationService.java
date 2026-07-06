package com.example.cramschool.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.config.LineProperties;
import com.example.cramschool.entity.LineNotificationLog;
import com.example.cramschool.entity.ParentLineBinding;
import com.example.cramschool.entity.Student;
import com.example.cramschool.entity.StudentAttendance;
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
		if (student == null || student.getId() == null || referenceId == null) {
			return;
		}
		String notificationType = "ATTENDANCE_LATE_REMINDER";
		String referenceType = "CLASS_SCHEDULE_OCCURRENCE";
		if (lineNotificationLogRepository.existsByStudentAndNotificationTypeAndReferenceTypeAndReferenceId(
				student, notificationType, referenceType, referenceId)) {
			return;
		}
		String title = "LINE 遲到提醒";
		List<ParentLineBinding> bindings = parentLineBindingRepository
				.findByStudentAndStatus(student, ParentLineBinding.STATUS_BOUND);
		if (bindings.isEmpty()) {
			saveStudentNotificationLog(student, notificationType, referenceType, referenceId, title,
					buildLateArrivalReminderMessage(student, null, className, classStartTime),
					LineNotificationLog.STATUS_SKIPPED, "尚無已綁定家長", null);
			return;
		}

		int successCount = 0;
		String firstError = null;
		String firstProviderMessageId = null;
		for (ParentLineBinding binding : bindings) {
			String content = buildLateArrivalReminderMessage(student, binding, className, classStartTime);
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
		saveStudentNotificationLog(student, notificationType, referenceType, referenceId, title,
				buildLateArrivalReminderMessage(student, null, className, classStartTime),
				status, error, firstProviderMessageId);
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
		String className = attendance.getClassRoom() == null ? "-" : attendance.getClassRoom().getDisplayName();
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
		LineNotificationLog log = new LineNotificationLog();
		log.setStudent(student);
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
}
