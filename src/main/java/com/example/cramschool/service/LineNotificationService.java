package com.example.cramschool.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.entity.LineNotificationLog;
import com.example.cramschool.entity.ParentLineBinding;
import com.example.cramschool.entity.Student;
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

	public LineNotificationService(StudentRepository studentRepository,
			ParentLineBindingRepository parentLineBindingRepository,
			LineNotificationLogRepository lineNotificationLogRepository,
			TeacherPermissionService teacherPermissionService, LineMessageService lineMessageService) {
		this.studentRepository = studentRepository;
		this.parentLineBindingRepository = parentLineBindingRepository;
		this.lineNotificationLogRepository = lineNotificationLogRepository;
		this.teacherPermissionService = teacherPermissionService;
		this.lineMessageService = lineMessageService;
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

	private Student findStudent(Long studentId) {
		return studentRepository.findById(studentId)
				.orElseThrow(() -> new IllegalArgumentException("找不到學生資料"));
	}

	private String buildTestMessage(Student student) {
		return "【Whole Summer 測試通知】\n\n"
				+ "學生：" + student.getDisplayName() + "\n"
				+ "時間：" + LocalDateTime.now().format(DISPLAY_TIME_FORMAT) + "\n\n"
				+ "這是一則 LINE 家長通知測試。收到此訊息代表綁定與推播功能正常。";
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
