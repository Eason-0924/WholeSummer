package com.example.cramschool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import com.example.cramschool.entity.Student;
import com.example.cramschool.entity.Teacher;
import com.example.cramschool.entity.TeacherAccount;
import com.example.cramschool.entity.TeacherPosition;
import com.example.cramschool.entity.TeacherStatus;
import com.example.cramschool.repository.StudentRepository;
import com.example.cramschool.repository.TeacherAccountRepository;
import com.example.cramschool.repository.TeacherMonthlySalaryRepository;
import com.example.cramschool.repository.TeacherRepository;
import com.example.cramschool.service.PasswordHashService;

@SpringBootTest
@AutoConfigureMockMvc
class StudentCardBindingPageTests {

	private static final String TEST_PASSWORD = "card-page-password";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private StudentRepository studentRepository;

	@Autowired
	private TeacherRepository teacherRepository;

	@Autowired
	private TeacherAccountRepository teacherAccountRepository;

	@Autowired
	private TeacherMonthlySalaryRepository teacherMonthlySalaryRepository;

	@Autowired
	private PasswordHashService passwordHashService;

	private Teacher teacher;
	private TeacherAccount account;
	private Student student;
	private String username;

	@BeforeEach
	void setUp() {
		username = "card-page-" + System.nanoTime();
		teacher = new Teacher();
		teacher.setName("卡片頁測試教師");
		teacher.setPosition(TeacherPosition.TEACHER);
		teacher.setStatus(TeacherStatus.ACTIVE);
		teacher = teacherRepository.save(teacher);

		String salt = passwordHashService.newSalt();
		account = new TeacherAccount();
		account.setTeacher(teacher);
		account.setUsername(username);
		account.setPasswordSalt(salt);
		account.setPasswordHash(passwordHashService.hash(TEST_PASSWORD, salt));
		account = teacherAccountRepository.save(account);

		student = new Student();
		student.setChineseName("卡片頁測試學生");
		student.setGrade("國一");
		student.setActive(true);
		student = studentRepository.save(student);
	}

	@AfterEach
	void tearDown() {
		if (student != null && student.getId() != null) {
			studentRepository.deleteById(student.getId());
		}
		if (account != null && account.getId() != null) {
			teacherAccountRepository.deleteById(account.getId());
		}
		if (teacher != null && teacher.getId() != null) {
			teacherMonthlySalaryRepository.deleteByTeacherId(teacher.getId());
			teacherRepository.deleteById(teacher.getId());
		}
	}

	@Test
	void cardBindPageBindsNormalizedCardId() throws Exception {
		MockHttpSession session = login();

		mockMvc.perform(get("/attendance/card-bind").session(session))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("卡片綁定")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("卡片頁測試學生")));

		mockMvc.perform(post("/attendance/card-bind")
				.param("targetKey", "STUDENT:" + student.getId())
				.param("cardId", "  04 a1 b2 c3 \n")
				.session(session))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/attendance/card-bind?targetKey=STUDENT:" + student.getId()));

		Student updated = studentRepository.findById(student.getId()).orElseThrow();
		assertThat(updated.getCardId()).isEqualTo("04A1B2C3");
		assertThat(updated.getCardStatus()).isEqualTo("ACTIVE");
		assertThat(updated.getCardBoundAt()).isNotNull();
	}

	private MockHttpSession login() throws Exception {
		return (MockHttpSession) mockMvc.perform(post("/login")
				.param("username", username)
				.param("password", TEST_PASSWORD))
				.andExpect(status().is3xxRedirection())
				.andReturn()
				.getRequest()
				.getSession(false);
	}
}
