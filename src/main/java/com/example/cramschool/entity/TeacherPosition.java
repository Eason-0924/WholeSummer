package com.example.cramschool.entity;

public enum TeacherPosition {
	TEACHER("教師"),
	TUTOR("輔導老師"),
	DIRECTOR("主任");

	private final String displayName;

	TeacherPosition(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
