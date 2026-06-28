package com.example.cramschool.dto;

public enum MakeUpSlotStatus {
	STUDENT_CONFLICT("學生已有其他課程", "danger"),
	TEACHER_CONFLICT("教師已有其他課程", "warning"),
	AVAILABLE("學生與教師皆無課程", "success");

	private final String displayName;
	private final String cssClass;

	MakeUpSlotStatus(String displayName, String cssClass) {
		this.displayName = displayName;
		this.cssClass = cssClass;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getCssClass() {
		return cssClass;
	}
}
