package com.example.cramschool.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import com.example.cramschool.dto.TeacherDailySchedule;
import com.example.cramschool.dto.TeacherDailySchedule.TimeRange;
import com.example.cramschool.dto.TeacherScheduleMatch;
import com.example.cramschool.entity.Teacher;
import com.example.cramschool.entity.TeacherAttendance;
import com.example.cramschool.entity.TeacherAttendanceStatus;
import com.example.cramschool.form.TeacherAttendanceForm;
import com.example.cramschool.repository.TeacherAttendanceRepository;
import com.example.cramschool.repository.TeacherRepository;

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
	void monthlySalaryLookupKeepsStoredMatchedCourseMinutes() {
		TeacherAttendance attendance = attendanceWithoutCourse();
		attendance.setMatchedCourseId(5L);
		attendance.setMatchedCourseTimeText("18:00 ~ 20:00");
		attendance.setWorkMinutes(120L);
		TeacherAttendanceRepository repository = repositoryReturning(attendance);
		TeacherScheduleService changedScheduleService = new TeacherScheduleService(null) {
			@Override
			public TeacherDailySchedule findDailySchedule(Long teacherId, LocalDate date) {
				return TeacherDailySchedule.empty();
			}

			@Override
			public com.example.cramschool.dto.TeacherScheduleMatch findMatchedSchedule(
					Long teacherId, LocalDate date, LocalTime clockInTime, LocalTime clockOutTime) {
				throw new AssertionError("已配對課程的打卡紀錄不應因課程時間異動而重新套用課表");
			}
		};
		TeacherAttendanceService service = new TeacherAttendanceService(
				repository, null, changedScheduleService);

		List<TeacherAttendance> attendances = service.findByTeacherIdAndMonth(8L, YearMonth.of(2026, 6));

		assertThat(attendances).singleElement()
				.satisfies(record -> assertThat(record.getWorkMinutes()).isEqualTo(120));
	}

	@Test
	void manualSaveRemovesAbsenceMakeUpWhenAbsentRecordBecomesWorking() {
		Teacher teacher = new Teacher();
		teacher.setId(8L);
		teacher.setName("王老師");
		TeacherAttendance existingAbsent = new TeacherAttendance();
		existingAbsent.setId(188L);
		existingAbsent.setTeacher(teacher);
		existingAbsent.setDate(LocalDate.of(2026, 6, 30));
		existingAbsent.setStatus(TeacherAttendanceStatus.ABSENT);
		TeacherAttendanceRepository attendanceRepository = (TeacherAttendanceRepository) Proxy.newProxyInstance(
				TeacherAttendanceRepository.class.getClassLoader(),
				new Class<?>[] { TeacherAttendanceRepository.class },
				(proxy, method, args) -> {
					if (method.getDeclaringClass() == Object.class) {
						return switch (method.getName()) {
							case "toString" -> "TeacherAttendanceRepositoryTestProxy";
							case "hashCode" -> System.identityHashCode(proxy);
							case "equals" -> proxy == args[0];
							default -> null;
						};
					}
					if ("findByTeacherIdAndDate".equals(method.getName())) {
						return Optional.of(existingAbsent);
					}
					if ("save".equals(method.getName())) {
						return args[0];
					}
					throw new UnsupportedOperationException(method.getName());
				});
		TeacherRepository teacherRepository = (TeacherRepository) Proxy.newProxyInstance(
				TeacherRepository.class.getClassLoader(),
				new Class<?>[] { TeacherRepository.class },
				(proxy, method, args) -> {
					if (method.getDeclaringClass() == Object.class) {
						return switch (method.getName()) {
							case "toString" -> "TeacherRepositoryTestProxy";
							case "hashCode" -> System.identityHashCode(proxy);
							case "equals" -> proxy == args[0];
							default -> null;
						};
					}
					if ("findById".equals(method.getName())) {
						return Optional.of(teacher);
					}
					throw new UnsupportedOperationException(method.getName());
				});
		TeacherScheduleService scheduleService = new TeacherScheduleService(null) {
			@Override
			public TeacherScheduleMatch findMatchedSchedule(
					Long teacherId, LocalDate date, LocalTime clockInTime, LocalTime clockOutTime) {
				return new TeacherScheduleMatch(5L, "高一數學", "19:00 ~ 21:00", 120, LocalTime.of(19, 0));
			}
		};
		AtomicLong deletedAttendanceId = new AtomicLong();
		MakeUpClassService makeUpClassService = new MakeUpClassService(null, null, null, null) {
			@Override
			public void deleteAbsenceMakeUpForAttendance(Long attendanceId) {
				deletedAttendanceId.set(attendanceId);
			}
		};
		TeacherAttendanceService service = new TeacherAttendanceService(
				attendanceRepository, teacherRepository, scheduleService, makeUpClassService);
		TeacherAttendanceForm form = new TeacherAttendanceForm();
		form.setTeacherId(teacher.getId());
		form.setDate(existingAbsent.getDate());
		form.setClockInTime(LocalTime.of(18, 55));
		form.setClockOutTime(LocalTime.of(21, 0));
		form.setStatus(TeacherAttendanceStatus.WORKING);

		TeacherAttendance saved = service.save(form);

		assertThat(saved.getStatus()).isEqualTo(TeacherAttendanceStatus.WORKING);
		assertThat(deletedAttendanceId.get()).isEqualTo(188L);
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

	@SuppressWarnings("unchecked")
	private TeacherAttendanceRepository repositoryReturning(TeacherAttendance attendance) {
		return (TeacherAttendanceRepository) Proxy.newProxyInstance(
				TeacherAttendanceRepository.class.getClassLoader(),
				new Class<?>[] { TeacherAttendanceRepository.class },
				(proxy, method, args) -> {
					if (method.getDeclaringClass() == Object.class) {
						return switch (method.getName()) {
							case "toString" -> "TeacherAttendanceRepositoryTestProxy";
							case "hashCode" -> System.identityHashCode(proxy);
							case "equals" -> proxy == args[0];
							default -> null;
						};
					}
					if ("findByTeacherIdAndDateBetweenOrderByDateAsc".equals(method.getName())) {
						return List.of(attendance);
					}
					if ("saveAll".equals(method.getName())) {
						return args[0];
					}
					throw new UnsupportedOperationException(method.getName());
				});
	}
}
