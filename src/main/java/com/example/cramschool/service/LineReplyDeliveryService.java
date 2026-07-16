package com.example.cramschool.service;

import org.springframework.stereotype.Service;

import com.example.cramschool.dto.LineSendResult;
import com.example.cramschool.entity.LineNotificationLog;

/** Records and sends LINE webhook replies using the same durable guard as pushes. */
@Service
public class LineReplyDeliveryService {

	private final LineNotificationAttemptRecorder recorder;
	private final LineMessageService lineMessageService;

	public LineReplyDeliveryService(LineNotificationAttemptRecorder recorder, LineMessageService lineMessageService) {
		this.recorder = recorder;
		this.lineMessageService = lineMessageService;
	}

	public LineSendResult send(String lineUserId, String replyToken, String content) {
		LineNotificationLog pending = recorder.createPending(null, lineUserId, "LINE_REPLY",
				"LINE_WEBHOOK_REPLY", null, "LINE 回覆", content);
		LineSendResult result;
		try {
			result = lineMessageService.replyText(replyToken, content);
			if (result == null) {
				result = LineSendResult.failure("LINE 回覆服務未回傳結果");
			}
		} catch (RuntimeException ex) {
			result = LineSendResult.failure("LINE 回覆例外：" + (ex.getMessage() == null
					? ex.getClass().getSimpleName() : ex.getMessage()));
		}
		recorder.complete(pending, result);
		return result;
	}
}
