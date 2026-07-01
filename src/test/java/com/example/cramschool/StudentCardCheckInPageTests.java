package com.example.cramschool;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import com.example.cramschool.entity.Teacher;
import com.example.cramschool.entity.TeacherAccount;
import com.example.cramschool.entity.TeacherPosition;
import com.example.cramschool.entity.TeacherStatus;
import com.example.cramschool.repository.TeacherAccountRepository;
import com.example.cramschool.repository.TeacherMonthlySalaryRepository;
import com.example.cramschool.repository.TeacherRepository;
import com.example.cramschool.service.PasswordHashService;

@SpringBootTest
@AutoConfigureMockMvc
class StudentCardCheckInPageTests {

	private static final String TEST_PASSWORD = "card-checkin-page-password";

	@Autowired
	private MockMvc mockMvc;

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
	private String username;

	@BeforeEach
	void setUp() {
		username = "card-checkin-page-" + System.nanoTime();
		teacher = new Teacher();
		teacher.setName("刷卡頁測試教師");
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
	}

	@AfterEach
	void tearDown() {
		if (account != null && account.getId() != null) {
			teacherAccountRepository.deleteById(account.getId());
		}
		if (teacher != null && teacher.getId() != null) {
			teacherMonthlySalaryRepository.deleteByTeacherId(teacher.getId());
			teacherRepository.deleteById(teacher.getId());
		}
	}

	@Test
	void cardCheckInPageRequiresLogin() throws Exception {
		mockMvc.perform(get("/attendance/card-check-in"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrlPattern("/login?redirect=*"));
	}

	@Test
	void cardCheckInPageShowsScannerControls() throws Exception {
		mockMvc.perform(get("/attendance/card-check-in").session(login()))
				.andExpect(status().isOk())
				.andExpect(content().string(Matchers.containsString("刷卡點名")))
				.andExpect(content().string(Matchers.containsString("請感應學生卡片")))
				.andExpect(content().string(Matchers.containsString("id=\"cardIdInput\"")))
				.andExpect(content().string(Matchers.containsString("data-check-in-url=\"/api/attendance/card-check-in\"")))
				.andExpect(content().string(Matchers.containsString("最近刷卡紀錄")))
				.andExpect(content().string(Matchers.containsString("尚無刷卡紀錄")));
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
