package com.example.cramschool.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.example.cramschool.entity.ClassRoom;
import com.example.cramschool.entity.ClassStudent;
import com.example.cramschool.entity.Student;
import com.example.cramschool.entity.Subject;
import com.example.cramschool.form.GradePromotionDraft;
import com.example.cramschool.repository.ClassRoomRepository;
import com.example.cramschool.repository.ClassStudentRepository;
import com.example.cramschool.repository.StudentRepository;

class GradePromotionServiceTests {

	@Test
	void requiresNewSchoolWhenJuniorHighGraduatePromotesToHighSchool() {
		Student juniorGraduate = student(2L, "國三學生", "國三");
		StudentRepository studentRepository = repository(StudentRepository.class, (method, arguments) -> {
			if ("findByActiveTrue".equals(method)) {
				return List.of(juniorGraduate);
			}
			return null;
		});
		GradePromotionService service = new GradePromotionService(
				studentRepository,
				repository(ClassRoomRepository.class, (method, arguments) -> null),
				repository(ClassStudentRepository.class, (method, arguments) -> null));
		GradePromotionDraft draft = new GradePromotionDraft();
		draft.getTerminalStudentActions().put(2L, GradePromotionService.ACTION_PROMOTE);

		assertThatThrownBy(() -> service.validateTerminalActions(draft))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("新學校");
	}

	@Test
	void promotesStudentsCreatesNextClassAndAddsSelectedActiveStudents() {
		Student firstYear = student(1L, "國一學生", "國一");
		Student juniorGraduate = student(2L, "國三學生", "國三");
		Student seniorGraduate = student(3L, "高三學生", "高三");
		List<Student> students = List.of(firstYear, juniorGraduate, seniorGraduate);

		Subject subject = new Subject();
		subject.setId(10L);
		subject.setName("數學");
		ClassRoom oldClass = new ClassRoom();
		oldClass.setId(20L);
		oldClass.setGrade("國一");
		oldClass.setSubject(subject);
		oldClass.setClassType("A班");
		oldClass.setActive(true);

		ClassStudent firstMembership = membership(oldClass, firstYear);
		ClassStudent juniorMembership = membership(oldClass, juniorGraduate);
		ClassStudent seniorMembership = membership(oldClass, seniorGraduate);
		List<ClassStudent> originalMemberships = List.of(
				firstMembership, juniorMembership, seniorMembership);
		List<ClassRoom> savedClasses = new ArrayList<>();
		List<ClassStudent> savedMemberships = new ArrayList<>();

		StudentRepository studentRepository = repository(StudentRepository.class, (method, arguments) -> {
			if ("findByActiveTrue".equals(method)) {
				return students;
			}
			return null;
		});
		ClassRoomRepository classRoomRepository = repository(ClassRoomRepository.class, (method, arguments) -> {
			if ("findById".equals(method)) {
				return Optional.of(oldClass);
			}
			if ("save".equals(method)) {
				ClassRoom classRoom = (ClassRoom) arguments[0];
				savedClasses.add(classRoom);
				return classRoom;
			}
			return null;
		});
		ClassStudentRepository classStudentRepository = repository(ClassStudentRepository.class,
				(method, arguments) -> {
					if ("findByClassRoomIdAndActiveTrueOrderByStudentChineseNameAsc".equals(method)) {
						return originalMemberships;
					}
					if ("save".equals(method)) {
						ClassStudent membership = (ClassStudent) arguments[0];
						savedMemberships.add(membership);
						return membership;
					}
					return null;
				});
		GradePromotionService service = new GradePromotionService(
				studentRepository, classRoomRepository, classStudentRepository);

		GradePromotionDraft draft = new GradePromotionDraft();
		draft.getTerminalStudentActions().put(2L, GradePromotionService.ACTION_PROMOTE);
		draft.getPromotedStudentSchools().put(2L, "測試高中");
		draft.getTerminalStudentActions().put(3L, GradePromotionService.ACTION_GRADUATE);
		draft.getPromotedClassIds().add(20L);
		draft.getJoinedStudentIdsByClass().put(20L, Set.of(1L, 2L, 3L));

		GradePromotionService.PromotionResult result = service.complete(draft);

		assertThat(firstYear.getGrade()).isEqualTo("國二");
		assertThat(firstYear.isActive()).isTrue();
		assertThat(juniorGraduate.getGrade()).isEqualTo("高一");
		assertThat(juniorGraduate.getSchool()).isEqualTo("測試高中");
		assertThat(juniorGraduate.isActive()).isTrue();
		assertThat(seniorGraduate.getGrade()).isEqualTo("高三");
		assertThat(seniorGraduate.isActive()).isFalse();
		assertThat(oldClass.isActive()).isFalse();
		assertThat(result.promotedStudentCount()).isEqualTo(3);
		assertThat(result.createdClassCount()).isEqualTo(1);
		assertThat(result.joinedStudentCount()).isEqualTo(2);

		assertThat(savedClasses).hasSize(2);
		ClassRoom newClass = savedClasses.stream()
				.filter(ClassRoom::isActive)
				.findFirst()
				.orElseThrow();
		assertThat(newClass.getGrade()).isEqualTo("國二");
		assertThat(newClass.getDisplayName()).isEqualTo("國二數學（A班）");

		assertThat(savedMemberships)
				.extracting(membership -> membership.getStudent().getId())
				.containsExactlyInAnyOrder(1L, 2L);
		assertThat(savedMemberships)
				.allMatch(membership -> membership.getClassRoom() == newClass);
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

	private Student student(Long id, String name, String grade) {
		Student student = new Student();
		student.setId(id);
		student.setChineseName(name);
		student.setGrade(grade);
		student.setActive(true);
		return student;
	}

	private ClassStudent membership(ClassRoom classRoom, Student student) {
		ClassStudent membership = new ClassStudent();
		membership.setClassRoom(classRoom);
		membership.setStudent(student);
		membership.setActive(true);
		return membership;
	}
}
