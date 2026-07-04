package com.example.cramschool.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "line")
public class LineProperties {

	private boolean enabled = false;

	private String channelSecret = "";

	private String channelAccessToken = "";

	private String webhookPath = "/api/line/webhook";

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getChannelSecret() {
		return channelSecret;
	}

	public void setChannelSecret(String channelSecret) {
		this.channelSecret = channelSecret;
	}

	public String getChannelAccessToken() {
		return channelAccessToken;
	}

	public void setChannelAccessToken(String channelAccessToken) {
		this.channelAccessToken = channelAccessToken;
	}

	public String getWebhookPath() {
		return webhookPath;
	}

	public void setWebhookPath(String webhookPath) {
		this.webhookPath = webhookPath;
	}
}
