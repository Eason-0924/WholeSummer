package com.example.cramschool.dto;

import java.time.LocalDate;
import java.util.List;

public record MakeUpCalendarDay(LocalDate date, List<MakeUpAvailableSlot> slots) {

	public long getAvailableCount() {
		return slots.stream()
				.filter(slot -> slot.status() == MakeUpSlotStatus.AVAILABLE)
				.count();
	}

	public long getTeacherConflictCount() {
		return slots.stream()
				.filter(slot -> slot.status() == MakeUpSlotStatus.TEACHER_CONFLICT)
				.count();
	}

	public long getStudentConflictCount() {
		return slots.stream()
				.filter(slot -> slot.status() == MakeUpSlotStatus.STUDENT_CONFLICT)
				.count();
	}
}
