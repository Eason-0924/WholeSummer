package com.example.cramschool.entity;

public enum HomeworkStatus {
	NOT_SUBMITTED("未繳交"),
	SUBMITTED("已繳交"),
	LATE("逾期補交"),
	EXCUSED("免交");

	private final String displayName;

	HomeworkStatus(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}

	public boolean isCompleted() {
		return this == SUBMITTED || this == LATE || this == EXCUSED;
	}
}
