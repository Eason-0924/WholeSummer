package com.example.cramschool.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import com.example.cramschool.entity.TeacherAttendanceStatus;

class TeacherAttendanceServiceTests {

	private final TeacherAttendanceService service = new TeacherAttendanceService(null, null, null);

	@Test
	void marksTeacherLateWhenClockInIsAfterFirstClass() {
		TeacherAttendanceStatus status = service.resolveWorkingStatus(
				LocalTime.of(18, 1), LocalTime.of(18, 0));

		assertThat(status).isEqualTo(TeacherAttendanceStatus.LATE);
	}

	@Test
	void marksTeacherWorkingWhenClockInIsOnTime() {
		assertThat(service.resolveWorkingStatus(LocalTime.of(17, 55), LocalTime.of(18, 0)))
				.isEqualTo(TeacherAttendanceStatus.WORKING);
		assertThat(service.resolveWorkingStatus(LocalTime.of(18, 0), LocalTime.of(18, 0)))
				.isEqualTo(TeacherAttendanceStatus.WORKING);
	}
}
