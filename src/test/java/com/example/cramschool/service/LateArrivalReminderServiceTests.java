package com.example.cramschool.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import com.example.cramschool.dto.WeeklyScheduleDto;
import com.example.cramschool.entity.ClassStudent;
import com.example.cramschool.entity.ScheduleType;
import com.example.cramschool.entity.Student;
import com.example.cramschool.entity.StudentAttendance;
import com.example.cramschool.repository.ClassStudentRepository;
import com.example.cramschool.repository.ClassRoomRepository;
import com.example.cramschool.repository.StudentAttendanceRepository;

class LateArrivalReminderServiceTests {

	private static final ZoneId TAIPEI = ZoneId.of("Asia/Taipei");

	@Test
	void sendsReminderAfterFiveMinutesOnlyForStudentsWithoutAttendance() {
		LocalDate date = LocalDate.of(2026, 7, 5);
		WeeklyScheduleDto schedule = schedule(date);
		Student missingStudent = student(21L, "尚未到班");
		Student arrivedStudent = student(22L, "已到班");
		WeeklyScheduleService weeklyScheduleService = mock(WeeklyScheduleService.class);
		ClassStudentRepository classStudentRepository = mock(ClassStudentRepository.class);
		StudentAttendanceRepository attendanceRepository = mock(StudentAttendanceRepository.class);
		LineNotificationService lineNotificationService = mock(LineNotificationService.class);
		ClassRoomRepository classRoomRepository = mock(ClassRoomRepository.class);
		WebPushEventNotificationService webPushEventNotificationService = mock(WebPushEventNotificationService.class);
		when(lineNotificationService.isLineEnabled()).thenReturn(true);
		when(weeklyScheduleService.findWeeklySchedules(eq(date), eq(null), eq(true), eq(null), eq(null)))
				.thenReturn(List.of(schedule));
		when(classStudentRepository.findByClassRoomIdAndActiveTrueOrderByStudentChineseNameAsc(11L))
				.thenReturn(List.of(classStudent(missingStudent), classStudent(arrivedStudent)));
			when(attendanceRepository.existsByStudentIdAndAttendanceDateAndCheckInTimeIsNotNullAndCheckOutTimeIsNull(21L, date))
					.thenReturn(false);
			when(attendanceRepository.existsByStudentIdAndAttendanceDateAndCheckInTimeIsNotNullAndCheckOutTimeIsNull(22L, date))
					.thenReturn(true);
		LateArrivalReminderService service = new LateArrivalReminderService(
				weeklyScheduleService, classStudentRepository, attendanceRepository, lineNotificationService,
				classRoomRepository, webPushEventNotificationService);
		setNow(service, LocalDateTime.of(date, LocalTime.of(18, 6)));

		service.sendDueLateArrivalReminders();

		verify(lineNotificationService).sendLateArrivalReminders(argThat(reminders -> reminders.size() == 1
				&& reminders.get(0).student() == missingStudent
				&& reminders.get(0).referenceId().equals(20639000000101L)
				&& reminders.get(0).className().equals("國一數學")));
		verify(lineNotificationService, never()).sendLateArrivalReminder(eq(arrivedStudent), eq(20639000000101L),
				eq("國一數學"), eq(LocalDateTime.of(date, LocalTime.of(18, 0))));
	}

	@Test
	void doesNotSendReminderWhenStudentArrivesWithinFiveMinutes() {
		LocalDate date = LocalDate.of(2026, 7, 5);
		WeeklyScheduleService weeklyScheduleService = mock(WeeklyScheduleService.class);
		LineNotificationService lineNotificationService = mock(LineNotificationService.class);
		WebPushEventNotificationService webPushEventNotificationService = mock(WebPushEventNotificationService.class);
		when(weeklyScheduleService.findWeeklySchedules(eq(date), eq(null), eq(true), eq(null), eq(null)))
				.thenReturn(List.of(schedule(date)));
		LateArrivalReminderService service = new LateArrivalReminderService(
				weeklyScheduleService, mock(ClassStudentRepository.class), mock(StudentAttendanceRepository.class),
				lineNotificationService, mock(ClassRoomRepository.class), webPushEventNotificationService);
		setNow(service, LocalDateTime.of(date, LocalTime.of(18, 5)));

		service.sendDueLateArrivalReminders();

		verifyNoInteractions(lineNotificationService, webPushEventNotificationService);
	}

	@Test
	void doesNotSendLateReminderWhenStudentIsAlreadyOnCampusFromAnotherClassThatDay() {
		LocalDate date = LocalDate.of(2026, 7, 5);
		WeeklyScheduleService weeklyScheduleService = mock(WeeklyScheduleService.class);
		ClassStudentRepository classStudentRepository = mock(ClassStudentRepository.class);
		StudentAttendanceRepository attendanceRepository = mock(StudentAttendanceRepository.class);
		LineNotificationService lineNotificationService = mock(LineNotificationService.class);
		WebPushEventNotificationService webPushEventNotificationService = mock(WebPushEventNotificationService.class);
		Student student = student(21L, "下午已到班");
		when(lineNotificationService.isLineEnabled()).thenReturn(true);
		when(weeklyScheduleService.findWeeklySchedules(eq(date), eq(null), eq(true), eq(null), eq(null)))
				.thenReturn(List.of(schedule(date)));
		when(classStudentRepository.findByClassRoomIdAndActiveTrueOrderByStudentChineseNameAsc(11L))
				.thenReturn(List.of(classStudent(student)));
			when(attendanceRepository.existsByStudentIdAndAttendanceDateAndCheckInTimeIsNotNullAndCheckOutTimeIsNull(21L, date)).thenReturn(false);
		when(attendanceRepository.existsByStudentIdAndAttendanceDateAndCheckInTimeIsNotNullAndCheckOutTimeIsNull(21L, date))
				.thenReturn(true);
		LateArrivalReminderService service = new LateArrivalReminderService(
				weeklyScheduleService, classStudentRepository, attendanceRepository, lineNotificationService,
				mock(ClassRoomRepository.class), webPushEventNotificationService);
		setNow(service, LocalDateTime.of(date, LocalTime.of(18, 6)));

		service.sendDueLateArrivalReminders();

		verifyNoInteractions(webPushEventNotificationService);
		verify(lineNotificationService, never()).sendLateArrivalReminders(org.mockito.ArgumentMatchers.anyList());
		verify(attendanceRepository).existsByStudentIdAndAttendanceDateAndCheckInTimeIsNotNullAndCheckOutTimeIsNull(21L, date);
	}

	@Test
	void doesNotSendLateReminderWhenStudentWasPresentAtThisScheduleStartButCheckedOutDuringClass() {
		LocalDate date = LocalDate.of(2026, 7, 5);
		WeeklyScheduleService weeklyScheduleService = mock(WeeklyScheduleService.class);
		ClassStudentRepository classStudentRepository = mock(ClassStudentRepository.class);
		StudentAttendanceRepository attendanceRepository = mock(StudentAttendanceRepository.class);
		LineNotificationService lineNotificationService = mock(LineNotificationService.class);
		WebPushEventNotificationService webPushEventNotificationService = mock(WebPushEventNotificationService.class);
		Student student = student(21L, "已於上課中離校");
		StudentAttendance attendance = new StudentAttendance();
		attendance.setStudent(student);
		attendance.setCheckInTime(LocalDateTime.of(date, LocalTime.of(16, 54)));
		attendance.setCheckOutTime(LocalDateTime.of(date, LocalTime.of(19, 32)));
		when(weeklyScheduleService.findWeeklySchedules(eq(date), eq(null), eq(true), eq(null), eq(null)))
				.thenReturn(List.of(schedule(date)));
		when(classStudentRepository.findByClassRoomIdAndActiveTrueOrderByStudentChineseNameAsc(11L))
				.thenReturn(List.of(classStudent(student)));
		when(attendanceRepository.findByStudentIdAndAttendanceDateOrderByIdDesc(21L, date))
				.thenReturn(List.of(attendance));
		LateArrivalReminderService service = new LateArrivalReminderService(
				weeklyScheduleService, classStudentRepository, attendanceRepository, lineNotificationService,
				mock(ClassRoomRepository.class), webPushEventNotificationService);
		setNow(service, LocalDateTime.of(date, LocalTime.of(19, 33)));

		service.sendDueLateArrivalReminders();

		verifyNoInteractions(lineNotificationService, webPushEventNotificationService);
	}

	@Test
	void sendsOneBrowserNotificationWhenTheSameScheduleIsReturnedTwice() {
		LocalDate date = LocalDate.of(2026, 7, 5);
		WeeklyScheduleDto schedule = schedule(date);
		Student student = student(21L, "尚未到班");
		WeeklyScheduleService weeklyScheduleService = mock(WeeklyScheduleService.class);
		ClassStudentRepository classStudentRepository = mock(ClassStudentRepository.class);
		StudentAttendanceRepository attendanceRepository = mock(StudentAttendanceRepository.class);
		LineNotificationService lineNotificationService = mock(LineNotificationService.class);
		WebPushEventNotificationService webPushEventNotificationService = mock(WebPushEventNotificationService.class);
		when(lineNotificationService.isLineEnabled()).thenReturn(true);
		when(weeklyScheduleService.findWeeklySchedules(eq(date), eq(null), eq(true), eq(null), eq(null)))
				.thenReturn(List.of(schedule, schedule));
		when(classStudentRepository.findByClassRoomIdAndActiveTrueOrderByStudentChineseNameAsc(11L))
				.thenReturn(List.of(classStudent(student)));
		when(attendanceRepository.existsByClassRoomIdAndStudentIdAndAttendanceDate(11L, 21L, date)).thenReturn(false);
		LateArrivalReminderService service = new LateArrivalReminderService(
				weeklyScheduleService, classStudentRepository, attendanceRepository, lineNotificationService,
				mock(ClassRoomRepository.class), webPushEventNotificationService);
		setNow(service, LocalDateTime.of(date, LocalTime.of(18, 6)));

		service.sendDueLateArrivalWebPushNotifications();

		verifyNoInteractions(lineNotificationService);
		verify(webPushEventNotificationService, times(1)).notifyLateArrival(eq("尚未到班"), eq("國一數學"), eq(null));
	}

	@Test
	void scansLineRemindersOnTheMinuteAndWebPushEveryTenMinutesOnTheMinute() throws NoSuchMethodException {
		Scheduled scheduled = LateArrivalReminderService.class
				.getMethod("sendDueLateArrivalReminders")
				.getAnnotation(Scheduled.class);
		Scheduled webPushScheduled = LateArrivalReminderService.class
				.getMethod("sendDueLateArrivalWebPushNotifications")
				.getAnnotation(Scheduled.class);

		assertThat(scheduled.cron()).isEqualTo("${line.late-reminder.cron:0 * * * * *}");
		assertThat(webPushScheduled.cron()).isEqualTo("${webpush.late-arrival.cron:0 */10 * * * *}");
	}

	private void setNow(LateArrivalReminderService service, LocalDateTime now) {
		Instant instant = now.atZone(TAIPEI).toInstant();
		service.setClock(Clock.fixed(instant, TAIPEI));
	}

	private WeeklyScheduleDto schedule(LocalDate date) {
		return new WeeklyScheduleDto(
				101L,
				null,
				11L,
				"數學",
				"國一數學",
				"王老師",
				date,
				LocalDateTime.of(date, LocalTime.of(18, 0)),
				LocalDateTime.of(date, LocalTime.of(20, 0)),
				ScheduleType.NORMAL,
				null,
				null,
				"數學",
				"王老師",
				"國一");
	}

	private ClassStudent classStudent(Student student) {
		ClassStudent classStudent = new ClassStudent();
		classStudent.setStudent(student);
		return classStudent;
	}

	private Student student(Long id, String name) {
		Student student = new Student();
		student.setId(id);
		student.setChineseName(name);
		student.setActive(true);
		return student;
	}
}
