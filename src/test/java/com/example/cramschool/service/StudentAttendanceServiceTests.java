package com.example.cramschool.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.example.cramschool.dto.WeeklyScheduleDto;
import com.example.cramschool.entity.AttendanceStatus;
import com.example.cramschool.entity.ClassRoom;
import com.example.cramschool.entity.ScheduleType;
import com.example.cramschool.entity.Student;
import com.example.cramschool.entity.StudentAttendance;
import com.example.cramschool.form.StudentAttendanceEntryForm;
import com.example.cramschool.form.StudentAttendanceForm;
import com.example.cramschool.repository.StudentAttendanceRepository;
import com.example.cramschool.repository.StudentRepository;

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
				null, null, null, null, null, null, null, weeklyScheduleService, null);

		assertThat(service.isClassDay(classRoomId, originalDate)).isFalse();
		assertThat(service.isClassDay(classRoomId, rescheduledDate)).isTrue();
	}

	@Test
	void saveAttendanceStoresManualCheckInAndCheckOutTimes() {
		LocalDate attendanceDate = LocalDate.of(2026, 7, 3);
		ClassRoom classRoom = new ClassRoom();
		classRoom.setId(11L);
		Student student = new Student();
		student.setId(21L);
		StudentAttendanceRepository attendanceRepository = mock(StudentAttendanceRepository.class);
		StudentRepository studentRepository = mock(StudentRepository.class);
		ClassRoomService classRoomService = mock(ClassRoomService.class);
		WeeklyScheduleService weeklyScheduleService = weeklyScheduleServiceWithClassDay(classRoom.getId(), attendanceDate);
		when(classRoomService.findById(classRoom.getId())).thenReturn(classRoom);
		when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));
		when(attendanceRepository.findByClassRoomIdAndStudentIdAndAttendanceDate(
				classRoom.getId(), student.getId(), attendanceDate)).thenReturn(Optional.empty());
		when(attendanceRepository.save(any(StudentAttendance.class))).thenAnswer(invocation -> invocation.getArgument(0));

		StudentAttendanceService service = new StudentAttendanceService(
				attendanceRepository, studentRepository, classRoomService, null, null, null, null, weeklyScheduleService,
				null);
		StudentAttendanceForm form = new StudentAttendanceForm();
		form.setAttendanceDate(attendanceDate);
		StudentAttendanceEntryForm entry = new StudentAttendanceEntryForm();
		entry.setStudentId(student.getId());
		entry.setStatus(AttendanceStatus.LATE);
		entry.setCheckInTime(LocalTime.of(18, 10));
		entry.setCheckOutTime(LocalTime.of(20, 5));
		form.getEntries().add(entry);

		service.saveAttendance(classRoom.getId(), form);

		ArgumentCaptor<StudentAttendance> captor = ArgumentCaptor.forClass(StudentAttendance.class);
		org.mockito.Mockito.verify(attendanceRepository).save(captor.capture());
		StudentAttendance saved = captor.getValue();
		assertThat(saved.getCheckInTime()).isEqualTo(LocalDateTime.of(attendanceDate, LocalTime.of(18, 10)));
		assertThat(saved.getCheckOutTime()).isEqualTo(LocalDateTime.of(attendanceDate, LocalTime.of(20, 5)));
	}

	@Test
	void saveAttendanceClearsTimesForAbsentOrLeaveStatus() {
		LocalDate attendanceDate = LocalDate.of(2026, 7, 3);
		ClassRoom classRoom = new ClassRoom();
		classRoom.setId(11L);
		Student student = new Student();
		student.setId(21L);
		StudentAttendance existingAttendance = new StudentAttendance();
		existingAttendance.setCheckInTime(LocalDateTime.of(attendanceDate, LocalTime.of(18, 0)));
		existingAttendance.setCheckOutTime(LocalDateTime.of(attendanceDate, LocalTime.of(20, 0)));
		StudentAttendanceRepository attendanceRepository = mock(StudentAttendanceRepository.class);
		StudentRepository studentRepository = mock(StudentRepository.class);
		ClassRoomService classRoomService = mock(ClassRoomService.class);
		WeeklyScheduleService weeklyScheduleService = weeklyScheduleServiceWithClassDay(classRoom.getId(), attendanceDate);
		when(classRoomService.findById(classRoom.getId())).thenReturn(classRoom);
		when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));
		when(attendanceRepository.findByClassRoomIdAndStudentIdAndAttendanceDate(
				classRoom.getId(), student.getId(), attendanceDate)).thenReturn(Optional.of(existingAttendance));
		when(attendanceRepository.save(any(StudentAttendance.class))).thenAnswer(invocation -> invocation.getArgument(0));

		StudentAttendanceService service = new StudentAttendanceService(
				attendanceRepository, studentRepository, classRoomService, null, null, null, null, weeklyScheduleService,
				null);
		StudentAttendanceForm form = new StudentAttendanceForm();
		form.setAttendanceDate(attendanceDate);
		StudentAttendanceEntryForm entry = new StudentAttendanceEntryForm();
		entry.setStudentId(student.getId());
		entry.setStatus(AttendanceStatus.ABSENT);
		entry.setCheckInTime(LocalTime.of(18, 10));
		entry.setCheckOutTime(LocalTime.of(20, 5));
		form.getEntries().add(entry);

		service.saveAttendance(classRoom.getId(), form);

		ArgumentCaptor<StudentAttendance> captor = ArgumentCaptor.forClass(StudentAttendance.class);
		org.mockito.Mockito.verify(attendanceRepository).save(captor.capture());
		StudentAttendance saved = captor.getValue();
		assertThat(saved.getCheckInTime()).isNull();
		assertThat(saved.getCheckOutTime()).isNull();
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

	private WeeklyScheduleService weeklyScheduleServiceWithClassDay(Long classRoomId, LocalDate date) {
		WeeklyScheduleService weeklyScheduleService = mock(WeeklyScheduleService.class);
		when(weeklyScheduleService.findWeeklySchedules(eq(date), any(), eq(true), any(), eq(classRoomId)))
				.thenReturn(List.of(schedule(classRoomId, date, ScheduleType.NORMAL)));
		return weeklyScheduleService;
	}
}
