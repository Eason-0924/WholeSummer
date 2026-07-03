package com.example.cramschool.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Proxy;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.example.cramschool.entity.ClassRoom;
import com.example.cramschool.entity.ClassSchedule;
import com.example.cramschool.entity.Subject;
import com.example.cramschool.entity.Teacher;
import com.example.cramschool.entity.TeacherPosition;
import com.example.cramschool.form.ClassRoomForm;
import com.example.cramschool.repository.ClassRoomRepository;
import com.example.cramschool.repository.ClassScheduleRepository;
import com.example.cramschool.repository.ClassStudentRepository;
import com.example.cramschool.repository.ExamRepository;
import com.example.cramschool.repository.HomeworkRecordRepository;
import com.example.cramschool.repository.HomeworkRepository;
import com.example.cramschool.repository.MakeUpClassRequestRepository;
import com.example.cramschool.repository.ScoreRepository;
import com.example.cramschool.repository.StudentAttendanceRepository;
import com.example.cramschool.repository.SubjectRepository;
import com.example.cramschool.repository.TeacherLeaveRepository;
import com.example.cramschool.repository.TeacherPermissionRepository;
import com.example.cramschool.repository.TeacherRepository;

class ClassRoomServiceTests {

	@Test
	void updateSubjectKeepsUnchangedSchedules() {
		Subject oldSubject = subject(1L, "數學");
		Subject newSubject = subject(2L, "物理");
		Teacher director = new Teacher();
		director.setId(7L);
		director.setName("主任");
		director.setPosition(TeacherPosition.DIRECTOR);

		ClassSchedule originalSchedule = new ClassSchedule("星期二", LocalTime.of(19, 0), LocalTime.of(21, 0));
		originalSchedule.setId(101L);
		ClassRoom classRoom = new ClassRoom();
		classRoom.setId(11L);
		classRoom.setGrade("高一");
		classRoom.setSubject(oldSubject);
		classRoom.setTeacher(director);
		classRoom.addSchedule(originalSchedule);

		ClassRoomService service = service(classRoom, newSubject, director);
		ClassRoomForm form = ClassRoomForm.from(classRoom);
		form.setSubjectId(newSubject.getId());

		ClassRoom updatedClassRoom = service.update(classRoom.getId(), form, director.getId());

		assertThat(updatedClassRoom.getSubject()).isSameAs(newSubject);
		assertThat(updatedClassRoom.getSchedules()).containsExactly(originalSchedule);
	}

	@Test
	void updateScheduleKeepsExistingScheduleRow() {
		Subject subject = subject(1L, "數學");
		Teacher director = new Teacher();
		director.setId(7L);
		director.setName("主任");
		director.setPosition(TeacherPosition.DIRECTOR);

		ClassSchedule originalSchedule = new ClassSchedule("星期二", LocalTime.of(19, 0), LocalTime.of(21, 0));
		originalSchedule.setId(101L);
		ClassRoom classRoom = new ClassRoom();
		classRoom.setId(11L);
		classRoom.setGrade("高一");
		classRoom.setSubject(subject);
		classRoom.setTeacher(director);
		classRoom.addSchedule(originalSchedule);

		ClassRoomService service = service(classRoom, subject, director);
		ClassRoomForm form = ClassRoomForm.from(classRoom);
		form.getScheduleEntries().getFirst().setWeekday("星期三");
		form.getScheduleEntries().getFirst().setStartTime(LocalTime.of(18, 30));
		form.getScheduleEntries().getFirst().setEndTime(LocalTime.of(20, 30));

		ClassRoom updatedClassRoom = service.update(classRoom.getId(), form, director.getId());

		assertThat(updatedClassRoom.getSchedules()).containsExactly(originalSchedule);
		assertThat(originalSchedule.getId()).isEqualTo(101L);
		assertThat(originalSchedule.getWeekday()).isEqualTo("星期三");
		assertThat(originalSchedule.getStartTime()).isEqualTo(LocalTime.of(18, 30));
		assertThat(originalSchedule.getEndTime()).isEqualTo(LocalTime.of(20, 30));
	}

	@Test
	void deleteClearsScheduleEventsBeforeDeletingClassRoom() {
		Subject subject = subject(1L, "數學");
		Teacher director = new Teacher();
		director.setId(7L);
		director.setName("主任");
		director.setPosition(TeacherPosition.DIRECTOR);

		ClassRoom classRoom = new ClassRoom();
		classRoom.setId(11L);
		classRoom.setGrade("高一");
		classRoom.setSubject(subject);
		classRoom.setTeacher(director);
		List<String> calls = new ArrayList<>();

		ClassRoomService service = service(classRoom, subject, director, calls);

		service.delete(classRoom.getId(), director.getId());

		assertThat(calls).containsSubsequence(
				"classSchedules.deleteEventSchedulesByClassRoomId",
				"classSchedules.deleteBaseSchedulesByClassRoomId",
				"classRooms.deleteById");
	}

	private ClassRoomService service(ClassRoom classRoom, Subject subject, Teacher teacher) {
		return service(classRoom, subject, teacher, new ArrayList<>());
	}

	private ClassRoomService service(ClassRoom classRoom, Subject subject, Teacher teacher, List<String> calls) {
		ClassRoomRepository classRoomRepository = repository(ClassRoomRepository.class, (method, arguments) -> {
			if ("findById".equals(method)) {
				return Optional.of(classRoom);
			}
			if ("save".equals(method)) {
				return arguments[0];
			}
			if ("delete".equals(method)) {
				calls.add("classRooms.delete");
				return null;
			}
			if ("deleteById".equals(method)) {
				calls.add("classRooms.deleteById");
				return null;
			}
			if ("existsByUrlSlug".equals(method) || "existsByUrlSlugAndIdNot".equals(method)) {
				return false;
			}
			throw new UnsupportedOperationException(method);
		});
		SubjectRepository subjectRepository = repository(SubjectRepository.class, (method, arguments) -> {
			if ("findById".equals(method)) {
				return Optional.of(subject);
			}
			if ("existsByUrlSlug".equals(method) || "existsByUrlSlugAndIdNot".equals(method)) {
				return false;
			}
			throw new UnsupportedOperationException(method);
		});
		TeacherRepository teacherRepository = repository(TeacherRepository.class, (method, arguments) -> {
			if ("findById".equals(method)) {
				return Optional.of(teacher);
			}
			throw new UnsupportedOperationException(method);
		});
		TeacherPermissionRepository teacherPermissionRepository = repository(TeacherPermissionRepository.class,
				(method, arguments) -> Optional.empty());
		ExamRepository examRepository = repository(ExamRepository.class, (method, arguments) -> {
			if ("findByClassRoomIdOrderByExamDateDescIdDesc".equals(method)) {
				return List.of();
			}
			if ("deleteByClassRoomId".equals(method)) {
				return null;
			}
			throw new UnsupportedOperationException(method);
		});
		ScoreRepository scoreRepository = repository(ScoreRepository.class, (method, arguments) -> null);
		HomeworkRepository homeworkRepository = repository(HomeworkRepository.class, (method, arguments) -> {
			if ("deleteByClassRoomId".equals(method)) {
				return null;
			}
			throw new UnsupportedOperationException(method);
		});
		HomeworkRecordRepository homeworkRecordRepository = repository(HomeworkRecordRepository.class, (method, arguments) -> {
			if ("deleteByHomeworkClassRoomId".equals(method)) {
				return null;
			}
			throw new UnsupportedOperationException(method);
		});
		StudentAttendanceRepository studentAttendanceRepository = repository(StudentAttendanceRepository.class,
				(method, arguments) -> {
					if ("deleteByClassRoomId".equals(method)) {
						return null;
					}
					throw new UnsupportedOperationException(method);
				});
		MakeUpClassRequestRepository makeUpClassRequestRepository = repository(MakeUpClassRequestRepository.class,
				(method, arguments) -> {
					if ("deleteByClassRoomId".equals(method)) {
						return null;
					}
					if ("flush".equals(method)) {
						return null;
					}
					throw new UnsupportedOperationException(method);
				});
		TeacherLeaveRepository teacherLeaveRepository = repository(TeacherLeaveRepository.class, (method, arguments) -> {
			if ("deleteByCourseScheduleClassRoomId".equals(method)) {
				return null;
			}
			if ("flush".equals(method)) {
				return null;
			}
			throw new UnsupportedOperationException(method);
		});
		ClassStudentRepository classStudentRepository = repository(ClassStudentRepository.class, (method, arguments) -> {
			if ("deleteByClassRoomId".equals(method)) {
				return null;
			}
			if ("flush".equals(method)) {
				return null;
			}
			throw new UnsupportedOperationException(method);
		});
		ClassScheduleRepository classScheduleRepository = repository(ClassScheduleRepository.class, (method, arguments) -> {
			if ("deleteEventSchedulesByClassRoomId".equals(method)) {
				calls.add("classSchedules.deleteEventSchedulesByClassRoomId");
				return null;
			}
			if ("deleteBaseSchedulesByClassRoomId".equals(method)) {
				calls.add("classSchedules.deleteBaseSchedulesByClassRoomId");
				return null;
			}
			if ("flush".equals(method)) {
				return null;
			}
			throw new UnsupportedOperationException(method);
		});
		UrlSlugSupport urlSlugSupport = new UrlSlugSupport();
		SubjectUrlSlugService subjectUrlSlugService = new SubjectUrlSlugService(subjectRepository, urlSlugSupport);
		ClassRoomUrlSlugService classRoomUrlSlugService = new ClassRoomUrlSlugService(
				classRoomRepository, subjectUrlSlugService, urlSlugSupport);
		TeacherPermissionService teacherPermissionService = new TeacherPermissionService(
				teacherPermissionRepository, teacherRepository);
		return new ClassRoomService(
				classRoomRepository,
				subjectRepository,
				teacherRepository,
				classStudentRepository,
				examRepository,
				scoreRepository,
				homeworkRepository,
				homeworkRecordRepository,
				studentAttendanceRepository,
				teacherPermissionService,
				teacherLeaveRepository,
				makeUpClassRequestRepository,
				classScheduleRepository,
				classRoomUrlSlugService);
	}

	private Subject subject(Long id, String name) {
		Subject subject = new Subject();
		subject.setId(id);
		subject.setName(name);
		return subject;
	}

	@SuppressWarnings("unchecked")
	private <T> T repository(Class<T> type, RepositoryCall call) {
		return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, (proxy, method, arguments) -> {
			if (method.getDeclaringClass() == Object.class) {
				return switch (method.getName()) {
					case "toString" -> type.getSimpleName() + "TestProxy";
					case "hashCode" -> System.identityHashCode(proxy);
					case "equals" -> proxy == arguments[0];
					default -> null;
				};
			}
			return call.invoke(method.getName(), arguments == null ? new Object[0] : arguments);
		});
	}

	@FunctionalInterface
	private interface RepositoryCall {
		Object invoke(String method, Object[] arguments);
	}
}
