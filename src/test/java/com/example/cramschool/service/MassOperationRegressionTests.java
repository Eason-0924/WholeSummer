package com.example.cramschool.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.dto.CardCheckInRequest;
import com.example.cramschool.entity.AttendanceStatus;
import com.example.cramschool.entity.ClassRoom;
import com.example.cramschool.entity.ClassSchedule;
import com.example.cramschool.entity.ClassStudent;
import com.example.cramschool.entity.Exam;
import com.example.cramschool.entity.Homework;
import com.example.cramschool.entity.MakeUpClassRequest;
import com.example.cramschool.entity.MakeUpSourceType;
import com.example.cramschool.entity.MakeUpStatus;
import com.example.cramschool.entity.Student;
import com.example.cramschool.entity.Subject;
import com.example.cramschool.entity.Teacher;
import com.example.cramschool.entity.TeacherAttendanceStatus;
import com.example.cramschool.entity.TeacherPosition;
import com.example.cramschool.entity.TeacherStatus;
import com.example.cramschool.form.ClassRoomForm;
import com.example.cramschool.form.ClassRoomForm.ScheduleEntryForm;
import com.example.cramschool.form.ExamForm;
import com.example.cramschool.form.HomeworkForm;
import com.example.cramschool.form.StudentAttendanceEntryForm;
import com.example.cramschool.form.StudentAttendanceForm;
import com.example.cramschool.form.StudentForm;
import com.example.cramschool.form.SubjectForm;
import com.example.cramschool.form.TeacherForm;
import com.example.cramschool.repository.ClassRoomRepository;
import com.example.cramschool.repository.ClassStudentRepository;
import com.example.cramschool.repository.HomeworkRecordRepository;
import com.example.cramschool.repository.MakeUpClassRequestRepository;
import com.example.cramschool.repository.StudentAttendanceRepository;
import com.example.cramschool.repository.TeacherAttendanceRepository;
import com.example.cramschool.repository.TeacherRepository;

@SpringBootTest
@Transactional
class MassOperationRegressionTests {

	private static final ZoneId TAIPEI = ZoneId.of("Asia/Taipei");
	private static final Path REPORT_PATH = Path.of("target/test-reports/mass-operation-test-report.md");

	@Autowired
	private TeacherRepository teacherRepository;
	@Autowired
	private TeacherService teacherService;
	@Autowired
	private SubjectService subjectService;
	@Autowired
	private ClassRoomService classRoomService;
	@Autowired
	private StudentService studentService;
	@Autowired
	private ClassStudentService classStudentService;
	@Autowired
	private ExamService examService;
	@Autowired
	private HomeworkService homeworkService;
	@Autowired
	private StudentAttendanceService studentAttendanceService;
	@Autowired
	private LeaveService leaveService;
	@Autowired
	private AbsenceService absenceService;
	@Autowired
	private MakeUpClassService makeUpClassService;
	@Autowired
	private WeeklyScheduleService weeklyScheduleService;
	@Autowired
	private ClassRoomRepository classRoomRepository;
	@Autowired
	private ClassStudentRepository classStudentRepository;
	@Autowired
	private StudentAttendanceRepository studentAttendanceRepository;
	@Autowired
	private TeacherAttendanceRepository teacherAttendanceRepository;
	@Autowired
	private MakeUpClassRequestRepository makeUpClassRequestRepository;
	@Autowired
	private HomeworkRecordRepository homeworkRecordRepository;

	private final List<OperationRecord> records = new ArrayList<>();
	private final AtomicInteger stepCounter = new AtomicInteger();

	@AfterEach
	void writeReport() throws Exception {
		Files.createDirectories(REPORT_PATH.getParent());
		List<String> lines = new ArrayList<>();
		lines.add("# 大量操作回歸測試報告");
		lines.add("");
		lines.add("| # | 操作 | 結果 | 紀錄 |");
		lines.add("|---:|---|---|---|");
		for (OperationRecord record : records) {
			lines.add("| " + record.step() + " | " + escape(record.name()) + " | "
					+ (record.success() ? "PASS" : "FAIL") + " | " + escape(record.detail()) + " |");
		}
		Files.write(REPORT_PATH, lines);
		studentAttendanceService.setClock(null);
	}

	@Test
	void massOperationsCreateReportForEveryStep() {
		LocalDate courseDate = LocalDate.now().plusDays(1);
		String weekday = weekdayName(courseDate);
		String runId = String.valueOf(System.nanoTime());

		Teacher director = step("建立主任測試帳號", () -> {
			Teacher teacher = new Teacher();
			teacher.setName("大量測試主任-" + runId);
			teacher.setPosition(TeacherPosition.DIRECTOR);
			teacher.setStatus(TeacherStatus.ACTIVE);
			return teacherRepository.save(teacher);
		}, teacher -> "teacherId=" + teacher.getId());

		Teacher teacher = step("新增教師", () -> teacherService.create(teacherForm("大量測試教師-" + runId), director.getId()),
				created -> "teacherId=" + created.getId());
		step("綁定教師卡片", () -> teacherService.bindCard(teacher.getId(), "T-" + runId, true, director.getId()),
				updated -> "cardId=" + updated.getCardId());

		Teacher absenceTeacher = step("新增缺席測試教師",
				() -> teacherService.create(teacherForm("大量測試缺席教師-" + runId), director.getId()),
				created -> "teacherId=" + created.getId());

		Subject subject = step("新增科目", () -> subjectService.create(subjectForm("大量測試科目-" + runId, teacher.getId())),
				created -> "subjectId=" + created.getId());
		step("停用科目", () -> {
			subjectService.deactivate(subject.getId());
			return subjectService.findById(subject.getId());
		}, updated -> "active=" + updated.isActive());
		step("啟用科目", () -> {
			subjectService.activate(subject.getId());
			return subjectService.findById(subject.getId());
		}, updated -> "active=" + updated.isActive());

		ClassRoom classRoom = step("新增班級", () -> classRoomService.create(
				classRoomForm(subject.getId(), teacher.getId(), weekday, LocalTime.of(18, 0), LocalTime.of(20, 0)),
				director.getId()), created -> "classRoomId=" + created.getId());

		ClassRoom absenceClassRoom = step("新增缺席測試班級", () -> classRoomService.create(
				classRoomForm(subject.getId(), absenceTeacher.getId(), weekday, LocalTime.of(8, 0), LocalTime.of(9, 0)),
				director.getId()), created -> "classRoomId=" + created.getId());

		Student firstStudent = step("新增學生 A", () -> studentService.create(studentForm("大量測試學生A-" + runId), director.getId()),
				created -> "studentId=" + created.getId());
		Student secondStudent = step("新增學生 B", () -> studentService.create(studentForm("大量測試學生B-" + runId), director.getId()),
				created -> "studentId=" + created.getId());
		step("綁定學生卡片", () -> studentService.bindCard(firstStudent.getId(), "S-" + runId, true, director.getId()),
				updated -> "cardId=" + updated.getCardId());

		step("學生 A 加入班級", () -> {
			classStudentService.addStudent(classRoom.getId(), firstStudent.getId(), director.getId());
			return classStudentRepository.findByClassRoomIdAndStudentId(classRoom.getId(), firstStudent.getId()).orElseThrow();
		}, membership -> "classStudentId=" + membership.getId());
		step("學生 B 加入班級", () -> {
			classStudentService.addStudent(classRoom.getId(), secondStudent.getId(), director.getId());
			return classStudentRepository.findByClassRoomIdAndStudentId(classRoom.getId(), secondStudent.getId()).orElseThrow();
		}, membership -> "classStudentId=" + membership.getId());

		Exam exam = step("新增測驗", () -> examService.create(examForm(classRoom.getId(), "大量測試測驗-" + runId, courseDate)),
				created -> "examId=" + created.getId());
		step("更新測驗", () -> {
			ExamForm form = examForm(classRoom.getId(), "大量測試測驗更新-" + runId, courseDate);
			form.setFullScore(120);
			return examService.update(exam.getId(), form);
		}, updated -> "fullScore=" + updated.getFullScore());

		Homework homework = step("新增作業", () -> homeworkService.create(
				homeworkForm(classRoom.getId(), "大量測試作業-" + runId, courseDate)),
				created -> "homeworkId=" + created.getId() + ", records="
						+ homeworkRecordRepository.countByHomeworkId(created.getId()));
		step("更新作業", () -> homeworkService.update(homework.getId(),
				homeworkForm(classRoom.getId(), "大量測試作業更新-" + runId, courseDate.plusDays(1))),
				updated -> "dueDate=" + updated.getDueDate());

		step("學生刷卡到班", () -> {
			studentAttendanceService.setClock(fixedClock(courseDate, LocalTime.of(17, 55)));
			CardCheckInRequest request = new CardCheckInRequest();
			request.setCardId("S-" + runId);
			request.setDeviceName("大量測試刷卡機");
			return studentAttendanceService.cardCheckIn(request);
		}, response -> response.getStatus() + ", studentId=" + response.getStudentId());
		step("學生刷卡簽退", () -> {
			studentAttendanceService.setClock(fixedClock(courseDate, LocalTime.of(19, 30)));
			CardCheckInRequest request = new CardCheckInRequest();
			request.setCardId("S-" + runId);
			request.setDeviceName("大量測試刷卡機");
			return studentAttendanceService.cardCheckIn(request);
		}, response -> response.getStatus() + ", checkOut=" + response.getCheckOutTime());

		step("人工點名記錄缺席", () -> {
			StudentAttendanceForm form = new StudentAttendanceForm();
			form.setAttendanceDate(courseDate);
			StudentAttendanceEntryForm entry = new StudentAttendanceEntryForm();
			entry.setStudentId(secondStudent.getId());
			entry.setStatus(AttendanceStatus.ABSENT);
			entry.setNote("大量測試缺席");
			form.getEntries().add(entry);
			studentAttendanceService.saveAttendance(classRoom.getId(), form);
			return studentAttendanceRepository
					.findByClassRoomIdAndStudentIdAndAttendanceDate(classRoom.getId(), secondStudent.getId(), courseDate)
					.orElseThrow();
		}, attendance -> attendance.getStatus() + ", attendanceId=" + attendance.getId());

		ClassSchedule mainSchedule = step("取得主要課程", () -> mainSchedule(classRoom.getId()),
				schedule -> "scheduleId=" + schedule.getId());
		step("教師請假並產生補課需求", () -> leaveService.createLeave(
				teacher.getId(), courseDate, mainSchedule.getId(), "大量測試請假"),
				leave -> "leaveId=" + leave.getId());
		MakeUpClassRequest leaveMakeUp = step("確認請假補課需求", () -> pendingMakeUp(classRoom.getId(), MakeUpSourceType.LEAVE),
				request -> "makeUpRequestId=" + request.getId());
		step("安排補課", () -> {
			LocalDateTime start = LocalDateTime.of(courseDate.plusDays(1), LocalTime.of(14, 0));
			makeUpClassService.scheduleMakeUpClass(leaveMakeUp.getId(), start, start.plusHours(2),
					director.getId(), true, true);
			return makeUpClassRequestRepository.findById(leaveMakeUp.getId()).orElseThrow();
		}, request -> "status=" + request.getStatus() + ", start=" + request.getSelectedMakeUpStart());

		step("教師缺席自動記錄並產生補課需求", () -> {
			absenceService.markAbsencesUntil(LocalDateTime.of(courseDate, LocalTime.of(9, 20)));
			return teacherAttendanceRepository.findByTeacherIdAndDate(absenceTeacher.getId(), courseDate).orElseThrow();
		}, attendance -> attendance.getStatus() + ", attendanceId=" + attendance.getId());
		step("確認缺席補課需求", () -> pendingMakeUp(absenceClassRoom.getId(), MakeUpSourceType.ABSENCE),
				request -> "makeUpRequestId=" + request.getId());

		step("調課", () -> {
			LocalDateTime newStart = LocalDateTime.of(courseDate.plusDays(2), LocalTime.of(14, 0));
			weeklyScheduleService.rescheduleClass(mainSchedule.getId(), courseDate, newStart,
					"大量測試調課", director.getId(), true, true);
			return classRoomRepository.findById(classRoom.getId()).orElseThrow();
		}, updated -> "classRoomId=" + updated.getId());

		step("停用班級", () -> {
			classRoomService.deactivate(classRoom.getId(), director.getId());
			return classRoomService.findById(classRoom.getId());
		}, updated -> "active=" + updated.isActive());
		step("啟用班級", () -> {
			classRoomService.activate(classRoom.getId(), director.getId());
			return classRoomService.findById(classRoom.getId());
		}, updated -> "active=" + updated.isActive());
		step("停用學生", () -> {
			studentService.deactivate(secondStudent.getId(), director.getId());
			return studentService.findById(secondStudent.getId());
		}, updated -> "active=" + updated.isActive());
		step("啟用學生", () -> {
			studentService.activate(secondStudent.getId(), director.getId());
			return studentService.findById(secondStudent.getId());
		}, updated -> "active=" + updated.isActive());

		step("刪除作業", () -> {
			homeworkService.delete(homework.getId());
			return homework.getId();
		}, id -> "deletedHomeworkId=" + id);
		step("刪除測驗", () -> {
			examService.delete(exam.getId());
			return exam.getId();
		}, id -> "deletedExamId=" + id);
		step("刪除含補課調課紀錄的班級", () -> {
			classRoomService.delete(classRoom.getId(), director.getId());
			return classRoom.getId();
		}, id -> "deletedClassRoomId=" + id);
		step("刪除缺席測試班級", () -> {
			classRoomService.delete(absenceClassRoom.getId(), director.getId());
			return absenceClassRoom.getId();
		}, id -> "deletedClassRoomId=" + id);
		step("刪除學生 A", () -> {
			studentService.delete(firstStudent.getId(), director.getId());
			return firstStudent.getId();
		}, id -> "deletedStudentId=" + id);
		step("刪除學生 B", () -> {
			studentService.delete(secondStudent.getId(), director.getId());
			return secondStudent.getId();
		}, id -> "deletedStudentId=" + id);
		step("刪除教師", () -> {
			teacherService.delete(teacher.getId(), director.getId());
			return teacher.getId();
		}, id -> "deletedTeacherId=" + id);
		step("刪除缺席測試教師", () -> {
			teacherService.delete(absenceTeacher.getId(), director.getId());
			return absenceTeacher.getId();
		}, id -> "deletedTeacherId=" + id);

		assertThat(records).allMatch(OperationRecord::success);
	}

	private <T> T step(String name, Supplier<T> action, java.util.function.Function<T, String> detail) {
		int step = stepCounter.incrementAndGet();
		try {
			T result = action.get();
			records.add(new OperationRecord(step, name, true, detail.apply(result)));
			return result;
		} catch (RuntimeException | AssertionError ex) {
			records.add(new OperationRecord(step, name, false,
					ex.getClass().getSimpleName() + ": " + (ex.getMessage() == null ? "" : ex.getMessage())));
			throw ex;
		}
	}

	private TeacherForm teacherForm(String name) {
		TeacherForm form = new TeacherForm();
		form.setName(name);
		form.setNickname(name);
		form.setPhone("0900000000");
		form.setEmail("mass-test-" + Math.abs(name.hashCode()) + "@example.test");
		form.setHireDate(LocalDate.now());
		form.setPosition(TeacherPosition.TEACHER);
		form.setNote("大量操作測試");
		return form;
	}

	private SubjectForm subjectForm(String name, Long teacherId) {
		SubjectForm form = new SubjectForm();
		form.setName(name);
		form.setDescription("大量操作測試科目");
		form.setGradeLevels(List.of("國一", "高一"));
		form.setTeacherIds(List.of(teacherId));
		return form;
	}

	private ClassRoomForm classRoomForm(Long subjectId, Long teacherId, String weekday, LocalTime start, LocalTime end) {
		ClassRoomForm form = ClassRoomForm.newForm();
		form.setGrade("高一");
		form.setSubjectId(subjectId);
		form.setTeacherId(teacherId);
		form.setClassType("大量測試班");
		form.setDescription("大量操作測試班級");
		ScheduleEntryForm entry = new ScheduleEntryForm();
		entry.setWeekday(weekday);
		entry.setStartTime(start);
		entry.setEndTime(end);
		form.setScheduleEntries(List.of(entry));
		return form;
	}

	private StudentForm studentForm(String name) {
		StudentForm form = new StudentForm();
		form.setChineseName(name);
		form.setEnglishName("Mass Test");
		form.setGender("其他");
		form.setBirthday(LocalDate.of(2010, 1, 1));
		form.setSchool("大量測試學校");
		form.setGrade("高一");
		form.setPhone("0911111111");
		form.setNote("大量操作測試學生");
		return form;
	}

	private ExamForm examForm(Long classRoomId, String name, LocalDate date) {
		ExamForm form = new ExamForm();
		form.setClassRoomId(classRoomId);
		form.setName(name);
		form.setExamDate(date);
		form.setFullScore(100);
		form.setDescription("大量操作測試測驗");
		return form;
	}

	private HomeworkForm homeworkForm(Long classRoomId, String title, LocalDate dueDate) {
		HomeworkForm form = new HomeworkForm();
		form.setClassRoomId(classRoomId);
		form.setTitle(title);
		form.setDescription("大量操作測試作業");
		form.setAssignedDate(dueDate.minusDays(1));
		form.setDueDate(dueDate);
		return form;
	}

	private ClassSchedule mainSchedule(Long classRoomId) {
		ClassRoom classRoom = classRoomRepository.findById(classRoomId).orElseThrow();
		return classRoom.getEffectiveSchedules().stream()
				.filter(schedule -> LocalTime.of(18, 0).equals(schedule.getStartTime()))
				.findFirst()
				.orElseThrow();
	}

	private MakeUpClassRequest pendingMakeUp(Long classRoomId, MakeUpSourceType sourceType) {
		return makeUpClassRequestRepository.findByStatusOrderByOriginalCourseDateAscIdAsc(MakeUpStatus.PENDING)
				.stream()
				.filter(request -> request.getClassRoom() != null && classRoomId.equals(request.getClassRoom().getId()))
				.filter(request -> request.getSourceType() == sourceType)
				.findFirst()
				.orElseThrow();
	}

	private Clock fixedClock(LocalDate date, LocalTime time) {
		return Clock.fixed(LocalDateTime.of(date, time).atZone(TAIPEI).toInstant(), TAIPEI);
	}

	private String weekdayName(LocalDate date) {
		return switch (date.getDayOfWeek()) {
			case MONDAY -> "星期一";
			case TUESDAY -> "星期二";
			case WEDNESDAY -> "星期三";
			case THURSDAY -> "星期四";
			case FRIDAY -> "星期五";
			case SATURDAY -> "星期六";
			case SUNDAY -> "星期日";
		};
	}

	private String escape(String value) {
		return value == null ? "" : value.replace("|", "\\|").replace("\n", " ");
	}

	private record OperationRecord(int step, String name, boolean success, String detail) {
	}
}
