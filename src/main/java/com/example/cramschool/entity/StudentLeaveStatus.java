package com.example.cramschool.entity;

public enum StudentLeaveStatus {
	PENDING("待審核"),
	APPROVED("已核准"),
	REJECTED("已拒絕"),
	CANCELLED("已取消");

	private final String label;

	StudentLeaveStatus(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}
