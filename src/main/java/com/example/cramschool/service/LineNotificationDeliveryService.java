package com.example.cramschool.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.cramschool.dto.LineSendResult;
import com.example.cramschool.entity.LineNotificationLog;
import com.example.cramschool.entity.Student;

/**
 * Guards every LINE push with a durable pre-send record and a post-send result.
 * If the pre-send record cannot be committed, the LINE request is deliberately
 * not made because an untracked external side effect is worse than a failed
 * notification.
 */
@Service
public class LineNotificationDeliveryService {

	private static final Logger log = LoggerFactory.getLogger(LineNotificationDeliveryService.class);

	private final LineNotificationAttemptRecorder recorder;
	private final LineMessageService lineMessageService;

	public LineNotificationDeliveryService(LineNotificationAttemptRecorder recorder,
			LineMessageService lineMessageService) {
		this.recorder = recorder;
		this.lineMessageService = lineMessageService;
	}

	public LineSendResult send(Student student, String lineUserId, String notificationType, String referenceType,
			Long referenceId, String title, String content) {
		return send(List.of(new DeliveryAttempt(student, lineUserId, notificationType, referenceType, referenceId,
				title, content)), lineUserId, content);
	}

	/** Sends one LINE message while recording one or more logical notification rows. */
	public LineSendResult send(List<DeliveryAttempt> attempts, String lineUserId, String content) {
		if (attempts == null || attempts.isEmpty()) {
			return LineSendResult.failure("沒有可記錄的 LINE 通知");
		}
		List<LineNotificationLog> pendingLogs = new java.util.ArrayList<>();
		try {
			for (DeliveryAttempt attempt : attempts) {
				pendingLogs.add(recorder.createPending(attempt.student(), lineUserId,
						attempt.notificationType(), attempt.referenceType(), attempt.referenceId(),
						attempt.title(), content == null ? attempt.content() : content));
			}
		} catch (RuntimeException ex) {
			// No external call is made. Mark rows already created as FAILED; if this
			// update also fails, they remain PENDING and are still auditable.
			log.error("LINE notification was blocked because its pending log could not be created", ex);
			LineSendResult blockedResult = LineSendResult.failure("LINE 通知紀錄建立失敗，已攔截發送：" + safeMessage(ex));
			for (LineNotificationLog pendingLog : pendingLogs) {
				try {
					recorder.complete(pendingLog, blockedResult);
				} catch (RuntimeException completionException) {
					log.error("Blocked LINE notification result could not be persisted; pending row retained",
							completionException);
				}
			}
			return blockedResult;
		}

		LineSendResult result;
		try {
			result = lineMessageService.pushText(lineUserId, content);
			if (result == null) {
				result = LineSendResult.failure("LINE 發送服務未回傳結果");
			}
		} catch (RuntimeException ex) {
			result = LineSendResult.failure("LINE 發送例外：" + safeMessage(ex));
			log.warn("LINE push threw an exception", ex);
		}

		for (LineNotificationLog pendingLog : pendingLogs) {
			try {
				recorder.complete(pendingLog, result);
			} catch (RuntimeException ex) {
				// The committed PENDING row remains as an auditable uncertain attempt.
				log.error("LINE notification result could not be persisted; pending row retained", ex);
			}
		}
		return result;
	}

	private String safeMessage(RuntimeException ex) {
		return ex.getMessage() == null || ex.getMessage().isBlank()
				? ex.getClass().getSimpleName()
				: ex.getMessage();
	}

	public record DeliveryAttempt(Student student, String lineUserId, String notificationType,
			String referenceType, Long referenceId, String title, String content) {
	}
}
