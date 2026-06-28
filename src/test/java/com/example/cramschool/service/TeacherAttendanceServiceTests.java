package com.example.cramschool.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.cramschool.dto.TeacherDailySchedule;
import com.example.cramschool.dto.TeacherDailySchedule.TimeRange;
import com.example.cramschool.entity.Teacher;
import com.example.cramschool.entity.TeacherAttendance;
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

	@Test
	void directorCanAddManualHoursToUnmatchedAttendance() {
		TeacherAttendanceService service = new TeacherAttendanceService(null, null, null);
		TeacherAttendance attendance = attendanceWithoutCourse();

		service.applyManualAdjustment(
				attendance, "加課", new BigDecimal("2.50"), 3L, true);

		assertThat(attendance.isManualAdjusted()).isTrue();
		assertThat(attendance.getManualRemark()).isEqualTo("加課");
		assertThat(attendance.getManualHours()).isEqualByComparingTo("2.50");
		assertThat(attendance.getAdjustedByTeacherId()).isEqualTo(3L);
		assertThat(attendance.getWorkMinutes()).isEqualTo(150);
	}

	@Test
	void manualAdjustmentRejectsNonDirectorAndMatchedCourse() {
		TeacherDailySchedule schedule = new TeacherDailySchedule(List.of(
				new TimeRange(LocalTime.of(18, 0), LocalTime.of(20, 0))), 120);
		TeacherScheduleService scheduleService = new TeacherScheduleService(null) {
			@Override
			public TeacherDailySchedule findDailySchedule(Long teacherId, LocalDate date) {
				return schedule;
			}
		};
		TeacherAttendanceService service = new TeacherAttendanceService(null, null, scheduleService);
		TeacherAttendance unmatched = attendanceWithoutCourse();

		assertThatThrownBy(() -> service.applyManualAdjustment(
				unmatched, "加課", BigDecimal.ONE, 3L, false))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("只有主任可以調整打卡紀錄");

		unmatched.setMatchedCourseId(5L);
		unmatched.setClockInTime(LocalTime.of(17, 30));
		unmatched.setClockOutTime(LocalTime.of(20, 30));
		assertThatThrownBy(() -> service.applyManualAdjustment(
				unmatched, "加課", BigDecimal.ONE, 3L, true))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("只有無對應課程，或打卡時間超出課程前後一小時的紀錄可手動調整");
	}

	@Test
	void allowsManualAdjustmentWhenPunchIsMoreThanOneHourOutsideClassTime() {
		TeacherAttendance attendance = attendanceWithoutCourse();
		attendance.setMatchedCourseId(5L);
		TeacherDailySchedule schedule = new TeacherDailySchedule(List.of(
				new TimeRange(LocalTime.of(18, 0), LocalTime.of(20, 0))), 120);

		attendance.setClockInTime(LocalTime.of(16, 59));
		attendance.setClockOutTime(LocalTime.of(20, 0));
		assertThat(service.isMoreThanOneHourOutsideSchedule(attendance, schedule)).isTrue();

		attendance.setClockInTime(LocalTime.of(18, 0));
		attendance.setClockOutTime(LocalTime.of(21, 1));
		assertThat(service.isMoreThanOneHourOutsideSchedule(attendance, schedule)).isTrue();
	}

	@Test
	void directorCanAddManualHoursToMatchedHoursWhenPunchIsOutsideClassTime() {
		TeacherDailySchedule schedule = new TeacherDailySchedule(List.of(
				new TimeRange(LocalTime.of(18, 0), LocalTime.of(20, 0))), 120);
		TeacherScheduleService scheduleService = new TeacherScheduleService(null) {
			@Override
			public TeacherDailySchedule findDailySchedule(Long teacherId, LocalDate date) {
				return schedule;
			}
		};
		TeacherAttendanceService service = new TeacherAttendanceService(null, null, scheduleService);
		TeacherAttendance attendance = attendanceWithoutCourse();
		attendance.setMatchedCourseId(5L);
		attendance.setWorkMinutes(120L);
		attendance.setClockInTime(LocalTime.of(16, 30));
		attendance.setClockOutTime(LocalTime.of(20, 0));

		service.applyManualAdjustment(
				attendance, "提早備課", new BigDecimal("2.50"), 3L, true);

		assertThat(attendance.isManualAdjusted()).isTrue();
		assertThat(attendance.getWorkMinutes()).isEqualTo(270);
	}

	@Test
	void doesNotAllowMatchedAttendanceExactlyOneHourOutsideClassTime() {
		TeacherAttendance attendance = attendanceWithoutCourse();
		attendance.setMatchedCourseId(5L);
		attendance.setClockInTime(LocalTime.of(17, 0));
		attendance.setClockOutTime(LocalTime.of(21, 0));
		TeacherDailySchedule schedule = new TeacherDailySchedule(List.of(
				new TimeRange(LocalTime.of(18, 0), LocalTime.of(20, 0))), 120);

		assertThat(service.isMoreThanOneHourOutsideSchedule(attendance, schedule)).isFalse();
	}

	@Test
	void manualHoursAreAddedToAutomaticallyMatchedCourseHours() {
		TeacherAttendance attendance = attendanceWithoutCourse();
		attendance.setMatchedCourseId(5L);
		attendance.setWorkMinutes(120L);
		attendance.setManualAdjusted(true);
		attendance.setManualHours(new BigDecimal("3.5"));

		assertThat(attendance.getWorkMinutes()).isEqualTo(330);
	}

	@Test
	void manualHoursMustBeBetweenZeroAndTwentyFour() {
		TeacherAttendanceService service = new TeacherAttendanceService(null, null, null);

		assertThatThrownBy(() -> service.applyManualAdjustment(
				attendanceWithoutCourse(), null, new BigDecimal("-0.01"), 3L, true))
				.hasMessage("增加時數不可小於 0");
		assertThatThrownBy(() -> service.applyManualAdjustment(
				attendanceWithoutCourse(), null, new BigDecimal("24.01"), 3L, true))
				.hasMessage("增加時數不可超過 24 小時");
		assertThatThrownBy(() -> service.applyManualAdjustment(
				attendanceWithoutCourse(), null, new BigDecimal("2.25"), 3L, true))
				.hasMessage("增加時數須以 0.5 小時為間距");
	}

	private TeacherAttendance attendanceWithoutCourse() {
		Teacher teacher = new Teacher();
		teacher.setId(8L);
		TeacherAttendance attendance = new TeacherAttendance();
		attendance.setTeacher(teacher);
		attendance.setDate(LocalDate.of(2026, 6, 25));
		attendance.setStatus(TeacherAttendanceStatus.WORKING);
		return attendance;
	}
}
