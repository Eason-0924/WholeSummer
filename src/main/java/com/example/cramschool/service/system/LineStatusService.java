package com.example.cramschool.service.system;

import org.springframework.stereotype.Service;

import com.example.cramschool.config.LineProperties;
import com.example.cramschool.dto.system.LineStatusDto;

@Service
public class LineStatusService {

	private final LineProperties properties;

	public LineStatusService(LineProperties properties) { this.properties = properties; }

	public LineStatusDto getStatus() {
		return new LineStatusDto(properties.isEnabled(), configured(properties.getChannelSecret()),
				configured(properties.getChannelAccessToken()), configured(properties.getLiffId())
						&& configured(properties.getLiffChannelId()), properties.getWebhookPath());
	}

	private boolean configured(String value) { return value != null && !value.isBlank(); }
}
