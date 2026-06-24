package com.example.cramschool.entity;

public enum TeacherStatus {
	ACTIVE("任教中"),
	LEFT("已離職");

	private final String displayName;

	TeacherStatus(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
