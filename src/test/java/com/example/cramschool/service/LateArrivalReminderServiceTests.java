package com.example.cramschool.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.cramschool.dto.WeeklyScheduleDto;
import com.example.cramschool.entity.ClassStudent;
import com.example.cramschool.entity.ScheduleType;
import com.example.cramschool.entity.Student;
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
		when(attendanceRepository.existsByClassRoomIdAndStudentIdAndAttendanceDate(11L, 21L, date))
				.thenReturn(false);
		when(attendanceRepository.existsByClassRoomIdAndStudentIdAndAttendanceDate(11L, 22L, date))
				.thenReturn(true);
		LateArrivalReminderService service = new LateArrivalReminderService(
				weeklyScheduleService, classStudentRepository, attendanceRepository, lineNotificationService,
				classRoomRepository, webPushEventNotificationService);
		setNow(service, LocalDateTime.of(date, LocalTime.of(18, 6)));

		service.sendDueLateArrivalReminders();

		verify(lineNotificationService).sendLateArrivalReminder(eq(missingStudent), eq(20639000000101L),
				eq("國一數學"), eq(LocalDateTime.of(date, LocalTime.of(18, 0))));
		verify(lineNotificationService, never()).sendLateArrivalReminder(eq(arrivedStudent), eq(20639000000101L),
				eq("國一數學"), eq(LocalDateTime.of(date, LocalTime.of(18, 0))));
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
