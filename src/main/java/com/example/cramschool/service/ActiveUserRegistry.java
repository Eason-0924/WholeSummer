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
		register(sessionId, accountId, teacherId, displayName, "未知");
	}

	public void register(String sessionId, Long accountId, Long teacherId, String displayName, String roleName) {
		if (sessionId == null || accountId == null) {
			return;
		}
		LocalDateTime now = LocalDateTime.now();
		activeUsers.put(sessionId, new ActiveUser(
				sessionId,
				accountId,
				teacherId,
				hasText(displayName) ? displayName.trim() : "未命名使用者",
				hasText(roleName) ? roleName.trim() : "未知",
				now,
				now,
				"未知",
				"未知"));
	}

	public void touch(String sessionId, String ipAddress, String userAgent) {
		if (sessionId == null) {
			return;
		}
		activeUsers.computeIfPresent(sessionId, (key, user) -> new ActiveUser(
				user.sessionId(), user.accountId(), user.teacherId(), user.displayName(), user.roleName(),
				user.loginAt(), LocalDateTime.now(), safe(ipAddress), safe(userAgent)));
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
			String displayName, String roleName, LocalDateTime loginAt, LocalDateTime lastActiveAt,
			String ipAddress, String userAgent) {
	}

	private String safe(String value) {
		return hasText(value) ? value.trim() : "未知";
	}
}
