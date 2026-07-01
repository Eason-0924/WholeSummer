package com.example.cramschool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
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
import com.example.cramschool.service.TeacherPermissionService;

@SpringBootTest
@AutoConfigureMockMvc
class HomeShortcutCustomizationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TeacherRepository teacherRepository;

	@Autowired
	private TeacherAccountRepository teacherAccountRepository;

	@Autowired
	private TeacherMonthlySalaryRepository teacherMonthlySalaryRepository;

	@Autowired
	private TeacherPermissionService teacherPermissionService;

	@Autowired
	private PasswordHashService passwordHashService;

	private Teacher regularTeacher;
	private Teacher directorTeacher;
	private TeacherAccount regularAccount;
	private TeacherAccount directorAccount;

	@AfterEach
	void cleanup() {
		deleteAccountAndTeacher(regularAccount, regularTeacher);
		deleteAccountAndTeacher(directorAccount, directorTeacher);
	}

	@Test
	void homePageShowsPermissionAwareShortcutOptions() throws Exception {
		regularTeacher = createTeacher("首頁一般教師", TeacherPosition.TEACHER);
		directorTeacher = createTeacher("首頁主任", TeacherPosition.DIRECTOR);
		regularAccount = createAccount(regularTeacher, "home-regular-" + System.nanoTime());
		directorAccount = createAccount(directorTeacher, "home-director-" + System.nanoTime());

		MockHttpSession regularSession = login(regularAccount.getUsername());
		MockHttpSession directorSession = login(directorAccount.getUsername());

		String regularHtml = mockMvc.perform(get("/").session(regularSession))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String directorHtml = mockMvc.perform(get("/").session(directorSession))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertThat(regularHtml)
				.contains("快速打卡")
				.doesNotContain("學費管理");
		assertThat(directorHtml)
				.contains("學費管理")
				.doesNotContain("快速打卡");
	}

	@Test
	void teacherCanSaveCustomHomeShortcutOrder() throws Exception {
		regularTeacher = createTeacher("快捷欄教師", TeacherPosition.TEACHER);
		regularAccount = createAccount(regularTeacher, "home-save-" + System.nanoTime());
		MockHttpSession session = login(regularAccount.getUsername());

		mockMvc.perform(post("/home/shortcuts")
				.session(session)
				.param("shortcutIds", "settings", "students", "quick-clock", "settings", "tuition")
				.param("showDescription", "true"))
				.andExpect(status().is3xxRedirection());

		Teacher savedTeacher = teacherRepository.findById(regularTeacher.getId()).orElseThrow();
		assertThat(savedTeacher.getHomeShortcuts()).isEqualTo("settings,students,quick-clock");
		assertThat(savedTeacher.isHomeShortcutShowDescription()).isTrue();

		String html = mockMvc.perform(get("/").session(session))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		assertThat(html.indexOf("data-shortcut-id=\"settings\""))
				.isLessThan(html.indexOf("data-shortcut-id=\"students\""));
		assertThat(html.indexOf("data-shortcut-id=\"students\""))
				.isLessThan(html.indexOf("data-shortcut-id=\"quick-clock\""));
		assertThat(html).doesNotContain("href=\"/classes\"");
		assertThat(html).contains("data-shortcut-id=\"classes\"");
		assertThat(html).doesNotContain("data-shortcut-id=\"tuition\"");
	}

	@Test
	void teacherCanHideShortcutDescriptions() throws Exception {
		regularTeacher = createTeacher("描述隱藏教師", TeacherPosition.TEACHER);
		regularAccount = createAccount(regularTeacher, "home-description-" + System.nanoTime());
		MockHttpSession session = login(regularAccount.getUsername());

		mockMvc.perform(post("/home/shortcuts")
				.session(session)
				.param("shortcutIds", "students", "settings"))
				.andExpect(status().is3xxRedirection());

		Teacher savedTeacher = teacherRepository.findById(regularTeacher.getId()).orElseThrow();
		assertThat(savedTeacher.isHomeShortcutShowDescription()).isFalse();

		String html = mockMvc.perform(get("/").session(session))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		assertThat(html).doesNotContain("新增、編輯與停用學生資料。");
		assertThat(html).contains("顯示描述");
	}

	private Teacher createTeacher(String name, TeacherPosition position) {
		Teacher teacher = new Teacher();
		teacher.setName(name);
		teacher.setPosition(position);
		teacher.setStatus(TeacherStatus.ACTIVE);
		return teacherRepository.save(teacher);
	}

	private TeacherAccount createAccount(Teacher teacher, String username) {
		String salt = passwordHashService.newSalt();
		TeacherAccount account = new TeacherAccount();
		account.setTeacher(teacher);
		account.setUsername(username);
		account.setPasswordSalt(salt);
		account.setPasswordHash(passwordHashService.hash("shortcut-password", salt));
		return teacherAccountRepository.save(account);
	}

	private MockHttpSession login(String username) throws Exception {
		return (MockHttpSession) mockMvc.perform(post("/login")
				.param("username", username)
				.param("password", "shortcut-password"))
				.andExpect(status().is3xxRedirection())
				.andReturn()
				.getRequest()
				.getSession(false);
	}

	private void deleteAccountAndTeacher(TeacherAccount account, Teacher teacher) {
		if (account != null && account.getId() != null) {
			teacherAccountRepository.deleteById(account.getId());
		}
		if (teacher != null && teacher.getId() != null) {
			teacherPermissionService.deleteByTeacherId(teacher.getId());
			teacherMonthlySalaryRepository.deleteByTeacherId(teacher.getId());
			teacherRepository.deleteById(teacher.getId());
		}
	}
}
