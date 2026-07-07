package com.example.cramschool.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
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
import com.example.cramschool.entity.ClassRoom;
import com.example.cramschool.entity.ClassStudent;
import com.example.cramschool.entity.ParentLineBinding;
import com.example.cramschool.entity.ScheduleType;
import com.example.cramschool.entity.Student;
import com.example.cramschool.repository.ClassStudentRepository;
import com.example.cramschool.repository.ParentLineBindingRepository;

class LineScheduleServiceTests {

	private static final Clock FIXED_CLOCK = Clock.fixed(
			Instant.parse("2026-07-06T03:00:00Z"), ZoneId.of("Asia/Taipei"));

	private final ParentLineBindingRepository bindingRepository = mock(ParentLineBindingRepository.class);
	private final ClassStudentRepository classStudentRepository = mock(ClassStudentRepository.class);
	private final WeeklyScheduleService weeklyScheduleService = mock(WeeklyScheduleService.class);
	private final LineScheduleService service = new LineScheduleService(
			bindingRepository, classStudentRepository, weeklyScheduleService, FIXED_CLOCK);

	@Test
	void buildWeeklyScheduleReplyReturnsBindingPromptWhenParentIsUnbound() {
		when(bindingRepository.findByLineUserIdAndStatusOrderByStudentChineseNameAsc(
				"line-user-1", ParentLineBinding.STATUS_BOUND)).thenReturn(List.of());

		String reply = service.buildWeeklyScheduleReply("line-user-1");

		assertThat(reply).contains("您尚未綁定學生資料")
				.contains("綁定 綁定碼")
				.contains("若尚未取得綁定碼，請洽補習班櫃台。");
	}

	@Test
	void buildWeeklyScheduleReplyFormatsSingleStudentWeeklySchedule() {
		Student student = student(1L, "王小明");
		whenBoundStudents("line-user-1", student);
		whenStudentClasses(student, 11L);
		whenWeeklySchedules(
				schedule(11L, LocalDate.of(2026, 7, 8), LocalTime.of(18, 0), LocalTime.of(20, 0),
						"數學", "A班", "陳老師", ScheduleType.NORMAL),
				schedule(99L, LocalDate.of(2026, 7, 9), LocalTime.of(19, 0), LocalTime.of(21, 0),
						"英文", "B班", "林老師", ScheduleType.NORMAL));

		String reply = service.buildWeeklyScheduleReply("line-user-1");

		assertThat(reply).contains("王小明 本週課表")
				.contains("2026/07/06（一） - 2026/07/12（日）")
				.contains("7/8（三）")
				.contains("18:00-20:00 數學 A班")
				.contains("教師：陳老師")
				.doesNotContain("英文 B班")
				.endsWith("若課表有異動，請以補習班最新通知為準。");
	}

	@Test
	void buildWeeklyScheduleReplyFormatsMultipleStudentsWeeklySchedules() {
		Student first = student(1L, "王小明");
		Student second = student(2L, "王小華");
		whenBoundStudents("line-user-1", first, second);
		whenStudentClasses(first, 11L);
		whenStudentClasses(second, 22L);
		whenWeeklySchedules(
				schedule(11L, LocalDate.of(2026, 7, 8), LocalTime.of(18, 0), LocalTime.of(20, 0),
						"數學", "A班", "陳老師", ScheduleType.NORMAL),
				schedule(22L, LocalDate.of(2026, 7, 9), LocalTime.of(18, 30), LocalTime.of(20, 30),
						"國文", "C班", "王老師", ScheduleType.NORMAL));

		String reply = service.buildWeeklyScheduleReply("line-user-1");

		assertThat(reply).contains("您已綁定 2 位學生，以下為本週課表")
				.contains("【王小明】")
				.contains("7/8（三） 18:00-20:00 數學 A班")
				.contains("【王小華】")
				.contains("7/9（四） 18:30-20:30 國文 C班");
	}

	@Test
	void buildWeeklyScheduleReplyReturnsNoScheduleMessageForSingleStudentWithoutSchedules() {
		Student student = student(1L, "王小明");
		whenBoundStudents("line-user-1", student);
		whenStudentClasses(student, 11L);
		whenWeeklySchedules();

		String reply = service.buildWeeklyScheduleReply("line-user-1");

		assertThat(reply).isEqualTo("王小明本週目前沒有排定課程。\n若您認為資料有誤，請洽補習班櫃台。");
	}

	@Test
	void buildWeeklyScheduleReplyMarksMakeUpSchedule() {
		Student student = student(1L, "王小明");
		whenBoundStudents("line-user-1", student);
		whenStudentClasses(student, 11L);
		whenWeeklySchedules(schedule(11L, LocalDate.of(2026, 7, 11), LocalTime.of(10, 0), LocalTime.of(12, 0),
				"英文", "B班", "林老師", ScheduleType.MAKE_UP));

		String reply = service.buildWeeklyScheduleReply("line-user-1");

		assertThat(reply).contains("10:00-12:00 英文 B班")
				.contains("類型：補課");
	}

	@Test
	void buildWeeklyScheduleReplyMarksRescheduledSchedule() {
		Student student = student(1L, "王小明");
		whenBoundStudents("line-user-1", student);
		whenStudentClasses(student, 11L);
		whenWeeklySchedules(schedule(11L, LocalDate.of(2026, 7, 12), LocalTime.of(14, 0), LocalTime.of(16, 0),
				"理化", "C班", "王老師", ScheduleType.RESCHEDULED));

		String reply = service.buildWeeklyScheduleReply("line-user-1");

		assertThat(reply).contains("14:00-16:00 理化 C班")
				.contains("類型：調課");
	}

	@Test
	void buildWeeklyScheduleReplyExcludesCancelledSchedule() {
		Student student = student(1L, "王小明");
		whenBoundStudents("line-user-1", student);
		whenStudentClasses(student, 11L);
		whenWeeklySchedules(schedule(11L, LocalDate.of(2026, 7, 10), LocalTime.of(19, 0), LocalTime.of(21, 0),
				"英文", "B班", "林老師", ScheduleType.CANCELLED));

		String reply = service.buildWeeklyScheduleReply("line-user-1");

		assertThat(reply).contains("本週目前沒有排定課程")
				.doesNotContain("英文 B班")
				.doesNotContain("已取消");
	}

	private void whenBoundStudents(String lineUserId, Student... students) {
		when(bindingRepository.findByLineUserIdAndStatusOrderByStudentChineseNameAsc(
				lineUserId, ParentLineBinding.STATUS_BOUND))
				.thenReturn(List.of(students).stream()
						.map(this::binding)
						.toList());
	}

	private ParentLineBinding binding(Student student) {
		ParentLineBinding binding = new ParentLineBinding();
		binding.setStudent(student);
		binding.setLineUserId("line-user-1");
		return binding;
	}

	private void whenStudentClasses(Student student, Long... classRoomIds) {
		when(classStudentRepository.findByStudentIdAndActiveTrue(student.getId()))
				.thenReturn(List.of(classRoomIds).stream()
						.map(this::classStudent)
						.toList());
	}

	private ClassStudent classStudent(Long classRoomId) {
		ClassRoom classRoom = new ClassRoom();
		classRoom.setId(classRoomId);
		ClassStudent classStudent = new ClassStudent();
		classStudent.setClassRoom(classRoom);
		return classStudent;
	}

	private void whenWeeklySchedules(WeeklyScheduleDto... schedules) {
		when(weeklyScheduleService.findWeeklySchedules(LocalDate.of(2026, 7, 6), null, true, null, null))
				.thenReturn(List.of(schedules));
	}

	private Student student(Long id, String name) {
		Student student = new Student();
		student.setId(id);
		student.setChineseName(name);
		return student;
	}

	private WeeklyScheduleDto schedule(Long classRoomId, LocalDate date, LocalTime start, LocalTime end,
			String courseName, String className, String teacherName, ScheduleType scheduleType) {
		return new WeeklyScheduleDto(
				classRoomId * 10,
				null,
				classRoomId,
				courseName,
				className,
				teacherName,
				date,
				LocalDateTime.of(date, start),
				LocalDateTime.of(date, end),
				scheduleType,
				null,
				null,
				courseName,
				teacherName,
				className);
	}
}
