package com.example.cramschool.entity;

public enum TeacherAttendanceStatus {
	WORKING("出勤"),
	LATE("遲到"),
	LEAVE("請假"),
	ABSENT("缺勤");

	private final String displayName;

	TeacherAttendanceStatus(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
