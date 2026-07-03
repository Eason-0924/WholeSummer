package com.example.cramschool.service;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.cramschool.dto.CardCheckInRequest;
import com.example.cramschool.dto.CardCheckInResponse;
import com.example.cramschool.dto.RecentCardCheckInRecord;

@Service
public class RecentCardCheckInService {

	private static final int MAX_RECORDS = 80;

	private final Deque<RecentCardCheckInRecord> records = new ArrayDeque<>();

	public synchronized void record(CardCheckInRequest request, CardCheckInResponse response) {
		if (response == null) {
			return;
		}
		records.addFirst(new RecentCardCheckInRecord(
				occurredAt(response),
				response.isSuccess(),
				personTypeLabel(response),
				displayName(response),
				actionLabel(response),
				response.getMessage(),
				blankToDash(response.getClassName()),
				blankToDash(response.getCardId()),
				blankToDash(request == null ? null : request.getDeviceName())));
		while (records.size() > MAX_RECORDS) {
			records.removeLast();
		}
	}

	public synchronized List<RecentCardCheckInRecord> findRecent(int limit) {
		return records.stream()
				.limit(Math.max(0, limit))
				.toList();
	}

	public synchronized void clear() {
		records.clear();
	}

	private LocalDateTime occurredAt(CardCheckInResponse response) {
		if (response.getCheckOutTime() != null) {
			return response.getCheckOutTime();
		}
		if (response.getCheckInTime() != null) {
			return response.getCheckInTime();
		}
		return LocalDateTime.now();
	}

	private String personTypeLabel(CardCheckInResponse response) {
		if ("TEACHER".equals(response.getPersonType()) || response.getTeacherId() != null) {
			return "教師";
		}
		if ("STUDENT".equals(response.getPersonType()) || response.getStudentId() != null) {
			return "學生";
		}
		return "-";
	}

	private String displayName(CardCheckInResponse response) {
		if (response.getStudentName() != null && !response.getStudentName().isBlank()) {
			return response.getStudentName();
		}
		if (response.getTeacherName() != null && !response.getTeacherName().isBlank()) {
			return response.getTeacherName();
		}
		return "-";
	}

	private String actionLabel(CardCheckInResponse response) {
		String status = response.getStatus();
		if (status == null || status.isBlank()) {
			return response.isSuccess() ? "成功" : "失敗";
		}
		return switch (status) {
			case "CHECKED_IN" -> "點名";
			case "CHECKED_OUT" -> "簽退";
			case "CLOCKED_IN" -> "上班";
			case "CLOCKED_OUT" -> "下班";
			case "CARD_NOT_BOUND" -> "卡片未綁定";
			case "CARD_DISABLED" -> "卡片停用";
			case "INVALID_CARD_ID" -> "卡號無效";
			case "NO_CLASS_TODAY" -> "今日無課";
			case "DUPLICATE_CHECK_IN" -> "重複刷卡";
			default -> status;
		};
	}

	private String blankToDash(String value) {
		return value == null || value.isBlank() ? "-" : value;
	}
}
