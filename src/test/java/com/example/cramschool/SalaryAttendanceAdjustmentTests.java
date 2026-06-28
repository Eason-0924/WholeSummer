package com.example.cramschool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalTime;

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
import com.example.cramschool.entity.TeacherAttendance;
import com.example.cramschool.entity.TeacherAttendanceStatus;
import com.example.cramschool.entity.TeacherPosition;
import com.example.cramschool.entity.TeacherStatus;
import com.example.cramschool.entity.ClassRoom;
import com.example.cramschool.entity.ClassSchedule;
import com.example.cramschool.repository.ClassRoomRepository;
import com.example.cramschool.repository.TeacherAccountRepository;
import com.example.cramschool.repository.TeacherAttendanceRepository;
import com.example.cramschool.repository.TeacherMonthlySalaryRepository;
import com.example.cramschool.repository.TeacherRepository;
import com.example.cramschool.service.PasswordHashService;

@SpringBootTest
@AutoConfigureMockMvc
class SalaryAttendanceAdjustmentTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TeacherRepository teacherRepository;

	@Autowired
	private TeacherAccountRepository teacherAccountRepository;

	@Autowired
	private TeacherAttendanceRepository teacherAttendanceRepository;

	@Autowired
	private TeacherMonthlySalaryRepository teacherMonthlySalaryRepository;

	@Autowired
	private ClassRoomRepository classRoomRepository;

	@Autowired
	private PasswordHashService passwordHashService;

	private Teacher director;
	private Teacher teacher;
	private TeacherAttendance attendance;
	private String directorUsername;
	private String teacherUsername;

	@BeforeEach
	void setup() {
		cleanupStaleTestTeachers();
		long suffix = System.nanoTime();
		director = createTeacher("薪資測試主任", TeacherPosition.DIRECTOR);
		teacher = createTeacher("薪資測試教師", TeacherPosition.TEACHER);
		directorUsername = "SalaryDirector" + suffix;
		teacherUsername = "SalaryTeacher" + suffix;
		createAccount(director, directorUsername, "DirectorPassword123");
		createAccount(teacher, teacherUsername, "TeacherPassword123");

		attendance = new TeacherAttendance();
		attendance.setTeacher(teacher);
		attendance.setDate(LocalDate.of(2026, 6, 25));
		attendance.setClockInTime(LocalTime.of(17, 30));
		attendance.setClockOutTime(LocalTime.of(19, 30));
		attendance.setStatus(TeacherAttendanceStatus.WORKING);
		attendance.setWorkMinutes(0L);
		attendance = teacherAttendanceRepository.save(attendance);
	}

	@AfterEach
	void cleanup() {
		deleteTeacherData(teacher);
		deleteTeacherData(director);
	}

	@Test
	void onlyDirectorCanAdjustUnmatchedAttendanceAndSalaryIsRecalculated() throws Exception {
		MockHttpSession teacherSession = login(teacherUsername, "TeacherPassword123");
		mockMvc.perform(post("/salary/attendance/{id}/adjust", attendance.getId())
				.session(teacherSession)
				.param("year", "2026")
				.param("month", "6")
				.param("manualRemark", "加課")
				.param("manualHours", "2"))
				.andExpect(status().is3xxRedirection());
		assertThat(teacherAttendanceRepository.findById(attendance.getId()).orElseThrow().isManualAdjusted())
				.isFalse();

		var regularSalaryResult = mockMvc.perform(get("/salary")
				.session(teacherSession)
				.param("year", "2026")
				.param("month", "6"))
				.andExpect(status().isOk())
				.andReturn();
		String regularHtml = regularSalaryResult.getResponse().getContentAsString();
		assertThat(regularHtml).contains("我的打卡紀錄").doesNotContain("儲存調整");

		MockHttpSession directorSession = login(directorUsername, "DirectorPassword123");
		mockMvc.perform(post("/salary/attendance/{id}/adjust", attendance.getId())
				.session(directorSession)
				.param("year", "2026")
				.param("month", "6")
				.param("manualRemark", "加課")
				.param("manualHours", "2"))
				.andExpect(status().is3xxRedirection());

		TeacherAttendance updated = teacherAttendanceRepository.findById(attendance.getId()).orElseThrow();
		assertThat(updated.isManualAdjusted()).isTrue();
		assertThat(updated.getManualRemark()).isEqualTo("加課");
		assertThat(updated.getManualHours()).isEqualByComparingTo("2.00");
		assertThat(updated.getAdjustedByTeacherId()).isEqualTo(director.getId());
		assertThat(updated.getAdjustedAt()).isNotNull();
		assertThat(updated.getWorkMinutes()).isEqualTo(120);

		assertThat(teacherMonthlySalaryRepository
				.findByTeacherIdAndSalaryYearAndSalaryMonth(teacher.getId(), 2026, 6)
				.orElseThrow().getWorkMinutes()).isEqualTo(120);

		String directorHtml = mockMvc.perform(get("/salary")
				.session(directorSession)
				.param("year", "2026")
				.param("month", "6"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		assertThat(directorHtml)
				.contains("salaryAttendance_" + teacher.getId(), "儲存調整", "加課");
	}

	@Test
	void matchedOutlierAdjustmentAddsHoursToOriginalCourseHours() throws Exception {
		ClassRoom classRoom = createClassRoomWithSchedule(
				"星期四", LocalTime.of(18, 0), LocalTime.of(20, 0));
		attendance.setMatchedCourseId(classRoom.getSchedules().getFirst().getId());
		attendance.setMatchedCourseName(classRoom.getDisplayName());
		attendance.setMatchedCourseTimeText("18:00 ~ 20:00");
		attendance.setClockInTime(LocalTime.of(16, 30));
		attendance.setClockOutTime(LocalTime.of(20, 0));
		attendance.setWorkMinutes(120L);
		attendance = teacherAttendanceRepository.save(attendance);

		MockHttpSession directorSession = login(directorUsername, "DirectorPassword123");
		mockMvc.perform(post("/salary/attendance/{id}/adjust", attendance.getId())
				.session(directorSession)
				.param("year", "2026")
				.param("month", "6")
				.param("manualRemark", "提早備課")
				.param("manualHours", "2.5"))
				.andExpect(status().is3xxRedirection());

		TeacherAttendance updated = teacherAttendanceRepository.findById(attendance.getId()).orElseThrow();
		assertThat(updated.getManualHours()).isEqualByComparingTo("2.5");
		assertThat(updated.getWorkMinutes()).isEqualTo(270);

		assertThat(teacherMonthlySalaryRepository
				.findByTeacherIdAndSalaryYearAndSalaryMonth(teacher.getId(), 2026, 6)
				.orElseThrow().getWorkMinutes()).isEqualTo(270);
	}

	private Teacher createTeacher(String name, TeacherPosition position) {
		Teacher created = new Teacher();
		created.setName(name);
		created.setPosition(position);
		created.setStatus(TeacherStatus.ACTIVE);
		return teacherRepository.save(created);
	}

	private TeacherAccount createAccount(Teacher accountTeacher, String username, String password) {
		String salt = passwordHashService.newSalt();
		TeacherAccount account = new TeacherAccount();
		account.setTeacher(accountTeacher);
		account.setUsername(username);
		account.setPasswordSalt(salt);
		account.setPasswordHash(passwordHashService.hash(password, salt));
		return teacherAccountRepository.save(account);
	}

	private ClassRoom createClassRoomWithSchedule(String weekday, LocalTime startTime, LocalTime endTime) {
		ClassRoom classRoom = new ClassRoom();
		classRoom.setGrade("測試班");
		classRoom.setClassType("薪資補正");
		classRoom.setTeacher(teacher);
		classRoom.addSchedule(new ClassSchedule(weekday, startTime, endTime));
		return classRoomRepository.save(classRoom);
	}

	private MockHttpSession login(String username, String password) throws Exception {
		return (MockHttpSession) mockMvc.perform(post("/login")
				.param("username", username)
				.param("password", password))
				.andExpect(status().is3xxRedirection())
				.andReturn().getRequest().getSession(false);
	}

	private void deleteTeacherData(Teacher target) {
		if (target == null || teacherRepository.findById(target.getId()).isEmpty()) {
			return;
		}
		teacherAttendanceRepository.deleteByTeacherId(target.getId());
		teacherMonthlySalaryRepository.deleteByTeacherId(target.getId());
		teacherAccountRepository.deleteByTeacherId(target.getId());
		classRoomRepository.findByTeacherIdOrderByIdAsc(target.getId())
				.forEach(classRoomRepository::delete);
		teacherRepository.deleteById(target.getId());
	}

	private void cleanupStaleTestTeachers() {
		teacherRepository.findAll().stream()
				.filter(candidate -> "薪資測試主任".equals(candidate.getName())
						|| "薪資測試教師".equals(candidate.getName()))
				.forEach(this::deleteTeacherData);
	}
}
