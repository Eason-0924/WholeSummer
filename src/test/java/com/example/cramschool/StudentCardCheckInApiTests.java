package com.example.cramschool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import com.example.cramschool.entity.ClassRoom;
import com.example.cramschool.entity.ClassSchedule;
import com.example.cramschool.entity.ClassStudent;
import com.example.cramschool.entity.AttendanceStatus;
import com.example.cramschool.entity.Student;
import com.example.cramschool.entity.StudentAttendance;
import com.example.cramschool.entity.Subject;
import com.example.cramschool.entity.Teacher;
import com.example.cramschool.entity.TeacherAccount;
import com.example.cramschool.entity.TeacherAttendance;
import com.example.cramschool.entity.TeacherPosition;
import com.example.cramschool.entity.TeacherStatus;
import com.example.cramschool.repository.ClassRoomRepository;
import com.example.cramschool.repository.ClassStudentRepository;
import com.example.cramschool.repository.LineNotificationLogRepository;
import com.example.cramschool.repository.StudentAttendanceRepository;
import com.example.cramschool.repository.StudentRepository;
import com.example.cramschool.repository.SubjectRepository;
import com.example.cramschool.repository.TeacherAccountRepository;
import com.example.cramschool.repository.TeacherAttendanceRepository;
import com.example.cramschool.repository.TeacherMonthlySalaryRepository;
import com.example.cramschool.repository.TeacherRepository;
import com.example.cramschool.service.PasswordHashService;

@SpringBootTest
@AutoConfigureMockMvc
class StudentCardCheckInApiTests {

	private static final String TEST_PASSWORD = "card-api-password";
	private static final Map<DayOfWeek, String> WEEKDAY_NAMES = Map.of(
			DayOfWeek.MONDAY, "星期一",
			DayOfWeek.TUESDAY, "星期二",
			DayOfWeek.WEDNESDAY, "星期三",
			DayOfWeek.THURSDAY, "星期四",
			DayOfWeek.FRIDAY, "星期五",
			DayOfWeek.SATURDAY, "星期六",
			DayOfWeek.SUNDAY, "星期日");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private StudentRepository studentRepository;

	@Autowired
	private SubjectRepository subjectRepository;

	@Autowired
	private ClassRoomRepository classRoomRepository;

	@Autowired
	private ClassStudentRepository classStudentRepository;

	@Autowired
	private StudentAttendanceRepository studentAttendanceRepository;

	@Autowired
	private LineNotificationLogRepository lineNotificationLogRepository;

	@Autowired
	private TeacherRepository teacherRepository;

	@Autowired
	private TeacherAccountRepository teacherAccountRepository;

	@Autowired
	private TeacherAttendanceRepository teacherAttendanceRepository;

	@Autowired
	private TeacherMonthlySalaryRepository teacherMonthlySalaryRepository;

	@Autowired
	private PasswordHashService passwordHashService;

	private Teacher teacher;
	private TeacherAccount account;
	private Student student;
	private Student noClassStudent;
	private Subject subject;
	private ClassRoom classRoom;
	private ClassStudent classStudent;
	private String username;
	private String cardPrefix;

	@BeforeEach
	void setUp() {
		cardPrefix = "API" + System.nanoTime();
		username = "card-api-" + System.nanoTime();
		teacher = new Teacher();
		teacher.setName("刷卡 API 測試教師");
		teacher.setPosition(TeacherPosition.TEACHER);
		teacher.setStatus(TeacherStatus.ACTIVE);
		teacher.setCardId(cardPrefix + "TEACHER");
		teacher.setCardStatus("ACTIVE");
		teacher = teacherRepository.save(teacher);

		String salt = passwordHashService.newSalt();
		account = new TeacherAccount();
		account.setTeacher(teacher);
		account.setUsername(username);
		account.setPasswordSalt(salt);
		account.setPasswordHash(passwordHashService.hash(TEST_PASSWORD, salt));
		account = teacherAccountRepository.save(account);

		student = student("刷卡 API 測試學生", cardPrefix + "A1B2", "ACTIVE");
		noClassStudent = student("刷卡 API 無課學生", cardPrefix + "NOCLASS", "ACTIVE");
		subject = new Subject();
		subject.setName("刷卡 API 測試科目");
		subject.setActive(true);
		subject = subjectRepository.save(subject);

		classRoom = new ClassRoom();
		classRoom.setGrade("國一");
		classRoom.setSubject(subject);
		classRoom.setClassType("刷卡測試班");
		classRoom.setTeacher(teacher);
		classRoom.setActive(true);
		LocalTime now = LocalTime.now();
		classRoom.addSchedule(new ClassSchedule(WEEKDAY_NAMES.get(LocalDate.now().getDayOfWeek()),
				now.minusMinutes(10), now.plusMinutes(50)));
		classRoom = classRoomRepository.save(classRoom);

		classStudent = new ClassStudent();
		classStudent.setClassRoom(classRoom);
		classStudent.setStudent(student);
		classStudent.setActive(true);
		classStudent = classStudentRepository.save(classStudent);
	}

	@AfterEach
	void tearDown() {
		if (student != null && student.getId() != null) {
			lineNotificationLogRepository.deleteByStudentId(student.getId());
			studentAttendanceRepository.findByStudentIdOrderByAttendanceDateDescIdDesc(student.getId())
					.forEach(attendance -> studentAttendanceRepository.deleteById(attendance.getId()));
		}
		if (noClassStudent != null && noClassStudent.getId() != null) {
			lineNotificationLogRepository.deleteByStudentId(noClassStudent.getId());
			studentAttendanceRepository.findByStudentIdOrderByAttendanceDateDescIdDesc(noClassStudent.getId())
					.forEach(attendance -> studentAttendanceRepository.deleteById(attendance.getId()));
		}
		if (classStudent != null && classStudent.getId() != null) {
			classStudentRepository.deleteById(classStudent.getId());
		}
		if (classRoom != null && classRoom.getId() != null) {
			classRoomRepository.deleteById(classRoom.getId());
		}
		if (subject != null && subject.getId() != null) {
			subjectRepository.deleteById(subject.getId());
		}
		if (student != null && student.getId() != null) {
			studentRepository.deleteById(student.getId());
		}
		if (noClassStudent != null && noClassStudent.getId() != null) {
			studentRepository.deleteById(noClassStudent.getId());
		}
		if (account != null && account.getId() != null) {
			teacherAccountRepository.deleteById(account.getId());
		}
		if (teacher != null && teacher.getId() != null) {
			teacherAttendanceRepository.deleteByTeacherId(teacher.getId());
		}
		if (teacher != null && teacher.getId() != null) {
			teacherMonthlySalaryRepository.deleteByTeacherId(teacher.getId());
			teacherRepository.deleteById(teacher.getId());
		}
	}

	@Test
	void cardCheckInCreatesAttendanceRecord() throws Exception {
		MockHttpSession session = login();

		mockMvc.perform(post("/api/attendance/card-check-in")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"cardId\":\" " + cardPrefix.toLowerCase() + " a1 b2 \",\"deviceName\":\"web-checkin-page\"}")
				.session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.status").value("CHECKED_IN"))
				.andExpect(jsonPath("$.studentId").value(student.getId()))
				.andExpect(jsonPath("$.studentName").value("刷卡 API 測試學生"))
				.andExpect(jsonPath("$.className").value("國一刷卡 API 測試科目（刷卡測試班）"))
				.andExpect(jsonPath("$.cardId").value(cardPrefix + "A1B2"));

		List<StudentAttendance> attendances = studentAttendanceRepository.findByStudentIdOrderByAttendanceDateDescIdDesc(
				student.getId());
		assertThat(attendances).hasSize(1);
		StudentAttendance attendance = attendances.getFirst();
		assertThat(attendance.getClassRoom().getId()).isEqualTo(classRoom.getId());
		assertThat(attendance.getCheckMethod()).isEqualTo("CARD");
		assertThat(attendance.getDeviceName()).isEqualTo("web-checkin-page");
		assertThat(attendance.getCardId()).isEqualTo(cardPrefix + "A1B2");
		assertThat(attendance.getCheckInTime()).isNotNull();
		assertThat(attendance.getCheckOutTime()).isNull();
		assertThat(attendance.getStatus()).isEqualTo(AttendanceStatus.LATE);
		assertThat(attendance.getNote()).startsWith("到班時間：");
	}

	@Test
	void cardCheckInReturnsCardNotBound() throws Exception {
		mockMvc.perform(post("/api/attendance/card-check-in")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"cardId\":\"" + cardPrefix + "UNKNOWN\"}")
				.session(login()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.status").value("CARD_NOT_BOUND"))
				.andExpect(jsonPath("$.cardId").value(cardPrefix + "UNKNOWN"));
	}

	@Test
	void cardCheckInReturnsNoClassToday() throws Exception {
		mockMvc.perform(post("/api/attendance/card-check-in")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"cardId\":\"" + cardPrefix + "NOCLASS\"}")
				.session(login()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.status").value("NO_CLASS_TODAY"))
				.andExpect(jsonPath("$.studentId").value(noClassStudent.getId()));
	}

	@Test
	void secondStudentCardCheckInSetsCheckOutTime() throws Exception {
		MockHttpSession session = login();
		String content = "{\"cardId\":\"" + cardPrefix + "A1B2\"}";
		mockMvc.perform(post("/api/attendance/card-check-in")
				.contentType(MediaType.APPLICATION_JSON)
				.content(content)
				.session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CHECKED_IN"));

		mockMvc.perform(post("/api/attendance/card-check-in")
				.contentType(MediaType.APPLICATION_JSON)
				.content(content)
				.session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.status").value("CHECKED_OUT"))
				.andExpect(jsonPath("$.studentId").value(student.getId()))
				.andExpect(jsonPath("$.checkOutTime").exists());

		List<StudentAttendance> attendances = studentAttendanceRepository.findByStudentIdOrderByAttendanceDateDescIdDesc(
				student.getId());
		assertThat(attendances).hasSize(1);
		assertThat(attendances.getFirst().getCheckInTime()).isNotNull();
		assertThat(attendances.getFirst().getCheckOutTime()).isNotNull();
		assertThat(attendances.getFirst().getStatus().name()).isIn("PRESENT", "LATE", "ABSENT", "LEAVE");
	}

	@Test
	void thirdStudentCardCheckInReturnsDuplicate() throws Exception {
		MockHttpSession session = login();
		String content = "{\"cardId\":\"" + cardPrefix + "A1B2\"}";
		mockMvc.perform(post("/api/attendance/card-check-in")
				.contentType(MediaType.APPLICATION_JSON)
				.content(content)
				.session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CHECKED_IN"));
		mockMvc.perform(post("/api/attendance/card-check-in")
				.contentType(MediaType.APPLICATION_JSON)
				.content(content)
				.session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CHECKED_OUT"));

		mockMvc.perform(post("/api/attendance/card-check-in")
				.contentType(MediaType.APPLICATION_JSON)
				.content(content)
				.session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.status").value("DUPLICATE_CHECK_IN"));
	}

	@Test
	void cardCheckInReturnsCardDisabled() throws Exception {
		student.setCardStatus("DISABLED");
		studentRepository.save(student);

		mockMvc.perform(post("/api/attendance/card-check-in")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"cardId\":\"" + cardPrefix + "A1B2\"}")
				.session(login()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.status").value("CARD_DISABLED"));
	}

	@Test
	void teacherCardCheckInCreatesTeacherAttendance() throws Exception {
		MockHttpSession session = login();

		mockMvc.perform(post("/api/attendance/card-check-in")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"cardId\":\"" + cardPrefix + "TEACHER\",\"deviceName\":\"teacher-card-page\"}")
				.session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.personType").value("TEACHER"))
				.andExpect(jsonPath("$.status").value("CLOCKED_IN"))
				.andExpect(jsonPath("$.teacherId").value(teacher.getId()))
				.andExpect(jsonPath("$.teacherName").value("刷卡 API 測試教師"))
				.andExpect(jsonPath("$.cardId").value(cardPrefix + "TEACHER"));

		TeacherAttendance attendance = teacherAttendanceRepository.findByTeacherIdAndDate(teacher.getId(), LocalDate.now())
				.orElseThrow();
		assertThat(attendance.getClockInTime()).isNotNull();
		assertThat(attendance.getCheckMethod()).isEqualTo("CARD");
		assertThat(attendance.getDeviceName()).isEqualTo("teacher-card-page");
		assertThat(attendance.getCardId()).isEqualTo(cardPrefix + "TEACHER");
	}

	@Test
	void secondTeacherCardCheckInClocksOut() throws Exception {
		MockHttpSession session = login();
		String content = "{\"cardId\":\"" + cardPrefix + "TEACHER\"}";
		mockMvc.perform(post("/api/attendance/card-check-in")
				.contentType(MediaType.APPLICATION_JSON)
				.content(content)
				.session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CLOCKED_IN"));

		mockMvc.perform(post("/api/attendance/card-check-in")
				.contentType(MediaType.APPLICATION_JSON)
				.content(content)
				.session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CLOCKED_OUT"));

		TeacherAttendance attendance = teacherAttendanceRepository.findByTeacherIdAndDate(teacher.getId(), LocalDate.now())
				.orElseThrow();
		assertThat(attendance.getClockInTime()).isNotNull();
		assertThat(attendance.getClockOutTime()).isNotNull();
	}

	@Test
	void thirdTeacherCardCheckInReturnsDuplicateWithoutRollbackOnlyError() throws Exception {
		MockHttpSession session = login();
		String content = "{\"cardId\":\"" + cardPrefix + "TEACHER\"}";
		mockMvc.perform(post("/api/attendance/card-check-in")
				.contentType(MediaType.APPLICATION_JSON)
				.content(content)
				.session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CLOCKED_IN"));
		mockMvc.perform(post("/api/attendance/card-check-in")
				.contentType(MediaType.APPLICATION_JSON)
				.content(content)
				.session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CLOCKED_OUT"));

		mockMvc.perform(post("/api/attendance/card-check-in")
				.contentType(MediaType.APPLICATION_JSON)
				.content(content)
				.session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.status").value("DUPLICATE_CHECK_IN"))
				.andExpect(jsonPath("$.message").value("今日已完成上下班打卡"));
	}

	private Student student(String name, String cardId, String cardStatus) {
		Student student = new Student();
		student.setChineseName(name);
		student.setActive(true);
		student.setCardId(cardId);
		student.setCardStatus(cardStatus);
		return studentRepository.save(student);
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
