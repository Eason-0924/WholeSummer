package com.example.cramschool.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.dto.LineBindingReply;
import com.example.cramschool.dto.LineBindCodeResult;
import com.example.cramschool.entity.LineBindCode;
import com.example.cramschool.entity.ParentLineBinding;
import com.example.cramschool.entity.Student;
import com.example.cramschool.entity.TeacherPermissionType;
import com.example.cramschool.repository.LineBindCodeRepository;
import com.example.cramschool.repository.ParentLineBindingRepository;
import com.example.cramschool.repository.StudentRepository;

@Service
@Transactional
public class LineBindingService {

	private static final int CODE_BOUND = 1_000_000;
	private static final int CODE_VALID_HOURS = 24;
	private static final Pattern BIND_COMMAND_PATTERN = Pattern.compile("^綁定\\s+([0-9]{6})(?:\\s+(.{1,30}))?$");
	private static final SecureRandom RANDOM = new SecureRandom();

	private final LineBindCodeRepository lineBindCodeRepository;
	private final ParentLineBindingRepository parentLineBindingRepository;
	private final StudentRepository studentRepository;
	private final TeacherPermissionService teacherPermissionService;

	public LineBindingService(LineBindCodeRepository lineBindCodeRepository,
			ParentLineBindingRepository parentLineBindingRepository,
			StudentRepository studentRepository, TeacherPermissionService teacherPermissionService) {
		this.lineBindCodeRepository = lineBindCodeRepository;
		this.parentLineBindingRepository = parentLineBindingRepository;
		this.studentRepository = studentRepository;
		this.teacherPermissionService = teacherPermissionService;
	}

	public LineBindCodeResult createBindCode(Long studentId, Long currentTeacherId, String relation) {
		teacherPermissionService.requirePermission(currentTeacherId, TeacherPermissionType.STUDENT_UPDATE,
				"權限不足，無法產生 LINE 綁定碼");
		Student student = studentRepository.findById(studentId)
				.orElseThrow(() -> new IllegalArgumentException("找不到學生資料"));
		String normalizedRelation = normalizeRelation(relation);

		LineBindCode bindCode = new LineBindCode();
		bindCode.setStudent(student);
		bindCode.setRelation(normalizedRelation);
		bindCode.setCode(generateCode());
		bindCode.setExpiredAt(LocalDateTime.now().plusHours(CODE_VALID_HOURS));
		LineBindCode saved = lineBindCodeRepository.save(bindCode);
		String instructionText = "綁定 " + saved.getCode() + " " + saved.getRelation();
		return new LineBindCodeResult(saved.getCode(), saved.getRelation(), instructionText, saved.getExpiredAt());
	}

	@Transactional(readOnly = true)
	public List<LineBindCode> findRecentBindCodes(Long studentId) {
		Student student = studentRepository.findById(studentId)
				.orElseThrow(() -> new IllegalArgumentException("找不到學生資料"));
		return lineBindCodeRepository.findTop5ByStudentOrderByCreatedAtDesc(student);
	}

	public LineBindingReply bindFromLineMessage(String lineUserId, String lineDisplayName, String messageText) {
		if (messageText == null) {
			return LineBindingReply.ignored();
		}
		Matcher matcher = BIND_COMMAND_PATTERN.matcher(messageText.trim());
		if (!matcher.matches()) {
			return LineBindingReply.ignored();
		}
		if (lineUserId == null || lineUserId.isBlank()) {
			return LineBindingReply.failure("綁定失敗：無法取得 LINE 使用者資訊，請重新加入官方帳號後再試一次。");
		}

		String code = matcher.group(1);
		LineBindCode bindCode = lineBindCodeRepository
				.findFirstByCodeAndExpiredAtAfterOrderByCreatedAtDesc(code, LocalDateTime.now())
				.orElse(null);
		if (bindCode == null) {
			return LineBindingReply.failure("綁定失敗：驗證碼不存在或已過期，請向補習班重新索取綁定碼。");
		}

		String relation = normalizeRelationOrDefault(matcher.group(2), bindCode.getRelation());
		Student student = bindCode.getStudent();
		return parentLineBindingRepository.findByStudentAndLineUserId(student, lineUserId)
				.map(existing -> refreshExistingBinding(existing, relation, lineDisplayName, student))
				.orElseGet(() -> createBinding(bindCode, relation, lineUserId, lineDisplayName, student));
	}

	private LineBindingReply refreshExistingBinding(ParentLineBinding binding, String relation,
			String lineDisplayName, Student student) {
		binding.setStatus(ParentLineBinding.STATUS_BOUND);
		binding.setRelation(relation);
		binding.setLineDisplayName(normalizeNullable(lineDisplayName));
		parentLineBindingRepository.save(binding);
		return LineBindingReply.success("您已綁定過此學生。\n學生：" + student.getDisplayName());
	}

	private LineBindingReply createBinding(LineBindCode bindCode, String relation, String lineUserId,
			String lineDisplayName, Student student) {
		ParentLineBinding binding = new ParentLineBinding();
		binding.setStudent(student);
		binding.setParentName(bindCode.getParentName());
		binding.setRelation(relation);
		binding.setLineUserId(lineUserId);
		binding.setLineDisplayName(normalizeNullable(lineDisplayName));
		parentLineBindingRepository.save(binding);
		return LineBindingReply.success("綁定成功！\n學生：" + student.getDisplayName()
				+ "\n之後您將收到上課、缺席、補課、成績等通知。");
	}

	private String normalizeNullable(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private String normalizeRelation(String relation) {
		String normalized = normalizeNullable(relation);
		if (normalized == null) {
			throw new IllegalArgumentException("請選擇家長關係");
		}
		if (normalized.length() > 30) {
			throw new IllegalArgumentException("家長關係不可超過 30 個字");
		}
		return normalized;
	}

	private String normalizeRelationOrDefault(String relation, String defaultRelation) {
		String normalized = normalizeNullable(relation);
		if (normalized == null) {
			return normalizeRelation(defaultRelation);
		}
		return normalizeRelation(normalized);
	}

	private String generateCode() {
		return String.format("%06d", RANDOM.nextInt(CODE_BOUND));
	}
}
