package com.example.cramschool.entity;

public enum AttendanceStatus {
	PRESENT("出席"),
	LATE("遲到"),
	ABSENT("缺席"),
	LEAVE("請假");

	private final String displayName;

	AttendanceStatus(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
