package com.example.cramschool.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

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
		TeacherAttendanceService service = new TeacherAttendanceService(null, null, null);
		TeacherAttendance unmatched = attendanceWithoutCourse();

		assertThatThrownBy(() -> service.applyManualAdjustment(
				unmatched, "加課", BigDecimal.ONE, 3L, false))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("只有主任可以調整打卡紀錄");

		unmatched.setMatchedCourseId(5L);
		assertThatThrownBy(() -> service.applyManualAdjustment(
				unmatched, "加課", BigDecimal.ONE, 3L, true))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("已有對應課程的打卡紀錄不可手動調整");
	}

	@Test
	void manualHoursMustBeBetweenZeroAndTwentyFour() {
		TeacherAttendanceService service = new TeacherAttendanceService(null, null, null);

		assertThatThrownBy(() -> service.applyManualAdjustment(
				attendanceWithoutCourse(), null, new BigDecimal("-0.01"), 3L, true))
				.hasMessage("上課時數不可小於 0");
		assertThatThrownBy(() -> service.applyManualAdjustment(
				attendanceWithoutCourse(), null, new BigDecimal("24.01"), 3L, true))
				.hasMessage("上課時數不可超過 24 小時");
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
