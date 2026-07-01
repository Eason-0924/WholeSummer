package com.example.cramschool.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.cramschool.entity.Student;
import com.example.cramschool.entity.Teacher;
import com.example.cramschool.entity.TeacherPosition;
import com.example.cramschool.entity.TeacherStatus;
import com.example.cramschool.repository.StudentRepository;
import com.example.cramschool.repository.TeacherMonthlySalaryRepository;
import com.example.cramschool.repository.TeacherRepository;

@SpringBootTest
class StudentCardBindingTests {

	@Autowired
	private StudentService studentService;

	@Autowired
	private StudentRepository studentRepository;

	@Autowired
	private TeacherRepository teacherRepository;

	@Autowired
	private TeacherMonthlySalaryRepository teacherMonthlySalaryRepository;

	private Teacher teacher;
	private Student firstStudent;
	private Student secondStudent;
	private String cardPrefix;

	@BeforeEach
	void setUp() {
		cardPrefix = "T" + System.nanoTime();
		teacher = new Teacher();
		teacher.setName("綁卡測試教師");
		teacher.setPosition(TeacherPosition.TEACHER);
		teacher.setStatus(TeacherStatus.ACTIVE);
		teacher = teacherRepository.save(teacher);

		firstStudent = student("綁卡測試學生一");
		secondStudent = student("綁卡測試學生二");
	}

	@AfterEach
	void tearDown() {
		if (firstStudent != null && firstStudent.getId() != null) {
			studentRepository.deleteById(firstStudent.getId());
		}
		if (secondStudent != null && secondStudent.getId() != null) {
			studentRepository.deleteById(secondStudent.getId());
		}
		if (teacher != null && teacher.getId() != null) {
			teacherMonthlySalaryRepository.deleteByTeacherId(teacher.getId());
			teacherRepository.deleteById(teacher.getId());
		}
	}

	@Test
	void normalizesCardIdBeforeBinding() {
		Student bound = studentService.bindCard(firstStudent.getId(), "  " + cardPrefix + " a1 b2 \n",
				false, teacher.getId());

		assertThat(bound.getCardId()).isEqualTo(cardPrefix + "A1B2");
		assertThat(bound.getCardStatus()).isEqualTo("ACTIVE");
		assertThat(bound.getCardBoundAt()).isNotNull();
	}

	@Test
	void rejectsCardAlreadyBoundToAnotherStudent() {
		studentService.bindCard(firstStudent.getId(), cardPrefix + "DUP", false, teacher.getId());

		assertThatThrownBy(() -> studentService.bindCard(secondStudent.getId(), cardPrefix + "DUP",
				false, teacher.getId()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("已綁定其他學生");
	}

	@Test
	void requiresOverwriteWhenStudentAlreadyHasDifferentCard() {
		studentService.bindCard(firstStudent.getId(), cardPrefix + "OLD", false, teacher.getId());

		assertThatThrownBy(() -> studentService.bindCard(firstStudent.getId(), cardPrefix + "NEW",
				false, teacher.getId()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("已有綁定卡片");

		Student rebound = studentService.bindCard(firstStudent.getId(), cardPrefix + "NEW", true, teacher.getId());

		assertThat(rebound.getCardId()).isEqualTo(cardPrefix + "NEW");
	}

	private Student student(String name) {
		Student student = new Student();
		student.setChineseName(name);
		student.setActive(true);
		return studentRepository.save(student);
	}
}
