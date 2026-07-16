package com.example.cramschool.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.dto.LineSendResult;
import com.example.cramschool.entity.LineNotificationLog;
import com.example.cramschool.entity.Student;
import com.example.cramschool.repository.LineNotificationLogRepository;

/**
 * Persists the delivery state in independent transactions. The pending row is
 * committed before the external LINE call, so a provider success can never be
 * hidden by a later application transaction rollback.
 */
@Service
public class LineNotificationAttemptRecorder {

	private final LineNotificationLogRepository repository;

	public LineNotificationAttemptRecorder(LineNotificationLogRepository repository) {
		this.repository = repository;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public LineNotificationLog createPending(Student student, String lineUserId, String notificationType,
			String referenceType, Long referenceId, String title, String content) {
		LineNotificationLog log = new LineNotificationLog();
		log.setStudent(student);
		log.setLineUserId(lineUserId);
		log.setNotificationType(notificationType);
		log.setReferenceType(referenceType);
		log.setReferenceId(referenceId);
		log.setTitle(title);
		log.setContent(content);
		log.setStatus(LineNotificationLog.STATUS_PENDING);
		log.setSentAt(null);
		LineNotificationLog persisted = repository.saveAndFlush(log);
		return persisted == null ? log : persisted;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void complete(LineNotificationLog log, LineSendResult result) {
		if (log == null) {
			return;
		}
		LineSendResult safeResult = result == null
				? LineSendResult.failure("LINE 發送服務未回傳結果")
				: result;
		log.setStatus(safeResult.success() ? LineNotificationLog.STATUS_SENT : LineNotificationLog.STATUS_FAILED);
		log.setProviderMessageId(safeResult.providerMessageId());
		log.setErrorMessage(safeResult.errorMessage());
		log.setSentAt(LocalDateTime.now());
		repository.saveAndFlush(log);
	}
}
