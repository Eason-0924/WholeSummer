package com.example.cramschool.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.cramschool.dto.TodayWorkbenchView;
import com.example.cramschool.dto.WeeklyScheduleDto;
import com.example.cramschool.entity.AttendanceStatus;
import com.example.cramschool.entity.ClassRoom;
import com.example.cramschool.entity.ClassStudent;
import com.example.cramschool.entity.Exam;
import com.example.cramschool.entity.Homework;
import com.example.cramschool.entity.ScheduleType;
import com.example.cramschool.entity.Student;
import com.example.cramschool.entity.StudentAttendance;
import com.example.cramschool.entity.Teacher;
import com.example.cramschool.repository.ExamRepository;
import com.example.cramschool.repository.HomeworkRepository;
import com.example.cramschool.repository.StudentAttendanceRepository;

class TodayWorkbenchServiceTests {

	private static final ZoneId TAIPEI = ZoneId.of("Asia/Taipei");

	@Test
	void buildIncludesTodayCoursesAttendanceNamesHomeworkAndExam() {
		LocalDate today = LocalDate.now();
		Long teacherId = 7L;
		Long classRoomId = 11L;

		WeeklyScheduleDto course = new WeeklyScheduleDto(
				101L, null, classRoomId,
				"數學", "國一數學（A班）", "王老師",
				today,
				LocalDateTime.of(today, LocalTime.of(18, 0)),
				LocalDateTime.of(today, LocalTime.of(20, 0)),
				ScheduleType.NORMAL,
				null, null,
				"1", "7", "國一");

		WeeklyScheduleService weeklyScheduleService = new WeeklyScheduleService(null, null, null, null, null) {
			@Override
			public List<WeeklyScheduleDto> findWeeklySchedules(LocalDate date, Long currentTeacherId,
					boolean director, Long teacherFilterId, Long classFilterId) {
				return List.of(course);
			}
		};

		Student presentStudent = student(1L, "小明");
		Student lateStudent = student(2L, "小華");
		Student missingStudent = student(3L, "小美");

		ClassStudentService classStudentService = new ClassStudentService(null, null, null, null) {
			@Override
			public List<ClassStudent> findActiveByClassRoomId(Long targetClassRoomId) {
				return List.of(
						classStudent(targetClassRoomId, presentStudent),
						classStudent(targetClassRoomId, lateStudent),
						classStudent(targetClassRoomId, missingStudent));
			}
		};

		StudentAttendanceRepository studentAttendanceRepository = proxy(StudentAttendanceRepository.class,
				(method, args) -> {
					if ("findByClassRoomIdAndAttendanceDateOrderByStudentChineseNameAsc".equals(method.getName())) {
						return List.of(
								attendance(classRoomId, presentStudent, today, AttendanceStatus.PRESENT,
										LocalDateTime.of(today, LocalTime.of(19, 30))),
								attendance(classRoomId, lateStudent, today, AttendanceStatus.LATE,
										LocalDateTime.of(today, LocalTime.of(19, 0))));
					}
					throw new UnsupportedOperationException(method.getName());
				});

		Homework homework = new Homework();
		homework.setId(201L);
		homework.setTitle("講義第 3 頁");
		homework.setDueDate(today);
		homework.setClassRoom(classRoom(classRoomId, "國一數學（A班）", "王老師"));
		HomeworkRepository homeworkRepository = proxy(HomeworkRepository.class, (method, args) -> {
			if ("findByDueDateBetweenOrderByDueDateAscIdAsc".equals(method.getName())) {
				return List.of(homework);
			}
			throw new UnsupportedOperationException(method.getName());
		});

		Exam exam = new Exam();
		exam.setId(301L);
		exam.setName("單元測驗一");
		exam.setExamDate(today);
		exam.setClassRoom(classRoom(classRoomId, "國一數學（A班）", "王老師"));
		ExamRepository examRepository = proxy(ExamRepository.class, (method, args) -> {
			if ("findAllByOrderByExamDateDescIdDesc".equals(method.getName())) {
				return List.of(exam);
			}
			throw new UnsupportedOperationException(method.getName());
		});

		TodayWorkbenchService service = new TodayWorkbenchService(
				weeklyScheduleService,
				classStudentService,
				studentAttendanceRepository,
				homeworkRepository,
				examRepository,
				Clock.fixed(LocalDateTime.of(today, LocalTime.of(17, 30)).atZone(TAIPEI).toInstant(), TAIPEI));

		TodayWorkbenchView view = service.build(teacherId, false);

		assertThat(view.courses()).hasSize(1);
		assertThat(view.courses().getFirst().classRoomId()).isEqualTo(classRoomId);
		assertThat(view.courses().getFirst().className()).isEqualTo("國一數學（A班）");
		assertThat(view.courses().getFirst().teacherName()).isEqualTo("王老師");

		assertThat(view.attendance().presentCount()).isEqualTo(1);
		assertThat(view.attendance().lateCount()).isEqualTo(1);
		assertThat(view.attendance().missingCount()).isEqualTo(1);
		assertThat(view.attendance().earlyLeaveCount()).isEqualTo(2);
		assertThat(view.attendance().presentNames()).containsExactly("小明");
		assertThat(view.attendance().lateNames()).containsExactly("小華");
		assertThat(view.attendance().missingNames()).containsExactly("小美");
		assertThat(view.attendance().earlyLeaveNames()).containsExactly("小明", "小華");

		assertThat(view.homeworks()).extracting(TodayWorkbenchView.TodayHomeworkItem::title)
				.containsExactly("講義第 3 頁");
		assertThat(view.exams()).extracting(TodayWorkbenchView.TodayExamItem::name)
				.containsExactly("單元測驗一");
	}

	@Test
	void buildSummarizesAttendanceForEveryTodayCourseOccurrence() {
		LocalDate today = LocalDate.of(2026, 7, 1);
		Long teacherId = 7L;
		Long classRoomId = 11L;
		Student presentStudent = student(1L, "小明");

		WeeklyScheduleDto firstCourse = new WeeklyScheduleDto(
				101L, null, classRoomId,
				"數學", "國一數學（A班）", "王老師",
				today,
				LocalDateTime.of(today, LocalTime.of(18, 0)),
				LocalDateTime.of(today, LocalTime.of(19, 0)),
				ScheduleType.NORMAL,
				null, null,
				"1", "7", "國一");
		WeeklyScheduleDto secondCourse = new WeeklyScheduleDto(
				102L, null, classRoomId,
				"數學", "國一數學（A班）", "王老師",
				today,
				LocalDateTime.of(today, LocalTime.of(20, 0)),
				LocalDateTime.of(today, LocalTime.of(21, 0)),
				ScheduleType.NORMAL,
				null, null,
				"1", "7", "國一");

		WeeklyScheduleService weeklyScheduleService = new WeeklyScheduleService(null, null, null, null, null) {
			@Override
			public List<WeeklyScheduleDto> findWeeklySchedules(LocalDate date, Long currentTeacherId,
					boolean director, Long teacherFilterId, Long classFilterId) {
				return List.of(firstCourse, secondCourse);
			}
		};
		ClassStudentService classStudentService = new ClassStudentService(null, null, null, null) {
			@Override
			public List<ClassStudent> findActiveByClassRoomId(Long targetClassRoomId) {
				return List.of(classStudent(targetClassRoomId, presentStudent));
			}
		};
		StudentAttendanceRepository studentAttendanceRepository = proxy(StudentAttendanceRepository.class,
				(method, args) -> {
					if ("findByClassRoomIdAndAttendanceDateOrderByStudentChineseNameAsc".equals(method.getName())) {
						return List.of(attendance(classRoomId, presentStudent, today, AttendanceStatus.PRESENT,
								LocalDateTime.of(today, LocalTime.of(21, 0))));
					}
					throw new UnsupportedOperationException(method.getName());
				});
		HomeworkRepository homeworkRepository = proxy(HomeworkRepository.class, (method, args) -> {
			if ("findByDueDateBetweenOrderByDueDateAscIdAsc".equals(method.getName())) {
				return List.of();
			}
			throw new UnsupportedOperationException(method.getName());
		});
		ExamRepository examRepository = proxy(ExamRepository.class, (method, args) -> {
			if ("findAllByOrderByExamDateDescIdDesc".equals(method.getName())) {
				return List.of();
			}
			throw new UnsupportedOperationException(method.getName());
		});

		TodayWorkbenchService service = new TodayWorkbenchService(
				weeklyScheduleService,
				classStudentService,
				studentAttendanceRepository,
				homeworkRepository,
				examRepository,
				Clock.fixed(LocalDateTime.of(today, LocalTime.of(17, 30)).atZone(TAIPEI).toInstant(), TAIPEI));

		TodayWorkbenchView view = service.build(teacherId, false);

		assertThat(view.courses()).hasSize(2);
		assertThat(view.courses()).extracting(TodayWorkbenchView.TodayCourseItem::startTime)
				.containsExactly(
						LocalDateTime.of(today, LocalTime.of(18, 0)),
						LocalDateTime.of(today, LocalTime.of(20, 0)));
		assertThat(view.attendance().presentCount()).isEqualTo(2);
		assertThat(view.attendance().presentNames()).containsExactly("小明", "小明");
		assertThat(view.attendance().earlyLeaveCount()).isZero();
	}

	@Test
	void absentExpectedStudentsStayMissingBeforeClassStarts() {
		LocalDate today = LocalDate.of(2026, 7, 1);
		Long classRoomId = 11L;
		Student expectedStudent = student(1L, "小明");

		TodayWorkbenchService service = serviceForAttendanceScenario(
				today,
				LocalTime.of(18, 0),
				classRoomId,
				List.of(expectedStudent),
				List.of(),
				Clock.fixed(LocalDateTime.of(today, LocalTime.of(17, 30)).atZone(TAIPEI).toInstant(), TAIPEI));

		TodayWorkbenchView view = service.build(7L, false);

		assertThat(view.attendance().missingCount()).isEqualTo(1);
		assertThat(view.attendance().missingNames()).containsExactly("小明");
		assertThat(view.attendance().lateCount()).isZero();
		assertThat(view.attendance().lateNames()).isEmpty();
	}

	@Test
	void absentExpectedStudentsBecomeLateAfterClassStarts() {
		LocalDate today = LocalDate.of(2026, 7, 1);
		Long classRoomId = 11L;
		Student expectedStudent = student(1L, "小明");

		TodayWorkbenchService service = serviceForAttendanceScenario(
				today,
				LocalTime.of(18, 0),
				classRoomId,
				List.of(expectedStudent),
				List.of(),
				Clock.fixed(LocalDateTime.of(today, LocalTime.of(18, 1)).atZone(TAIPEI).toInstant(), TAIPEI));

		TodayWorkbenchView view = service.build(7L, false);

		assertThat(view.attendance().lateCount()).isEqualTo(1);
		assertThat(view.attendance().lateNames()).containsExactly("小明");
		assertThat(view.attendance().missingCount()).isZero();
		assertThat(view.attendance().missingNames()).isEmpty();
	}

	private TodayWorkbenchService serviceForAttendanceScenario(LocalDate today, LocalTime startTime,
			Long classRoomId, List<Student> students, List<StudentAttendance> attendances, Clock clock) {
		WeeklyScheduleDto course = new WeeklyScheduleDto(
				101L, null, classRoomId,
				"數學", "國一數學（A班）", "王老師",
				today,
				LocalDateTime.of(today, startTime),
				LocalDateTime.of(today, startTime.plusHours(2)),
				ScheduleType.NORMAL,
				null, null,
				"1", "7", "國一");
		WeeklyScheduleService weeklyScheduleService = new WeeklyScheduleService(null, null, null, null, null) {
			@Override
			public List<WeeklyScheduleDto> findWeeklySchedules(LocalDate date, Long currentTeacherId,
					boolean director, Long teacherFilterId, Long classFilterId) {
				return List.of(course);
			}
		};
		ClassStudentService classStudentService = new ClassStudentService(null, null, null, null) {
			@Override
			public List<ClassStudent> findActiveByClassRoomId(Long targetClassRoomId) {
				return students.stream()
						.map(student -> classStudent(targetClassRoomId, student))
						.toList();
			}
		};
		StudentAttendanceRepository studentAttendanceRepository = proxy(StudentAttendanceRepository.class,
				(method, args) -> {
					if ("findByClassRoomIdAndAttendanceDateOrderByStudentChineseNameAsc".equals(method.getName())) {
						return attendances;
					}
					throw new UnsupportedOperationException(method.getName());
				});
		HomeworkRepository homeworkRepository = proxy(HomeworkRepository.class, (method, args) -> {
			if ("findByDueDateBetweenOrderByDueDateAscIdAsc".equals(method.getName())) {
				return List.of();
			}
			throw new UnsupportedOperationException(method.getName());
		});
		ExamRepository examRepository = proxy(ExamRepository.class, (method, args) -> {
			if ("findAllByOrderByExamDateDescIdDesc".equals(method.getName())) {
				return List.of();
			}
			throw new UnsupportedOperationException(method.getName());
		});
		return new TodayWorkbenchService(
				weeklyScheduleService,
				classStudentService,
				studentAttendanceRepository,
				homeworkRepository,
				examRepository,
				clock);
	}

	private Student student(Long id, String name) {
		Student student = new Student();
		student.setId(id);
		student.setChineseName(name);
		return student;
	}

	private ClassStudent classStudent(Long classRoomId, Student student) {
		ClassStudent classStudent = new ClassStudent();
		classStudent.setClassRoom(classRoom(classRoomId, "國一數學（A班）", "王老師"));
		classStudent.setStudent(student);
		return classStudent;
	}

	private StudentAttendance attendance(Long classRoomId, Student student, LocalDate date,
			AttendanceStatus status, LocalDateTime checkOutTime) {
		StudentAttendance attendance = new StudentAttendance();
		attendance.setClassRoom(classRoom(classRoomId, "國一數學（A班）", "王老師"));
		attendance.setStudent(student);
		attendance.setAttendanceDate(date);
		attendance.setStatus(status);
		attendance.setCheckOutTime(checkOutTime);
		return attendance;
	}

	private ClassRoom classRoom(Long id, String displayName, String teacherName) {
		ClassRoom classRoom = new ClassRoom() {
			@Override
			public String getDisplayName() {
				return displayName;
			}
		};
		classRoom.setId(id);
		Teacher teacher = new Teacher() {
			@Override
			public String getDisplayName() {
				return teacherName;
			}
		};
		teacher.setId(7L);
		classRoom.setTeacher(teacher);
		return classRoom;
	}

	@SuppressWarnings("unchecked")
	private <T> T proxy(Class<T> type, InvocationHandler handler) {
		return (T) Proxy.newProxyInstance(
				type.getClassLoader(),
				new Class<?>[] { type },
				(proxy, method, args) -> {
					if (method.getDeclaringClass() == Object.class) {
						return switch (method.getName()) {
							case "toString" -> type.getSimpleName() + "Proxy";
							case "hashCode" -> System.identityHashCode(proxy);
							case "equals" -> proxy == args[0];
							default -> null;
						};
					}
					return handler.invoke(method, args);
				});
	}

	@FunctionalInterface
	private interface InvocationHandler {
		Object invoke(java.lang.reflect.Method method, Object[] args) throws Throwable;
	}
}
