package com.example.cramschool.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import com.example.cramschool.dto.MakeUpRecordView;
import com.example.cramschool.entity.ClassRoom;
import com.example.cramschool.entity.ClassSchedule;
import com.example.cramschool.entity.MakeUpClassRequest;
import com.example.cramschool.entity.MakeUpSourceType;
import com.example.cramschool.entity.MakeUpStatus;
import com.example.cramschool.entity.ScheduleType;
import com.example.cramschool.entity.Subject;
import com.example.cramschool.entity.Teacher;
import com.example.cramschool.entity.TeacherAttendance;
import com.example.cramschool.repository.ClassScheduleRepository;
import com.example.cramschool.repository.MakeUpClassRequestRepository;

class MakeUpClassServiceTests {

	@Test
	void findScheduledRecordsIncludesScheduledMakeUpsAndDirectReschedules() {
		Teacher teacher = teacher(7L, "王老師");
		ClassRoom makeUpClass = classRoom(11L, teacher, "國一", "數學", "A班");
		ClassSchedule originalMakeUpSchedule = schedule(101L, makeUpClass, "星期一",
				LocalTime.of(18, 0), LocalTime.of(20, 0), ScheduleType.NORMAL);
		MakeUpClassRequest scheduledMakeUp = new MakeUpClassRequest();
		scheduledMakeUp.setId(201L);
		scheduledMakeUp.setClassRoom(makeUpClass);
		scheduledMakeUp.setTeacher(teacher);
		scheduledMakeUp.setOriginalCourseSchedule(originalMakeUpSchedule);
		scheduledMakeUp.setOriginalCourseDate(LocalDate.of(2026, 7, 6));
		scheduledMakeUp.setSourceType(MakeUpSourceType.LEAVE);
		scheduledMakeUp.setStatus(MakeUpStatus.SCHEDULED);
		scheduledMakeUp.setSelectedMakeUpStart(LocalDateTime.of(2026, 7, 8, 18, 0));
		scheduledMakeUp.setSelectedMakeUpEnd(LocalDateTime.of(2026, 7, 8, 20, 0));

		ClassRoom rescheduledClass = classRoom(12L, teacher, "國二", "英文", "B班");
		ClassSchedule originalReschedule = schedule(301L, rescheduledClass, "星期二",
				LocalTime.of(19, 0), LocalTime.of(21, 0), ScheduleType.NORMAL);
		ClassSchedule directReschedule = schedule(302L, rescheduledClass, "星期四",
				LocalTime.of(17, 0), LocalTime.of(19, 0), ScheduleType.RESCHEDULED);
		directReschedule.setOriginalSchedule(originalReschedule);
		directReschedule.setCourseDate(LocalDate.of(2026, 7, 9));
		directReschedule.setScheduledStartAt(LocalDateTime.of(2026, 7, 9, 17, 0));
		directReschedule.setScheduledEndAt(LocalDateTime.of(2026, 7, 9, 19, 0));
		directReschedule.setRescheduleReason("段考調整");
		ClassSchedule cancelledOriginal = schedule(303L, rescheduledClass, "星期二",
				LocalTime.of(19, 0), LocalTime.of(21, 0), ScheduleType.CANCELLED);
		cancelledOriginal.setOriginalSchedule(originalReschedule);
		cancelledOriginal.setCourseDate(LocalDate.of(2026, 7, 7));
		cancelledOriginal.setScheduledStartAt(LocalDateTime.of(2026, 7, 7, 19, 0));
		cancelledOriginal.setScheduledEndAt(LocalDateTime.of(2026, 7, 7, 21, 0));
		cancelledOriginal.setRescheduleReason("段考調整");

		MakeUpClassRequestRepository makeUpClassRequestRepository = proxy(
				MakeUpClassRequestRepository.class, (method, args) -> {
					if ("findByStatusOrderBySelectedMakeUpStartAscIdAsc".equals(method.getName())
							|| "findByTeacherIdAndStatusOrderBySelectedMakeUpStartAscIdAsc".equals(method.getName())) {
						return List.of(scheduledMakeUp);
					}
					throw new UnsupportedOperationException(method.getName());
				});
		ClassScheduleRepository classScheduleRepository = proxy(
				ClassScheduleRepository.class, (method, args) -> {
					if ("findByScheduleTypeOrderByScheduledStartAtAsc".equals(method.getName())
							|| "findByClassRoomTeacherIdAndScheduleTypeOrderByScheduledStartAtAsc".equals(method.getName())) {
						return List.of(directReschedule);
					}
					if ("findByOriginalScheduleIdAndScheduleType".equals(method.getName())) {
						return List.of(cancelledOriginal);
					}
					throw new UnsupportedOperationException(method.getName());
				});
		MakeUpClassService service = new MakeUpClassService(
				makeUpClassRequestRepository, null, classScheduleRepository, null);

		List<MakeUpRecordView> records = service.findScheduledRecords(teacher.getId(), false);

		assertThat(records).hasSize(2);
		assertThat(records).extracting(MakeUpRecordView::typeLabel)
				.containsExactly("補課", "調課");
		assertThat(records).extracting(MakeUpRecordView::className)
				.containsExactly("國一數學（A班）", "國二英文（B班）");
		assertThat(records.get(0).makeUpRequest()).isTrue();
		assertThat(records.get(1).makeUpRequest()).isFalse();
		assertThat(records.get(0).sourceLabel()).isEqualTo("王老師請假");
		assertThat(records.get(1).sourceLabel()).isEqualTo("段考調整");
		assertThat(records).extracting(MakeUpRecordView::note).containsOnlyNulls();
		assertThat(records.get(1).originalCourseText()).isEqualTo("原課程：2026/07/07 19:00 ~ 21:00");
	}

	@Test
	void absenceMakeUpIsNotCreatedWhenOriginalCourseIsAlreadyBlockedByRescheduleRequest() {
		Teacher teacher = teacher(7L, "王老師");
		ClassRoom classRoom = classRoom(11L, teacher, "高一", "數學", "");
		ClassSchedule originalSchedule = schedule(101L, classRoom, "星期二",
				LocalTime.of(19, 0), LocalTime.of(21, 0), ScheduleType.NORMAL);
		MakeUpClassRequest rescheduleRequest = new MakeUpClassRequest();
		rescheduleRequest.setSourceType(MakeUpSourceType.RESCHEDULE);
		rescheduleRequest.setStatus(MakeUpStatus.PENDING);

		TeacherAttendance attendance = new TeacherAttendance();
		attendance.setId(301L);
		attendance.setTeacher(teacher);
		attendance.setDate(LocalDate.of(2026, 6, 30));
		attendance.setMatchedCourseId(originalSchedule.getId());
		attendance.setNote("系統自動記錄缺勤");

		AtomicBoolean saved = new AtomicBoolean(false);
		MakeUpClassRequestRepository makeUpClassRequestRepository = proxy(
				MakeUpClassRequestRepository.class, (method, args) -> {
					if ("findByOriginalCourseScheduleIdAndOriginalCourseDateAndStatusIn".equals(method.getName())) {
						return List.of(rescheduleRequest);
					}
					if ("save".equals(method.getName())) {
						saved.set(true);
						return args[0];
					}
					throw new UnsupportedOperationException(method.getName());
				});
		ClassScheduleRepository classScheduleRepository = proxy(
				ClassScheduleRepository.class, (method, args) -> {
					if ("findById".equals(method.getName())) {
						return Optional.of(originalSchedule);
					}
					if ("findByOriginalScheduleIdAndScheduleType".equals(method.getName())) {
						return List.of();
					}
					throw new UnsupportedOperationException(method.getName());
				});
		MakeUpClassService service = new MakeUpClassService(
				makeUpClassRequestRepository, null, classScheduleRepository, null);

		assertThat(service.createRequiredMakeUpFromAbsence(attendance)).isNull();
		assertThat(saved).isFalse();
	}

	private ClassRoom classRoom(Long id, Teacher teacher, String grade, String subjectName, String classType) {
		Subject subject = new Subject();
		subject.setId(id + 100);
		subject.setName(subjectName);
		ClassRoom classRoom = new ClassRoom();
		classRoom.setId(id);
		classRoom.setGrade(grade);
		classRoom.setSubject(subject);
		classRoom.setClassType(classType);
		classRoom.setTeacher(teacher);
		return classRoom;
	}

	private ClassSchedule schedule(Long id, ClassRoom classRoom, String weekday,
			LocalTime startTime, LocalTime endTime, ScheduleType scheduleType) {
		ClassSchedule schedule = new ClassSchedule(weekday, startTime, endTime);
		schedule.setId(id);
		schedule.setClassRoom(classRoom);
		schedule.setScheduleType(scheduleType);
		return schedule;
	}

	private Teacher teacher(Long id, String name) {
		Teacher teacher = new Teacher();
		teacher.setId(id);
		teacher.setName(name);
		return teacher;
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
