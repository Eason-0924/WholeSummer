package com.example.cramschool.entity;

public enum BugReportStatus {
	PENDING("待寄送"),
	SENT("已寄送"),
	FAILED("寄送失敗");

	private final String displayName;

	BugReportStatus(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
