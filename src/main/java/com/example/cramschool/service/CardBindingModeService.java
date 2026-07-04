package com.example.cramschool.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.cramschool.dto.CardCheckInRequest;
import com.example.cramschool.dto.CardCheckInResponse;
import com.example.cramschool.entity.Student;
import com.example.cramschool.entity.Teacher;

@Service
public class CardBindingModeService {

	private static final String TARGET_STUDENT = "STUDENT";
	private static final String TARGET_TEACHER = "TEACHER";
	private static final int EXPIRES_AFTER_MINUTES = 3;

	private final StudentService studentService;
	private final TeacherService teacherService;

	private PendingBinding pendingBinding;

	public CardBindingModeService(StudentService studentService, TeacherService teacherService) {
		this.studentService = studentService;
		this.teacherService = teacherService;
	}

	public synchronized PendingBinding start(String targetType, Long targetId, boolean overwriteExisting,
			Long currentTeacherId) {
		if (TARGET_STUDENT.equals(targetType)) {
			Student student = studentService.findById(targetId);
			pendingBinding = new PendingBinding(targetType, targetId, student.getDisplayName(),
					overwriteExisting, currentTeacherId, LocalDateTime.now().plusMinutes(EXPIRES_AFTER_MINUTES));
			return pendingBinding;
		}
		if (TARGET_TEACHER.equals(targetType)) {
			Teacher teacher = teacherService.findById(targetId);
			pendingBinding = new PendingBinding(targetType, targetId, teacher.getDisplayName(),
					overwriteExisting, currentTeacherId, LocalDateTime.now().plusMinutes(EXPIRES_AFTER_MINUTES));
			return pendingBinding;
		}
		throw new IllegalArgumentException("請選擇學生或教師");
	}

	public synchronized Optional<PendingBinding> current() {
		clearIfExpired();
		return Optional.ofNullable(pendingBinding);
	}

	public synchronized void cancel() {
		pendingBinding = null;
	}

	public synchronized Optional<CardCheckInResponse> completeIfPending(CardCheckInRequest request) {
		clearIfExpired();
		if (pendingBinding == null) {
			return Optional.empty();
		}
		PendingBinding binding = pendingBinding;
		pendingBinding = null;
		String normalizedCardId = CardIdNormalizer.normalize(request == null ? null : request.getCardId());
		if (normalizedCardId == null) {
			CardCheckInResponse response = CardCheckInResponse.fail("CARD_BIND_FAILED", "卡號無效，綁定未完成");
			response.setCardId(normalizedCardId);
			return Optional.of(response);
		}
		try {
			if (TARGET_STUDENT.equals(binding.targetType())) {
				Student student = studentService.bindCard(binding.targetId(), normalizedCardId,
						binding.overwriteExisting(), binding.requestedByTeacherId());
				return Optional.of(successForStudent(student, normalizedCardId));
			}
			if (TARGET_TEACHER.equals(binding.targetType())) {
				Teacher teacher = teacherService.bindCard(binding.targetId(), normalizedCardId,
						binding.overwriteExisting(), binding.requestedByTeacherId());
				return Optional.of(successForTeacher(teacher, normalizedCardId));
			}
			CardCheckInResponse response = CardCheckInResponse.fail("CARD_BIND_FAILED", "待綁定對象無效，綁定未完成");
			response.setCardId(normalizedCardId);
			return Optional.of(response);
		} catch (IllegalArgumentException ex) {
			CardCheckInResponse response = CardCheckInResponse.fail("CARD_BIND_FAILED", ex.getMessage());
			response.setCardId(normalizedCardId);
			return Optional.of(response);
		}
	}

	private CardCheckInResponse successForStudent(Student student, String cardId) {
		CardCheckInResponse response = new CardCheckInResponse();
		response.setSuccess(true);
		response.setStatus("CARD_BOUND");
		response.setMessage(student.getDisplayName() + " 卡片綁定成功");
		response.setStudentId(student.getId());
		response.setStudentName(student.getDisplayName());
		response.setPersonType("STUDENT");
		response.setCheckInTime(LocalDateTime.now());
		response.setCardId(cardId);
		return response;
	}

	private CardCheckInResponse successForTeacher(Teacher teacher, String cardId) {
		CardCheckInResponse response = new CardCheckInResponse();
		response.setSuccess(true);
		response.setStatus("CARD_BOUND");
		response.setMessage(teacher.getDisplayName() + " 卡片綁定成功");
		response.setTeacherId(teacher.getId());
		response.setTeacherName(teacher.getDisplayName());
		response.setPersonType("TEACHER");
		response.setCheckInTime(LocalDateTime.now());
		response.setCardId(cardId);
		return response;
	}

	private void clearIfExpired() {
		if (pendingBinding != null && LocalDateTime.now().isAfter(pendingBinding.expiresAt())) {
			pendingBinding = null;
		}
	}

	public record PendingBinding(
			String targetType,
			Long targetId,
			String displayName,
			boolean overwriteExisting,
			Long requestedByTeacherId,
			LocalDateTime expiresAt) {
	}
}
