package com.example.cramschool.dto.system;

public record LineStatusDto(boolean enabled, boolean channelSecretConfigured,
		boolean accessTokenConfigured, boolean liffConfigured, String webhookPath) {
}
