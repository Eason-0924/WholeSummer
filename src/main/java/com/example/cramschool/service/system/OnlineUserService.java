package com.example.cramschool.service.system;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.cramschool.dto.system.OnlineUserDto;
import com.example.cramschool.service.ActiveUserRegistry;

@Service
public class OnlineUserService {

	private static final Duration ONLINE_WINDOW = Duration.ofMinutes(5);
	private static final Duration IDLE_WINDOW = Duration.ofMinutes(30);
	private final ActiveUserRegistry activeUserRegistry;

	public OnlineUserService(ActiveUserRegistry activeUserRegistry) {
		this.activeUserRegistry = activeUserRegistry;
	}

	public List<OnlineUserDto> findUsers() {
		LocalDateTime now = LocalDateTime.now();
		return activeUserRegistry.findActiveUsers().stream()
				.map(user -> new OnlineUserDto(
						user.accountId(), user.teacherId(), user.displayName(), user.roleName(),
						user.loginAt(), user.lastActiveAt(), maskSessionId(user.sessionId()),
						user.ipAddress(), device(user.userAgent()), state(user.lastActiveAt(), now)))
				.toList();
	}

	private String state(LocalDateTime lastActiveAt, LocalDateTime now) {
		if (lastActiveAt == null) return "未知";
		Duration idle = Duration.between(lastActiveAt, now);
		if (idle.compareTo(ONLINE_WINDOW) <= 0) return "在線";
		if (idle.compareTo(IDLE_WINDOW) <= 0) return "閒置";
		return "可能離線";
	}

	private String maskSessionId(String sessionId) {
		if (sessionId == null || sessionId.length() < 8) return "已遮蔽";
		return "********" + sessionId.substring(sessionId.length() - 4);
	}

	private String device(String userAgent) {
		if (userAgent == null || userAgent.isBlank() || "未知".equals(userAgent)) return "未知";
		String normalized = userAgent.replaceAll("\\s+", " ").trim();
		return normalized.length() <= 100 ? normalized : normalized.substring(0, 100) + "…";
	}
}
