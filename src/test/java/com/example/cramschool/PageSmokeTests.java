package com.example.cramschool;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import com.example.cramschool.repository.ClassRoomRepository;
import com.example.cramschool.repository.ExamRepository;
import com.example.cramschool.repository.HomeworkRepository;
import com.example.cramschool.repository.StudentRepository;
import com.example.cramschool.repository.SubjectRepository;
import com.example.cramschool.repository.TeacherRepository;
import com.example.cramschool.repository.TeacherAccountRepository;
import com.example.cramschool.entity.Teacher;
import com.example.cramschool.entity.TeacherAccount;
import com.example.cramschool.entity.TeacherPosition;
import com.example.cramschool.entity.TeacherStatus;
import com.example.cramschool.service.PasswordHashService;

@SpringBootTest
@AutoConfigureMockMvc
class PageSmokeTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private StudentRepository studentRepository;

	@Autowired
	private TeacherRepository teacherRepository;

	@Autowired
	private TeacherAccountRepository teacherAccountRepository;

	@Autowired
	private PasswordHashService passwordHashService;

	private Teacher testTeacher;
	private TeacherAccount testAccount;
	private String testUsername;
	private static final String TEST_PASSWORD = "page-smoke-password";

	@Autowired
	private ClassRoomRepository classRoomRepository;

	@Autowired
	private SubjectRepository subjectRepository;

	@Autowired
	private ExamRepository examRepository;

	@Autowired
	private HomeworkRepository homeworkRepository;

	@BeforeEach
	void createLoginAccount() {
		testUsername = "page-smoke-" + System.nanoTime();
		testTeacher = new Teacher();
		testTeacher.setName("頁面測試教師");
		testTeacher.setNickname("頁測");
		testTeacher.setPosition(TeacherPosition.TEACHER);
		testTeacher.setStatus(TeacherStatus.ACTIVE);
		testTeacher = teacherRepository.save(testTeacher);

		String salt = passwordHashService.newSalt();
		testAccount = new TeacherAccount();
		testAccount.setTeacher(testTeacher);
		testAccount.setUsername(testUsername);
		testAccount.setPasswordSalt(salt);
		testAccount.setPasswordHash(passwordHashService.hash(TEST_PASSWORD, salt));
		testAccount = teacherAccountRepository.save(testAccount);
	}

	@AfterEach
	void deleteLoginAccount() {
		if (testAccount != null) {
			teacherAccountRepository.deleteById(testAccount.getId());
		}
		if (testTeacher != null) {
			teacherRepository.deleteById(testTeacher.getId());
		}
	}

	@Test
	void mainPagesRender() throws Exception {
		MockHttpSession session = (MockHttpSession) mockMvc.perform(post("/login")
				.param("username", testUsername)
				.param("password", TEST_PASSWORD))
				.andExpect(status().is3xxRedirection())
				.andReturn()
				.getRequest()
				.getSession(false);

		String[] paths = {
				"/",
				"/students",
				"/teachers",
				"/teachers/attendance",
				"/classes",
				"/classes/new",
				"/subjects",
				"/subjects/new",
				"/exams",
				"/homeworks",
				"/settings"
		};

		mockMvc.perform(get("/register")).andExpect(status().isOk());

		for (String path : paths) {
			mockMvc.perform(get(path).session(session))
					.andExpect(status().isOk());
		}
		mockMvc.perform(get("/backup").session(session))
				.andExpect(status().is3xxRedirection());

		if (!studentRepository.findAll().isEmpty()) {
			Long id = studentRepository.findAll().getFirst().getId();
			mockMvc.perform(get("/students/{id}", id).session(session)).andExpect(status().isOk());
			mockMvc.perform(get("/students/{id}/edit", id).session(session)).andExpect(status().isOk());
		}
		if (!teacherRepository.findAll().isEmpty()) {
			Long id = teacherRepository.findAll().getFirst().getId();
			mockMvc.perform(get("/teachers/{id}", id).session(session)).andExpect(status().isOk());
			mockMvc.perform(get("/teachers/{id}/edit", id).session(session)).andExpect(status().isOk());
		}
		if (!classRoomRepository.findAll().isEmpty()) {
			Long id = classRoomRepository.findAll().getFirst().getId();
			mockMvc.perform(get("/classes/{id}", id).session(session)).andExpect(status().isOk());
			mockMvc.perform(get("/classes/{id}/edit", id).session(session)).andExpect(status().isOk());
			mockMvc.perform(get("/classes/{id}/attendance", id).session(session)).andExpect(status().isOk());
		}
		if (!subjectRepository.findAll().isEmpty()) {
			Long id = subjectRepository.findAll().getFirst().getId();
			mockMvc.perform(get("/subjects/{id}/edit", id).session(session)).andExpect(status().isOk());
		}
		if (!examRepository.findAll().isEmpty()) {
			Long id = examRepository.findAll().getFirst().getId();
			mockMvc.perform(get("/exams/{id}", id).session(session)).andExpect(status().isOk());
			mockMvc.perform(get("/exams/{id}/edit", id).session(session)).andExpect(status().isOk());
			mockMvc.perform(get("/exams/{id}/scores", id).session(session)).andExpect(status().isOk());
		}
		if (!homeworkRepository.findAll().isEmpty()) {
			Long id = homeworkRepository.findAll().getFirst().getId();
			mockMvc.perform(get("/homeworks/{id}", id).session(session)).andExpect(status().isOk());
			mockMvc.perform(get("/homeworks/{id}/edit", id).session(session)).andExpect(status().isOk());
			mockMvc.perform(get("/homeworks/{id}/records", id).session(session)).andExpect(status().isOk());
		}
	}
}
