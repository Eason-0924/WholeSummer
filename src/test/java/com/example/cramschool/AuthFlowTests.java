package com.example.cramschool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.example.cramschool.repository.TeacherRepository;
import com.example.cramschool.service.PasswordHashService;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TeacherRepository teacherRepository;

	@Autowired
	private TeacherAccountRepository teacherAccountRepository;

	@Autowired
	private PasswordHashService passwordHashService;

	private Teacher teacher;
	private Teacher director;
	private Teacher targetTeacher;
	private String username;

	@BeforeEach
	void createTeacher() {
		username = "registration-test-" + System.nanoTime();
		teacher = new Teacher();
		teacher.setName("註冊測試教師");
		teacher.setPosition(TeacherPosition.TUTOR);
		teacher.setStatus(TeacherStatus.ACTIVE);
		teacher = teacherRepository.save(teacher);
	}

	@AfterEach
	void cleanup() {
		if (teacher != null) {
			teacherAccountRepository.findByTeacherId(teacher.getId())
					.ifPresent(account -> teacherAccountRepository.deleteById(account.getId()));
			teacherRepository.deleteById(teacher.getId());
		}
		deleteTeacherAndAccount(targetTeacher);
		deleteTeacherAndAccount(director);
	}

	@Test
	void teacherMustRegisterWithCorrectSecurityCodeBeforeLogin() throws Exception {
		mockMvc.perform(post("/register")
				.param("teacherId", teacher.getId().toString())
				.param("username", username)
				.param("password", "teacher-password")
				.param("confirmPassword", "teacher-password")
				.param("registrationCode", "wrong-code"))
				.andExpect(status().isOk());
		assertThat(teacherAccountRepository.findByTeacherId(teacher.getId())).isEmpty();

		mockMvc.perform(post("/register")
				.param("teacherId", teacher.getId().toString())
				.param("username", username)
				.param("password", "teacher-password")
				.param("confirmPassword", "teacher-password")
				.param("registrationCode", "whole-summer"))
				.andExpect(status().is3xxRedirection());

		TeacherAccount account = teacherAccountRepository.findByTeacherId(teacher.getId()).orElseThrow();
		assertThat(account.getUsername()).isEqualTo(username);

		mockMvc.perform(post("/login")
				.param("username", username)
				.param("password", "teacher-password"))
				.andExpect(status().is3xxRedirection());

		teacher.setStatus(TeacherStatus.LEFT);
		teacherRepository.save(teacher);
		mockMvc.perform(post("/login")
				.param("username", username)
				.param("password", "teacher-password"))
				.andExpect(status().isOk());

		mockMvc.perform(get("/students"))
				.andExpect(status().is3xxRedirection());
	}

	@Test
	void onlyDirectorCanChangeTeacherPositionsAndRegistrationCode() throws Exception {
		TeacherAccount regularAccount = createAccount(teacher, username, "teacher-password");
		MockHttpSession regularSession = login(username, "teacher-password");

		targetTeacher = new Teacher();
		targetTeacher.setName("職位測試對象");
		targetTeacher.setPosition(TeacherPosition.TEACHER);
		targetTeacher.setStatus(TeacherStatus.ACTIVE);
		targetTeacher = teacherRepository.save(targetTeacher);

		mockMvc.perform(post("/teachers/{id}", targetTeacher.getId())
				.session(regularSession)
				.param("name", targetTeacher.getName())
				.param("position", "DIRECTOR"))
				.andExpect(status().is3xxRedirection());
		assertThat(teacherRepository.findById(targetTeacher.getId()).orElseThrow().getPosition())
				.isEqualTo(TeacherPosition.TEACHER);

		mockMvc.perform(post("/settings/registration-code")
				.session(regularSession)
				.param("currentRegistrationCode", "whole-summer")
				.param("newRegistrationCode", "changed-code")
				.param("confirmRegistrationCode", "changed-code"))
				.andExpect(status().is3xxRedirection());

		director = new Teacher();
		director.setName("權限測試主任");
		director.setPosition(TeacherPosition.DIRECTOR);
		director.setStatus(TeacherStatus.ACTIVE);
		director = teacherRepository.save(director);
		String directorUsername = "director-test-" + System.nanoTime();
		createAccount(director, directorUsername, "director-password");
		MockHttpSession directorSession = login(directorUsername, "director-password");

		mockMvc.perform(post("/teachers/{id}", targetTeacher.getId())
				.session(directorSession)
				.param("name", targetTeacher.getName())
				.param("position", "TUTOR"))
				.andExpect(status().is3xxRedirection());
		assertThat(teacherRepository.findById(targetTeacher.getId()).orElseThrow().getPosition())
				.isEqualTo(TeacherPosition.TUTOR);

		teacherAccountRepository.deleteById(regularAccount.getId());
	}

	private TeacherAccount createAccount(Teacher accountTeacher, String accountUsername, String password) {
		String salt = passwordHashService.newSalt();
		TeacherAccount account = new TeacherAccount();
		account.setTeacher(accountTeacher);
		account.setUsername(accountUsername);
		account.setPasswordSalt(salt);
		account.setPasswordHash(passwordHashService.hash(password, salt));
		return teacherAccountRepository.save(account);
	}

	private MockHttpSession login(String accountUsername, String password) throws Exception {
		return (MockHttpSession) mockMvc.perform(post("/login")
				.param("username", accountUsername)
				.param("password", password))
				.andExpect(status().is3xxRedirection())
				.andReturn()
				.getRequest()
				.getSession(false);
	}

	private void deleteTeacherAndAccount(Teacher teacherToDelete) {
		if (teacherToDelete == null || teacherRepository.findById(teacherToDelete.getId()).isEmpty()) {
			return;
		}
		teacherAccountRepository.findByTeacherId(teacherToDelete.getId())
				.ifPresent(account -> teacherAccountRepository.deleteById(account.getId()));
		teacherRepository.deleteById(teacherToDelete.getId());
	}
}
