package com.example.cramschool.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.cramschool.dto.WeeklyScheduleDto;
import com.example.cramschool.entity.ScheduleType;

class StudentAttendanceServiceTests {

	@Test
	void isClassDayUsesWeeklyScheduleEventsForCancelledAndRescheduledCourses() {
		Long classRoomId = 11L;
		LocalDate originalDate = LocalDate.of(2026, 7, 7);
		LocalDate rescheduledDate = LocalDate.of(2026, 7, 9);
		WeeklyScheduleService weeklyScheduleService = new WeeklyScheduleService(null, null, null, null, null) {
			@Override
			public List<WeeklyScheduleDto> findWeeklySchedules(LocalDate date, Long currentTeacherId,
					boolean director, Long teacherFilterId, Long classFilterId) {
				if (originalDate.equals(date)) {
					return List.of(schedule(classRoomId, originalDate, ScheduleType.CANCELLED));
				}
				if (rescheduledDate.equals(date)) {
					return List.of(schedule(classRoomId, rescheduledDate, ScheduleType.RESCHEDULED));
				}
				return List.of();
			}
		};
		StudentAttendanceService service = new StudentAttendanceService(
				null, null, null, null, null, null, null, weeklyScheduleService);

		assertThat(service.isClassDay(classRoomId, originalDate)).isFalse();
		assertThat(service.isClassDay(classRoomId, rescheduledDate)).isTrue();
	}

	private WeeklyScheduleDto schedule(Long classRoomId, LocalDate date, ScheduleType scheduleType) {
		return new WeeklyScheduleDto(
				101L,
				100L,
				classRoomId,
				"數學",
				"國一數學",
				"王老師",
				date,
				LocalDateTime.of(date, LocalTime.of(18, 0)),
				LocalDateTime.of(date, LocalTime.of(20, 0)),
				scheduleType,
				null,
				null,
				"數學",
				"王老師",
				"國一");
	}
}
