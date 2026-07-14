package com.example.cramschool.dto.system;

import java.time.LocalDateTime;

public record OnlineUserDto(
		Long accountId,
		Long teacherId,
		String displayName,
		String roleName,
		LocalDateTime loginAt,
		LocalDateTime lastActiveAt,
		String sessionIdMasked,
		String ipAddress,
		String device,
		String state) {
}
