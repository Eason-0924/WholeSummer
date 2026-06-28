package com.example.cramschool.entity;

public enum MakeUpSourceType {
	LEAVE("請假"),
	ABSENCE("缺勤"),
	RESCHEDULE("調課"),
	MANUAL("手動");

	private final String displayName;

	MakeUpSourceType(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
