package com.example.cramschool.entity;

public enum TuitionStatus {
	UNPAID("未繳費"),
	PARTIALLY_PAID("部分繳費"),
	PAID("已繳清");

	private final String displayName;

	TuitionStatus(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
