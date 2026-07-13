package com.example.cramschool.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "web_push_subscriptions")
public class WebPushSubscription {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String endpoint;

	@Column(name = "endpoint_hash", nullable = false, length = 64, unique = true)
	private String endpointHash;

	@Column(name = "vapid_key_hash", length = 64)
	private String vapidKeyHash;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String p256dh;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String auth;

	@Column(length = 100)
	private String browser;

	@Column(name = "user_agent", columnDefinition = "TEXT")
	private String userAgent;

	@Column(name = "device_name", length = 100)
	private String deviceName;

	@Column(nullable = false)
	private boolean enabled = true;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@Column(name = "last_success_at")
	private LocalDateTime lastSuccessAt;

	@Column(name = "last_failure_at")
	private LocalDateTime lastFailureAt;

	@Column(name = "failure_count", nullable = false)
	private int failureCount;

	@PrePersist
	void prePersist() {
		LocalDateTime now = LocalDateTime.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	void preUpdate() {
		this.updatedAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public String getEndpointHash() {
		return endpointHash;
	}

	public void setEndpointHash(String endpointHash) {
		this.endpointHash = endpointHash;
	}

	public String getVapidKeyHash() {
		return vapidKeyHash;
	}

	public void setVapidKeyHash(String vapidKeyHash) {
		this.vapidKeyHash = vapidKeyHash;
	}

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

	public String getBrowser() {
		return browser;
	}

	public void setBrowser(String browser) {
		this.browser = browser;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	public String getDeviceName() {
		return deviceName;
	}

	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public LocalDateTime getLastSuccessAt() {
		return lastSuccessAt;
	}

	public void setLastSuccessAt(LocalDateTime lastSuccessAt) {
		this.lastSuccessAt = lastSuccessAt;
	}

	public LocalDateTime getLastFailureAt() {
		return lastFailureAt;
	}

	public void setLastFailureAt(LocalDateTime lastFailureAt) {
		this.lastFailureAt = lastFailureAt;
	}

	public int getFailureCount() {
		return failureCount;
	}

	public void setFailureCount(int failureCount) {
		this.failureCount = failureCount;
	}
}
