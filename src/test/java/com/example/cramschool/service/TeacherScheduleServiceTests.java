package com.example.cramschool.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.cramschool.dto.TeacherDailySchedule;
import com.example.cramschool.dto.TeacherDailySchedule.TimeRange;
import com.example.cramschool.entity.ClassRoom;
import com.example.cramschool.entity.ClassSchedule;

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

	@Test
	void matchesOverlappingCoursesAndDoesNotCountBreaksOrUnmatchedCourses() {
		TeacherScheduleService service = new TeacherScheduleService(null);
		ClassRoom math = classRoom("國一", "A班",
				schedule(1L, "星期一", "18:00", "20:00"));
		ClassRoom overlapping = classRoom("國一", "B班",
				schedule(2L, "星期一", "19:00", "21:00"));
		ClassRoom outsidePunch = classRoom("國一", "C班",
				schedule(3L, "星期一", "14:00", "16:00"));
		var result = service.matchSchedules(
				List.of(math, overlapping, outsidePunch),
				LocalDate.of(2026, 6, 29),
				LocalTime.of(17, 50), LocalTime.of(20, 10));

		assertThat(result.isMatched()).isTrue();
		assertThat(result.firstScheduleId()).isEqualTo(1L);
		assertThat(result.courseNames()).contains("國一", "A班", "B班");
		assertThat(result.timeRangeText()).isEqualTo("18:00 ~ 21:00");
		assertThat(result.workMinutes()).isEqualTo(180);
	}

	private TimeRange range(String startTime, String endTime) {
		return new TimeRange(LocalTime.parse(startTime), LocalTime.parse(endTime));
	}

	private ClassRoom classRoom(String grade, String classType, ClassSchedule schedule) {
		ClassRoom classRoom = new ClassRoom();
		classRoom.setGrade(grade);
		classRoom.setClassType(classType);
		classRoom.setSchedules(List.of(schedule));
		return classRoom;
	}

	private ClassSchedule schedule(Long id, String weekday, String start, String end) {
		ClassSchedule schedule = new ClassSchedule(
				weekday, LocalTime.parse(start), LocalTime.parse(end));
		schedule.setId(id);
		return schedule;
	}
}
