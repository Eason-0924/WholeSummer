package com.example.cramschool.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.cramschool.dto.WeeklyScheduleDto;
import com.example.cramschool.entity.ClassRoom;
import com.example.cramschool.entity.ClassSchedule;
import com.example.cramschool.entity.MakeUpClassRequest;
import com.example.cramschool.entity.MakeUpSourceType;
import com.example.cramschool.entity.MakeUpStatus;
import com.example.cramschool.entity.ScheduleType;
import com.example.cramschool.entity.Subject;
import com.example.cramschool.entity.Teacher;
import com.example.cramschool.repository.ClassRoomRepository;
import com.example.cramschool.repository.ClassScheduleRepository;
import com.example.cramschool.repository.MakeUpClassRequestRepository;

class WeeklyScheduleServiceTests {

	@Test
	void findWeeklySchedulesIncludesTodayNormalCourseWithClassRoomId() {
		LocalDate wednesday = LocalDate.of(2026, 7, 1);
		ClassRoom classRoom = classRoom(11L, 7L, "國一", "數學", "李老師");
		classRoom.addSchedule(schedule(101L, "星期三", LocalTime.of(18, 0), LocalTime.of(20, 0),
				ScheduleType.NORMAL, null, null));

		WeeklyScheduleService service = service(List.of(classRoom), List.of());

		List<WeeklyScheduleDto> schedules = service.findWeeklySchedules(wednesday, 7L, true, null, null);

		assertThat(schedules).singleElement()
				.satisfies(schedule -> {
					assertThat(schedule.getClassRoomId()).isEqualTo(11L);
					assertThat(schedule.getClassName()).isEqualTo("國一數學");
					assertThat(schedule.getTeacherName()).isEqualTo("李老師");
					assertThat(schedule.getCourseDate()).isEqualTo(wednesday);
					assertThat(schedule.getStartTime()).isEqualTo(LocalDateTime.of(wednesday, LocalTime.of(18, 0)));
					assertThat(schedule.getEndTime()).isEqualTo(LocalDateTime.of(wednesday, LocalTime.of(20, 0)));
				});
	}

	@Test
	void findWeeklySchedulesKeepsClassRoomIdForEventCourse() {
		LocalDate wednesday = LocalDate.of(2026, 7, 1);
		ClassRoom classRoom = classRoom(11L, 7L, "國一", "數學", "李老師");
		ClassSchedule makeUp = schedule(201L, "星期三", LocalTime.of(18, 0), LocalTime.of(20, 0),
				ScheduleType.MAKE_UP,
				wednesday,
				LocalDateTime.of(wednesday, LocalTime.of(18, 0)));
		classRoom.addSchedule(makeUp);

		WeeklyScheduleService service = service(List.of(), List.of(makeUp));

		List<WeeklyScheduleDto> schedules = service.findWeeklySchedules(wednesday, 7L, true, null, null);

		assertThat(schedules).singleElement()
				.satisfies(schedule -> {
					assertThat(schedule.getClassRoomId()).isEqualTo(11L);
					assertThat(schedule.getScheduleType()).isEqualTo(ScheduleType.MAKE_UP);
					assertThat(schedule.getCourseDate()).isEqualTo(wednesday);
				});
	}

	@Test
	void blockedOriginalRequestsAreDeduplicatedAndIgnoreNonActualEventDates() {
		LocalDate tuesday = LocalDate.of(2026, 6, 30);
		ClassRoom classRoom = classRoom(11L, 7L, "高一", "數學", "李老師");
		ClassSchedule original = schedule(101L, "星期二", LocalTime.of(19, 0), LocalTime.of(21, 0),
				ScheduleType.NORMAL, null, null);
		classRoom.addSchedule(original);
		MakeUpClassRequest rescheduleRequest = request(
				201L, original, classRoom, MakeUpSourceType.RESCHEDULE, tuesday, MakeUpStatus.SCHEDULED);
		MakeUpClassRequest absenceRequest = request(
				202L, original, classRoom, MakeUpSourceType.ABSENCE, tuesday, MakeUpStatus.PENDING);
		ClassSchedule makeUpEvent = schedule(301L, "星期三", LocalTime.of(18, 0), LocalTime.of(20, 0),
				ScheduleType.MAKE_UP, LocalDate.of(2026, 7, 8),
				LocalDateTime.of(2026, 7, 8, 18, 0));
		makeUpEvent.setOriginalSchedule(original);
		makeUpEvent.setClassRoom(classRoom);
		MakeUpClassRequest wrongEventDateAbsence = request(
				203L, makeUpEvent, classRoom, MakeUpSourceType.ABSENCE,
				LocalDate.of(2026, 7, 1), MakeUpStatus.PENDING);

		WeeklyScheduleService service = service(
				List.of(classRoom), List.of(), List.of(rescheduleRequest, absenceRequest, wrongEventDateAbsence));

		List<WeeklyScheduleDto> schedules = service.findWeeklySchedules(tuesday, 7L, true, null, null);

		assertThat(schedules)
				.filteredOn(schedule -> schedule.getScheduleType() == ScheduleType.CANCELLED)
				.singleElement()
				.satisfies(schedule -> {
					assertThat(schedule.getScheduleId()).isEqualTo(original.getId());
					assertThat(schedule.getCourseDate()).isEqualTo(tuesday);
					assertThat(schedule.getNote()).isEqualTo("調課");
				});
	}

	private WeeklyScheduleService service(List<ClassRoom> activeClassRooms, List<ClassSchedule> eventSchedules) {
		return service(activeClassRooms, eventSchedules, List.of());
	}

	private WeeklyScheduleService service(List<ClassRoom> activeClassRooms, List<ClassSchedule> eventSchedules,
			List<MakeUpClassRequest> requests) {
		ClassScheduleRepository classScheduleRepository = proxy(ClassScheduleRepository.class, (method, args) -> {
			if ("findByScheduledStartAtBetweenOrderByScheduledStartAtAsc".equals(method.getName())
					|| "findByClassRoomTeacherIdAndScheduledStartAtBetweenOrderByScheduledStartAtAsc".equals(method.getName())) {
				return eventSchedules;
			}
			throw new UnsupportedOperationException(method.getName());
		});
		ClassRoomRepository classRoomRepository = proxy(ClassRoomRepository.class, (method, args) -> {
			if ("findByActiveTrue".equals(method.getName()) || "findByTeacherIdAndActiveTrue".equals(method.getName())) {
				return activeClassRooms;
			}
			throw new UnsupportedOperationException(method.getName());
		});
		MakeUpClassRequestRepository makeUpClassRequestRepository = proxy(MakeUpClassRequestRepository.class,
				(method, args) -> {
					if (method.getName().startsWith("findByStatus")
							|| method.getName().startsWith("findByTeacherIdAndStatus")) {
						return requests.stream()
								.filter(request -> request.getStatus() == args[0])
								.toList();
					}
					throw new UnsupportedOperationException(method.getName());
				});
		return new WeeklyScheduleService(
				classScheduleRepository,
				classRoomRepository,
				makeUpClassRequestRepository,
				null,
				null);
	}

	private ClassRoom classRoom(Long id, Long teacherId, String grade, String subjectName, String teacherName) {
		Subject subject = new Subject();
		subject.setId(3L);
		subject.setName(subjectName);

		Teacher teacher = new Teacher();
		teacher.setId(teacherId);
		teacher.setName(teacherName);

		ClassRoom classRoom = new ClassRoom();
		classRoom.setId(id);
		classRoom.setGrade(grade);
		classRoom.setSubject(subject);
		classRoom.setTeacher(teacher);
		return classRoom;
	}

	private ClassSchedule schedule(Long id, String weekday, LocalTime startTime, LocalTime endTime,
			ScheduleType scheduleType, LocalDate courseDate, LocalDateTime scheduledStartAt) {
		ClassSchedule schedule = new ClassSchedule(weekday, startTime, endTime);
		schedule.setId(id);
		schedule.setScheduleType(scheduleType);
		schedule.setCourseDate(courseDate);
		schedule.setScheduledStartAt(scheduledStartAt);
		schedule.setScheduledEndAt(scheduledStartAt == null ? null : scheduledStartAt.plusHours(2));
		return schedule;
	}

	private MakeUpClassRequest request(Long id, ClassSchedule originalSchedule, ClassRoom classRoom,
			MakeUpSourceType sourceType, LocalDate originalDate, MakeUpStatus status) {
		MakeUpClassRequest request = new MakeUpClassRequest();
		request.setId(id);
		request.setOriginalCourseSchedule(originalSchedule);
		request.setClassRoom(classRoom);
		request.setTeacher(classRoom.getTeacher());
		request.setSourceType(sourceType);
		request.setOriginalCourseDate(originalDate);
		request.setStatus(status);
		request.setNote(sourceType == MakeUpSourceType.RESCHEDULE ? "調課" : "系統自動記錄缺勤");
		return request;
	}

	@SuppressWarnings("unchecked")
	private <T> T proxy(Class<T> type, MethodHandler handler) {
		return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, (proxy, method, args) -> {
			if (method.getDeclaringClass() == Object.class) {
				return switch (method.getName()) {
					case "toString" -> type.getSimpleName() + " proxy";
					case "hashCode" -> System.identityHashCode(proxy);
					case "equals" -> proxy == args[0];
					default -> null;
				};
			}
			return handler.invoke(method, args);
		});
	}

	@FunctionalInterface
	private interface MethodHandler {
		Object invoke(Method method, Object[] args) throws Throwable;
	}
}
