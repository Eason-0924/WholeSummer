package com.example.cramschool.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class PushSubscriptionRequest {

	@NotBlank
	private String endpoint;

	@NotBlank
	private String applicationServerKey;

	@Valid
	@NotNull
	private PushSubscriptionKeys keys;

	@Size(max = 100)
	private String browser;

	@Size(max = 100)
	private String deviceName;

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public String getApplicationServerKey() {
		return applicationServerKey;
	}

	public void setApplicationServerKey(String applicationServerKey) {
		this.applicationServerKey = applicationServerKey;
	}

	public PushSubscriptionKeys getKeys() {
		return keys;
	}

	public void setKeys(PushSubscriptionKeys keys) {
		this.keys = keys;
	}

	public String getBrowser() {
		return browser;
	}

	public void setBrowser(String browser) {
		this.browser = browser;
	}

	public String getDeviceName() {
		return deviceName;
	}

	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}

	public static class PushSubscriptionKeys {

		@NotBlank
		private String p256dh;

		@NotBlank
		private String auth;

		public String getP256dh() {
			return p256dh;
		}

		public void setP256dh(String p256dh) {
			this.p256dh = p256dh;
		}

		public String getAuth() {
			return auth;
		}

		public void setAuth(String auth) {
			this.auth = auth;
		}
	}
}
