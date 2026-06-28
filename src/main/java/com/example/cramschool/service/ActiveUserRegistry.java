package com.example.cramschool.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

@Component
public class ActiveUserRegistry implements HttpSessionListener {

	private final Map<String, ActiveUser> activeUsers = new ConcurrentHashMap<>();

	public void register(String sessionId, Long accountId, Long teacherId, String displayName) {
		if (sessionId == null || accountId == null) {
			return;
		}
		activeUsers.put(sessionId, new ActiveUser(
				sessionId,
				accountId,
				teacherId,
				hasText(displayName) ? displayName.trim() : "未命名使用者",
				LocalDateTime.now()));
	}

	public void unregister(String sessionId) {
		if (sessionId != null) {
			activeUsers.remove(sessionId);
		}
	}

	public List<ActiveUser> findActiveUsers() {
		return activeUsers.values().stream()
				.sorted(Comparator.comparing(ActiveUser::loginAt).reversed())
				.toList();
	}

	public int count() {
		return activeUsers.size();
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent event) {
		unregister(event.getSession().getId());
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	public record ActiveUser(String sessionId, Long accountId, Long teacherId,
			String displayName, LocalDateTime loginAt) {
	}
}
