package com.example.cramschool.entity;

public enum MakeUpStatus {
	PENDING("待補課"),
	SCHEDULED("已安排"),
	COMPLETED("已完成"),
	CANCELLED("已取消");

	private final String displayName;

	MakeUpStatus(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
