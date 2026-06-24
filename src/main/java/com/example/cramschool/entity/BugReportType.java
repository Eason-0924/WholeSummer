package com.example.cramschool.entity;

public enum BugReportType {
	BUG("錯誤回報"),
	USAGE("操作問題"),
	FEATURE("功能建議"),
	OTHER("其他");

	private final String displayName;

	BugReportType(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
