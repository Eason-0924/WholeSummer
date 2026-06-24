package com.example.cramschool.entity;

public enum BackupStatus {
	SUCCESS("成功"),
	FAILED("失敗");

	private final String displayName;

	BackupStatus(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
