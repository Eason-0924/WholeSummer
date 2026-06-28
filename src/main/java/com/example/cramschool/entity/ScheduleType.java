package com.example.cramschool.entity;

public enum ScheduleType {
	NORMAL("原課程"),
	MAKE_UP("補課"),
	RESCHEDULED("調課"),
	CANCELLED("已取消 / 已調課");

	private final String displayName;

	ScheduleType(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
