package com.example.cramschool.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.cramschool.dto.TeacherDailySchedule;
import com.example.cramschool.dto.TeacherDailySchedule.TimeRange;

class TeacherScheduleServiceTests {

	@Test
	void mergesOverlappingClassesAndExcludesBreaks() {
		TeacherScheduleService service = new TeacherScheduleService(null);
		TeacherDailySchedule result = service.mergeTimeRanges(List.of(
				range("13:00", "15:00"),
				range("10:00", "12:00"),
				range("09:00", "11:00"),
				range("13:00", "15:00")));

		assertThat(result.getTimeRangeText()).isEqualTo("09:00 ~ 12:00、13:00 ~ 15:00");
		assertThat(result.getWorkMinutes()).isEqualTo(300);
		assertThat(result.getFirstStartTime()).isEqualTo(LocalTime.of(9, 0));
	}

	@Test
	void returnsZeroForEmptySchedule() {
		TeacherScheduleService service = new TeacherScheduleService(null);
		TeacherDailySchedule result = service.mergeTimeRanges(List.of());

		assertThat(result.getWorkMinutes()).isZero();
		assertThat(result.getTimeRangeText()).isEqualTo("當日無課程");
	}

	private TimeRange range(String startTime, String endTime) {
		return new TimeRange(LocalTime.parse(startTime), LocalTime.parse(endTime));
	}
}
