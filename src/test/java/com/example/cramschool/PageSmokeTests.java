package com.example.cramschool;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import com.example.cramschool.repository.ClassRoomRepository;
import com.example.cramschool.repository.ClassScheduleRepository;
import com.example.cramschool.repository.ExamRepository;
import com.example.cramschool.repository.HomeworkRepository;
import com.example.cramschool.repository.MakeUpClassRequestRepository;
import com.example.cramschool.repository.StudentRepository;
import com.example.cramschool.repository.SubjectRepository;
import com.example.cramschool.repository.TeacherRepository;
import com.example.cramschool.repository.TeacherAccountRepository;
import com.example.cramschool.repository.TeacherMonthlySalaryRepository;
import com.example.cramschool.entity.ClassRoom;
import com.example.cramschool.entity.ClassSchedule;
import com.example.cramschool.entity.Exam;
import com.example.cramschool.entity.MakeUpClassRequest;
import com.example.cramschool.entity.MakeUpSourceType;
import com.example.cramschool.entity.MakeUpStatus;
import com.example.cramschool.entity.ScheduleType;
import com.example.cramschool.entity.Subject;
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
	private TeacherMonthlySalaryRepository teacherMonthlySalaryRepository;

	@Autowired
	private PasswordHashService passwordHashService;

	private Teacher testTeacher;
	private TeacherAccount testAccount;
	private String testUsername;
	private static final String TEST_PASSWORD = "page-smoke-password";

	@Autowired
	private ClassRoomRepository classRoomRepository;

	@Autowired
	private ClassScheduleRepository classScheduleRepository;

	@Autowired
	private SubjectRepository subjectRepository;

	@Autowired
	private ExamRepository examRepository;

	@Autowired
	private HomeworkRepository homeworkRepository;

	@Autowired
	private MakeUpClassRequestRepository makeUpClassRequestRepository;

	private ClassRoom makeUpClassRoom;
	private MakeUpClassRequest makeUpRequest;
	private Subject makeUpSubject;
	private ClassSchedule directReschedule;
	private ClassSchedule directRescheduleCancellation;
	private ClassRoom uploadedExamClassRoom;
	private Subject uploadedExamSubject;
	private Exam uploadedExam;

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
		if (uploadedExam != null) {
			examRepository.findById(uploadedExam.getId()).ifPresent(exam -> {
				if (exam.getPaperFilePath() != null && !exam.getPaperFilePath().isBlank()) {
					try {
						Files.deleteIfExists(Path.of(exam.getPaperFilePath()));
					} catch (java.io.IOException ignored) {
					}
				}
				examRepository.deleteById(exam.getId());
			});
		}
		if (uploadedExamClassRoom != null) {
			classRoomRepository.deleteById(uploadedExamClassRoom.getId());
		}
		if (uploadedExamSubject != null) {
			subjectRepository.deleteById(uploadedExamSubject.getId());
		}
		if (makeUpRequest != null) {
			makeUpClassRequestRepository.deleteById(makeUpRequest.getId());
		}
		if (directReschedule != null) {
			classScheduleRepository.findById(directReschedule.getId()).ifPresent(classScheduleRepository::delete);
		}
		if (directRescheduleCancellation != null) {
			classScheduleRepository.findById(directRescheduleCancellation.getId()).ifPresent(classScheduleRepository::delete);
		}
		if (makeUpClassRoom != null) {
			classRoomRepository.deleteById(makeUpClassRoom.getId());
		}
		if (makeUpSubject != null) {
			subjectRepository.deleteById(makeUpSubject.getId());
		}
		if (testAccount != null) {
			teacherAccountRepository.deleteById(testAccount.getId());
		}
		if (testTeacher != null) {
			teacherMonthlySalaryRepository.deleteByTeacherId(testTeacher.getId());
			teacherRepository.deleteById(testTeacher.getId());
		}
	}

	@Test
	void examPaperUploadExtractsSelectedPdfPagesAndCanOpenFolder() throws Exception {
		MockHttpSession session = (MockHttpSession) mockMvc.perform(post("/login")
				.param("username", testUsername)
				.param("password", TEST_PASSWORD))
				.andExpect(status().is3xxRedirection())
				.andReturn()
				.getRequest()
				.getSession(false);
		createUploadedExamClassRoom();

		MockMultipartFile paperFile = new MockMultipartFile(
				"paperFile",
				"math-paper.pdf",
				"application/pdf",
				testPdfBytes(3));

		mockMvc.perform(multipart("/exams")
				.file(paperFile)
				.param("classRoomId", uploadedExamClassRoom.getId().toString())
				.param("subjectId", uploadedExamSubject.getId().toString())
				.param("name", "頁測考卷上傳")
				.param("examDate", LocalDate.now().toString())
				.param("fullScore", "100")
				.param("description", "含考卷檔案")
				.param("paperPageSelection", "2-3")
				.session(session))
				.andExpect(status().is3xxRedirection());

		uploadedExam = examRepository.findAllByOrderByExamDateDescIdDesc().stream()
				.filter(exam -> "頁測考卷上傳".equals(exam.getName()))
				.findFirst()
				.orElseThrow();
		org.assertj.core.api.Assertions.assertThat(Files.isRegularFile(Path.of(uploadedExam.getPaperFilePath()))).isTrue();
		org.assertj.core.api.Assertions.assertThat(uploadedExam.getPaperFilePath())
				.contains(uploadedExamClassRoom.getDisplayName())
				.contains("測驗考卷")
				.contains("頁測考卷上傳");

		mockMvc.perform(get("/exams/{id}", uploadedExam.getId()).session(session))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("math-paper_pages_2-3.pdf")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(uploadedExam.getPaperFilePath())))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("考卷頁數：2-3")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("開啟資料夾")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("/exams/" + uploadedExam.getId() + "/paper/folder")));

		try (PDDocument document = Loader.loadPDF(Path.of(uploadedExam.getPaperFilePath()).toFile())) {
			org.assertj.core.api.Assertions.assertThat(document.getNumberOfPages()).isEqualTo(2);
		}
	}

	@Test
	void deletingExamDeletesStoredPaperFile() throws Exception {
		MockHttpSession session = (MockHttpSession) mockMvc.perform(post("/login")
				.param("username", testUsername)
				.param("password", TEST_PASSWORD))
				.andExpect(status().is3xxRedirection())
				.andReturn()
				.getRequest()
				.getSession(false);
		createUploadedExamClassRoom();

		MockMultipartFile paperFile = new MockMultipartFile(
				"paperFile",
				"delete-me.pdf",
				"application/pdf",
				testPdfBytes(2));

		mockMvc.perform(multipart("/exams")
				.file(paperFile)
				.param("classRoomId", uploadedExamClassRoom.getId().toString())
				.param("subjectId", uploadedExamSubject.getId().toString())
				.param("name", "刪除考卷檔案測試")
				.param("examDate", LocalDate.now().toString())
				.param("fullScore", "100")
				.param("description", "")
				.param("paperPageSelection", "")
				.session(session))
				.andExpect(status().is3xxRedirection());

		uploadedExam = examRepository.findAllByOrderByExamDateDescIdDesc().stream()
				.filter(exam -> "刪除考卷檔案測試".equals(exam.getName()))
				.findFirst()
				.orElseThrow();
		Path storedPaperPath = Path.of(uploadedExam.getPaperFilePath());
		Path storedPaperFolder = storedPaperPath.getParent();
		org.assertj.core.api.Assertions.assertThat(Files.isRegularFile(storedPaperPath)).isTrue();
		org.assertj.core.api.Assertions.assertThat(Files.isDirectory(storedPaperFolder)).isTrue();

		mockMvc.perform(post("/exams/{id}/delete", uploadedExam.getId()).session(session))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/exams"));

		org.assertj.core.api.Assertions.assertThat(examRepository.existsById(uploadedExam.getId())).isFalse();
		org.assertj.core.api.Assertions.assertThat(Files.exists(storedPaperPath)).isFalse();
		org.assertj.core.api.Assertions.assertThat(Files.exists(storedPaperFolder)).isFalse();
		uploadedExam = null;
	}

	@Test
	void classAttendanceDateShowsWarningForNonClassDays() throws Exception {
		MockHttpSession session = (MockHttpSession) mockMvc.perform(post("/login")
				.param("username", testUsername)
				.param("password", TEST_PASSWORD))
				.andExpect(status().is3xxRedirection())
				.andReturn()
				.getRequest()
				.getSession(false);
		createUploadedExamClassRoom();
		String classRoomPath = uploadedExamClassRoom.getUrlSlug() == null
				? String.valueOf(uploadedExamClassRoom.getId())
				: uploadedExamClassRoom.getUrlSlug();

		mockMvc.perform(get("/classes/{slug}/attendance", classRoomPath)
				.param("date", "2026-06-30")
				.session(session))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"2026-06-30\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("date=2026-06-29")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("date=2026-07-02")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("此日期不是上課日")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("data-class-weekdays=\"1,4\"")));
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
				"/classes",
				"/classes/new",
				"/subjects",
				"/subjects/new",
				"/exams",
				"/homeworks",
				"/line-notifications",
				"/salary",
				"/settings"
		};

		mockMvc.perform(get("/register")).andExpect(status().isOk());

		for (String path : paths) {
			mockMvc.perform(get(path).session(session))
					.andExpect(status().isOk());
		}
		mockMvc.perform(get("/teachers/attendance").session(session))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/attendance/my"));
		mockMvc.perform(get("/backup").session(session))
				.andExpect(status().is3xxRedirection());

		if (!studentRepository.findAll().isEmpty()) {
			var student = studentRepository.findAll().getFirst();
			mockMvc.perform(get("/students/{slug}", student.getUrlSlug()).session(session)).andExpect(status().isOk());
			mockMvc.perform(get("/students/{slug}/edit", student.getUrlSlug()).session(session)).andExpect(status().isOk());
			mockMvc.perform(get("/students/{id}", student.getId()).session(session))
					.andExpect(status().is3xxRedirection())
					.andExpect(redirectedUrl("/students/" + student.getUrlSlug()));
		}
		if (!teacherRepository.findAll().isEmpty()) {
			var teacher = teacherRepository.findAll().getFirst();
			String teacherPath = teacher.getUrlSlug() == null ? String.valueOf(teacher.getId()) : teacher.getUrlSlug();
			mockMvc.perform(get("/teachers/{slug}", teacherPath).session(session)).andExpect(status().isOk());
			if (teacher.getUrlSlug() != null) {
				mockMvc.perform(get("/teachers/{id}", teacher.getId()).session(session))
						.andExpect(status().is3xxRedirection())
						.andExpect(redirectedUrl("/teachers/" + teacher.getUrlSlug()));
			}
			String editableTeacherPath = testTeacher.getUrlSlug() == null
					? String.valueOf(testTeacher.getId())
					: testTeacher.getUrlSlug();
			mockMvc.perform(get("/teachers/{slug}/edit", editableTeacherPath).session(session))
					.andExpect(status().isOk());
		}
		if (!classRoomRepository.findAll().isEmpty()) {
			var classRoom = classRoomRepository.findAll().getFirst();
			String classRoomPath = classRoom.getUrlSlug() == null ? String.valueOf(classRoom.getId()) : classRoom.getUrlSlug();
			mockMvc.perform(get("/classes/{slug}", classRoomPath).session(session)).andExpect(status().isOk());
			mockMvc.perform(get("/classes/{slug}/edit", classRoomPath).session(session)).andExpect(status().isOk());
			mockMvc.perform(get("/classes/{slug}/attendance", classRoomPath).session(session)).andExpect(status().isOk());
			if (classRoom.getUrlSlug() != null) {
				mockMvc.perform(get("/classes/{id}", classRoom.getId()).session(session))
						.andExpect(status().is3xxRedirection())
						.andExpect(redirectedUrl("/classes/" + classRoom.getUrlSlug()));
			}
		}
		if (!subjectRepository.findAll().isEmpty()) {
			var subject = subjectRepository.findAll().getFirst();
			String subjectPath = subject.getUrlSlug() == null ? String.valueOf(subject.getId()) : subject.getUrlSlug();
			mockMvc.perform(get("/subjects/{slug}/edit", subjectPath).session(session)).andExpect(status().isOk());
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

	@Test
	void makeUpDetailRendersCalendarAndLoadsSlotsLazily() throws Exception {
		MockHttpSession session = (MockHttpSession) mockMvc.perform(post("/login")
				.param("username", testUsername)
				.param("password", TEST_PASSWORD))
				.andExpect(status().is3xxRedirection())
				.andReturn()
				.getRequest()
				.getSession(false);
		makeUpRequest = createMakeUpRequest();

		mockMvc.perform(get("/make-up/{id}", makeUpRequest.getId()).session(session))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("data-calendar-url")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("data-slots-url")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("make-up-day-count-success")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("make-up-day-count-warning")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("make-up-day-count-danger")))
				.andExpect(content().string(org.hamcrest.Matchers.not(
						org.hamcrest.Matchers.containsString("list-group-item-success"))));

		mockMvc.perform(get("/make-up/{id}/slots", makeUpRequest.getId())
				.param("date", LocalDate.now().plusDays(1).toString())
				.session(session))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("timeText")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("statusLabel")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("statusClass")));

		mockMvc.perform(get("/make-up/{id}/calendar", makeUpRequest.getId()).session(session))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("statusClass")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("availableCount")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("teacherConflictCount")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("studentConflictCount")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("success")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("calculated")));
	}

	@Test
	void makeUpIndexRendersPendingAndScheduledColumns() throws Exception {
		MockHttpSession session = (MockHttpSession) mockMvc.perform(post("/login")
				.param("username", testUsername)
				.param("password", TEST_PASSWORD))
				.andExpect(status().is3xxRedirection())
				.andReturn()
				.getRequest()
				.getSession(false);
		makeUpRequest = createMakeUpRequest();
		createDirectReschedule();

		mockMvc.perform(get("/make-up").session(session))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("補課/調課")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("我的補課需求")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("補課紀錄")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("安排補課時間")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("段考調整")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("重新安排時間")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("忽略補課")));
	}

	@Test
	void directRescheduleEditRendersCalendarAndLoadsSlots() throws Exception {
		MockHttpSession session = (MockHttpSession) mockMvc.perform(post("/login")
				.param("username", testUsername)
				.param("password", TEST_PASSWORD))
				.andExpect(status().is3xxRedirection())
				.andReturn()
				.getRequest()
				.getSession(false);
		createDirectReschedule();

		mockMvc.perform(get("/make-up/reschedules/{id}/edit", directReschedule.getId()).session(session))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("重新設定調課時間")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("data-calendar-url")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("data-slots-url")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("make-up-day-count-success")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"newStart\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("段考調整")));

		mockMvc.perform(get("/make-up/reschedules/{id}/slots", directReschedule.getId())
				.param("date", LocalDate.now().plusDays(1).toString())
				.session(session))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("timeText")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("statusLabel")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("statusClass")));

		mockMvc.perform(get("/make-up/reschedules/{id}/calendar", directReschedule.getId()).session(session))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("statusClass")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("availableCount")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("calculated")));
	}

	private MakeUpClassRequest createMakeUpRequest() {
		makeUpSubject = new Subject();
		makeUpSubject.setName("補課頁測試科目");
		makeUpSubject = subjectRepository.save(makeUpSubject);

		makeUpClassRoom = new ClassRoom();
		makeUpClassRoom.setGrade("頁測");
		makeUpClassRoom.setSubject(makeUpSubject);
		makeUpClassRoom.setClassType("補課測試");
		makeUpClassRoom.setTeacher(testTeacher);
		makeUpClassRoom.addSchedule(new ClassSchedule("星期一", LocalTime.of(18, 0), LocalTime.of(20, 0)));
		makeUpClassRoom = classRoomRepository.save(makeUpClassRoom);

		MakeUpClassRequest request = new MakeUpClassRequest();
		request.setOriginalCourseSchedule(makeUpClassRoom.getSchedules().getFirst());
		request.setOriginalCourseDate(LocalDate.now());
		request.setTeacher(testTeacher);
		request.setClassRoom(makeUpClassRoom);
		request.setSourceType(MakeUpSourceType.LEAVE);
		request.setSourceRecordId(System.nanoTime());
		request.setStatus(MakeUpStatus.PENDING);
		return makeUpClassRequestRepository.save(request);
	}

	private void createDirectReschedule() {
		if (makeUpClassRoom == null) {
			makeUpSubject = new Subject();
			makeUpSubject.setName("調課頁測試科目");
			makeUpSubject = subjectRepository.save(makeUpSubject);

			makeUpClassRoom = new ClassRoom();
			makeUpClassRoom.setGrade("頁測");
			makeUpClassRoom.setSubject(makeUpSubject);
			makeUpClassRoom.setClassType("調課測試");
			makeUpClassRoom.setTeacher(testTeacher);
			makeUpClassRoom.addSchedule(new ClassSchedule("星期二", LocalTime.of(18, 0), LocalTime.of(20, 0)));
			makeUpClassRoom = classRoomRepository.save(makeUpClassRoom);
		}
		ClassSchedule original = makeUpClassRoom.getSchedules().getFirst();

		LocalDate originalDate = LocalDate.now().plusDays(2);
		directRescheduleCancellation = new ClassSchedule("星期二", LocalTime.of(18, 0), LocalTime.of(20, 0));
		directRescheduleCancellation.setClassRoom(makeUpClassRoom);
		directRescheduleCancellation.setScheduleType(ScheduleType.CANCELLED);
		directRescheduleCancellation.setOriginalSchedule(original);
		directRescheduleCancellation.setCourseDate(originalDate);
		directRescheduleCancellation.setScheduledStartAt(LocalDateTime.of(originalDate, LocalTime.of(18, 0)));
		directRescheduleCancellation.setScheduledEndAt(LocalDateTime.of(originalDate, LocalTime.of(20, 0)));
		directRescheduleCancellation.setRescheduleReason("段考調整");
		directRescheduleCancellation = classScheduleRepository.save(directRescheduleCancellation);

		LocalDate rescheduleDate = LocalDate.now().plusDays(3);
		directReschedule = new ClassSchedule("星期三", LocalTime.of(18, 0), LocalTime.of(20, 0));
		directReschedule.setClassRoom(makeUpClassRoom);
		directReschedule.setScheduleType(ScheduleType.RESCHEDULED);
		directReschedule.setOriginalSchedule(original);
		directReschedule.setCourseDate(rescheduleDate);
		directReschedule.setScheduledStartAt(LocalDateTime.of(rescheduleDate, LocalTime.of(18, 0)));
		directReschedule.setScheduledEndAt(LocalDateTime.of(rescheduleDate, LocalTime.of(20, 0)));
		directReschedule.setRescheduleReason("段考調整");
		directReschedule = classScheduleRepository.save(directReschedule);
	}

	private void createUploadedExamClassRoom() {
		uploadedExamSubject = new Subject();
		uploadedExamSubject.setName("考卷上傳頁測科目");
		uploadedExamSubject = subjectRepository.save(uploadedExamSubject);

		uploadedExamClassRoom = new ClassRoom();
		uploadedExamClassRoom.setGrade("頁測");
		uploadedExamClassRoom.setSubject(uploadedExamSubject);
		uploadedExamClassRoom.setClassType("考卷上傳測試");
		uploadedExamClassRoom.setTeacher(testTeacher);
		uploadedExamClassRoom.addSchedule(new ClassSchedule("星期一", LocalTime.of(18, 0), LocalTime.of(20, 0)));
		uploadedExamClassRoom.addSchedule(new ClassSchedule("星期四", LocalTime.of(18, 0), LocalTime.of(20, 0)));
		uploadedExamClassRoom = classRoomRepository.save(uploadedExamClassRoom);
	}

	private byte[] testPdfBytes(int pageCount) throws Exception {
		try (PDDocument document = new PDDocument();
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			for (int i = 0; i < pageCount; i += 1) {
				document.addPage(new PDPage());
			}
			document.save(outputStream);
			return outputStream.toByteArray();
		}
	}
}
