package com.example.cramschool.entity;

public enum LeaveStatus {
	APPROVED("已核准"),
	CANCELLED("已取消");

	private final String displayName;

	LeaveStatus(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
