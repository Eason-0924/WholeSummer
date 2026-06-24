package com.example.cramschool.service;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.example.cramschool.dto.AvailableUpdate;

@Service
public class UpdateCoordinator {

	private final UpdateChecker updateChecker;
	private final UpdateStateService updateStateService;
	private final boolean enabled;
	private final boolean checkOnStartup;
	private final AtomicBoolean checking = new AtomicBoolean();

	private volatile AvailableUpdate availableUpdate;
	private volatile String lastError;

	public UpdateCoordinator(UpdateChecker updateChecker, UpdateStateService updateStateService,
			@Value("${app.auto-update.enabled:false}") boolean enabled,
			@Value("${app.update.check-on-startup:true}") boolean checkOnStartup) {
		this.updateChecker = updateChecker;
		this.updateStateService = updateStateService;
		this.enabled = enabled;
		this.checkOnStartup = checkOnStartup;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void checkAfterStartup() {
		if (!enabled || !checkOnStartup || !updateStateService.shouldCheckNow()) {
			return;
		}
		checkInBackground();
	}

	public void checkInBackground() {
		if (!checking.compareAndSet(false, true)) {
			return;
		}
		Thread.ofVirtual().name("wholesummer-update-check").start(() -> {
			try {
				checkNow();
			} finally {
				checking.set(false);
			}
		});
	}

	public synchronized Optional<AvailableUpdate> checkNow() {
		if (!enabled) {
			lastError = "自動更新功能尚未啟用";
			return Optional.empty();
		}
		try {
			Optional<AvailableUpdate> result = updateChecker.checkLatest();
			availableUpdate = result
					.filter(update -> !updateStateService.isIgnored(update.latestVersion()))
					.orElse(null);
			lastError = null;
			updateStateService.recordCheckNow();
			return Optional.ofNullable(availableUpdate);
		} catch (IOException ex) {
			lastError = "無法連線 GitHub 檢查更新";
			return Optional.empty();
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			lastError = "更新檢查已中斷";
			return Optional.empty();
		}
	}

	public void ignoreCurrentUpdate() {
		if (availableUpdate == null) {
			return;
		}
		updateStateService.ignoreVersion(availableUpdate.latestVersion());
		availableUpdate = null;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public boolean isChecking() {
		return checking.get();
	}

	public Optional<AvailableUpdate> getAvailableUpdate() {
		return Optional.ofNullable(availableUpdate);
	}

	public String getLastError() {
		return lastError;
	}
}
