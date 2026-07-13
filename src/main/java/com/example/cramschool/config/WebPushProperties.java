package com.example.cramschool.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "webpush.vapid")
public class WebPushProperties {

	private String publicKey = "";
	private String privateKey = "";
	private String subject = "mailto:admin@whole-summer.com";

	public boolean isConfigured() {
		return hasText(publicKey) && hasText(privateKey) && hasText(subject);
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	public String getPublicKey() {
		return publicKey;
	}

	public void setPublicKey(String publicKey) {
		this.publicKey = publicKey == null ? "" : publicKey.trim();
	}

	public String getPrivateKey() {
		return privateKey;
	}

	public void setPrivateKey(String privateKey) {
		this.privateKey = privateKey == null ? "" : privateKey.trim();
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject == null ? "" : subject.trim();
	}
}
