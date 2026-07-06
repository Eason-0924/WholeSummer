package com.example.cramschool.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.cramschool.dto.CardCheckInRequest;
import com.example.cramschool.dto.CardCheckInResponse;
import com.example.cramschool.entity.AttendanceStatus;
import com.example.cramschool.entity.ClassRoom;
import com.example.cramschool.entity.ClassSchedule;
import com.example.cramschool.entity.ClassStudent;
import com.example.cramschool.entity.Student;
import com.example.cramschool.entity.Subject;
import com.example.cramschool.entity.StudentAttendance;
import com.example.cramschool.repository.ClassRoomRepository;
import com.example.cramschool.repository.ClassStudentRepository;
import com.example.cramschool.repository.LineNotificationLogRepository;
import com.example.cramschool.repository.StudentAttendanceRepository;
import com.example.cramschool.repository.StudentRepository;
import com.example.cramschool.repository.SubjectRepository;

@SpringBootTest
class StudentCardCheckInBoundaryTests {

	private static final ZoneId TAIPEI = ZoneId.of("Asia/Taipei");
	private static final String TUESDAY = "星期二";
	private final String cardPrefix = "BOUNDARY" + System.nanoTime();

	@Autowired
	private StudentAttendanceService studentAttendanceService;

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

	private final List<Student> students = new ArrayList<>();
	private final List<ClassStudent> memberships = new ArrayList<>();
	private final List<ClassRoom> classRooms = new ArrayList<>();
	private final List<Subject> subjects = new ArrayList<>();

	@AfterEach
	void tearDown() {
		studentAttendanceService.setClock(null);
		for (Student student : students) {
			if (student.getId() != null) {
				lineNotificationLogRepository.deleteByStudentId(student.getId());
				studentAttendanceRepository.findByStudentIdOrderByAttendanceDateDescIdDesc(student.getId())
						.forEach(attendance -> studentAttendanceRepository.deleteById(attendance.getId()));
			}
		}
		for (ClassStudent membership : memberships.reversed()) {
			if (membership.getId() != null) {
				classStudentRepository.deleteById(membership.getId());
			}
		}
		for (ClassRoom classRoom : classRooms.reversed()) {
			if (classRoom.getId() != null) {
				classRoomRepository.deleteById(classRoom.getId());
			}
		}
		for (Subject subject : subjects.reversed()) {
			if (subject.getId() != null) {
				subjectRepository.deleteById(subject.getId());
			}
		}
		for (Student student : students.reversed()) {
			if (student.getId() != null) {
				studentRepository.deleteById(student.getId());
			}
		}
	}

	@Test
	void blankCardIdReturnsInvalidCardId() {
		CardCheckInResponse response = studentAttendanceService.cardCheckIn(request(" \n "));

		assertThat(response.isSuccess()).isFalse();
		assertThat(response.getStatus()).isEqualTo("INVALID_CARD_ID");
	}

	@Test
	void rejectsSwipesOutsideThirtyMinuteWindow() {
		Student student = student("時間窗外學生", "WINDOWOUT");
		ClassRoom classRoom = classRoom("時間窗外班", LocalTime.of(17, 0), LocalTime.of(18, 0));
		membership(classRoom, student);

		setNow(LocalDateTime.of(2026, 6, 30, 16, 29));
		assertThat(studentAttendanceService.cardCheckIn(request("WINDOWOUT")).getStatus())
				.isEqualTo("NO_CLASS_TODAY");

		setNow(LocalDateTime.of(2026, 6, 30, 18, 31));
		assertThat(studentAttendanceService.cardCheckIn(request("WINDOWOUT")).getStatus())
				.isEqualTo("NO_CLASS_TODAY");
	}

	@Test
	void acceptsSwipesExactlyAtWindowBoundaries() {
		Student beforeStudent = student("上課前邊界學生", "WINDOWSTART");
		ClassRoom beforeClass = classRoom("上課前邊界班", LocalTime.of(17, 0), LocalTime.of(18, 0));
		membership(beforeClass, beforeStudent);
		setNow(LocalDateTime.of(2026, 6, 30, 16, 30));

		CardCheckInResponse beforeResponse = studentAttendanceService.cardCheckIn(request("WINDOWSTART"));

		assertThat(beforeResponse.getStatus()).isEqualTo("CHECKED_IN");
		assertThat(beforeResponse.getClassName()).isEqualTo(beforeClass.getDisplayName());

		Student afterStudent = student("下課後邊界學生", "WINDOWEND");
		ClassRoom afterClass = classRoom("下課後邊界班", LocalTime.of(17, 0), LocalTime.of(18, 0));
		membership(afterClass, afterStudent);
		setNow(LocalDateTime.of(2026, 6, 30, 18, 30));

		CardCheckInResponse afterResponse = studentAttendanceService.cardCheckIn(request("WINDOWEND"));

		assertThat(afterResponse.getStatus()).isEqualTo("CHECKED_IN");
		assertThat(afterResponse.getClassName()).isEqualTo(afterClass.getDisplayName());
	}

	@Test
	void firstSwipeBeforeClassStartRecordsPresentAndArrivalTimeNote() {
		Student student = student("準時學生", "ONTIME");
		ClassRoom classRoom = classRoom("準時班", LocalTime.of(17, 0), LocalTime.of(18, 0));
		membership(classRoom, student);
		setNow(LocalDateTime.of(2026, 6, 30, 16, 55));

		CardCheckInResponse response = studentAttendanceService.cardCheckIn(request("ONTIME"));

		assertThat(response.getStatus()).isEqualTo("CHECKED_IN");
		StudentAttendance attendance = studentAttendanceRepository.findByStudentIdOrderByAttendanceDateDescIdDesc(
				student.getId()).getFirst();
		assertThat(attendance.getStatus()).isEqualTo(AttendanceStatus.PRESENT);
		assertThat(attendance.getNote()).isEqualTo("到班時間：16:55");
	}

	@Test
	void firstSwipeAfterClassStartRecordsLateAndArrivalTimeNote() {
		Student student = student("遲到學生", "LATECARD");
		ClassRoom classRoom = classRoom("遲到班", LocalTime.of(17, 0), LocalTime.of(18, 0));
		membership(classRoom, student);
		setNow(LocalDateTime.of(2026, 6, 30, 17, 6));

		CardCheckInResponse response = studentAttendanceService.cardCheckIn(request("LATECARD"));

		assertThat(response.getStatus()).isEqualTo("CHECKED_IN");
		StudentAttendance attendance = studentAttendanceRepository.findByStudentIdOrderByAttendanceDateDescIdDesc(
				student.getId()).getFirst();
		assertThat(attendance.getStatus()).isEqualTo(AttendanceStatus.LATE);
		assertThat(attendance.getNote()).isEqualTo("到班時間：17:06");
	}

	@Test
	void choosesNearestClassWhenMultipleClassesAreWithinWindow() {
		Student student = student("多班級學生", "MULTICLASS");
		ClassRoom earlierClass = classRoom("較早班", LocalTime.of(13, 0), LocalTime.of(14, 0));
		ClassRoom laterClass = classRoom("較晚班", LocalTime.of(14, 50), LocalTime.of(15, 50));
		membership(earlierClass, student);
		membership(laterClass, student);
		setNow(LocalDateTime.of(2026, 6, 30, 14, 20));

		CardCheckInResponse response = studentAttendanceService.cardCheckIn(request("MULTICLASS"));

		assertThat(response.getStatus()).isEqualTo("CHECKED_IN");
		assertThat(response.getClassName()).isEqualTo(earlierClass.getDisplayName());
		List<StudentAttendance> attendances = studentAttendanceRepository.findByStudentIdOrderByAttendanceDateDescIdDesc(
				student.getId());
		assertThat(attendances).hasSize(1);
		assertThat(attendances.getFirst().getClassRoom().getId()).isEqualTo(earlierClass.getId());
	}

	private void setNow(LocalDateTime now) {
		Instant instant = now.atZone(TAIPEI).toInstant();
		studentAttendanceService.setClock(Clock.fixed(instant, TAIPEI));
	}

	private CardCheckInRequest request(String cardId) {
		CardCheckInRequest request = new CardCheckInRequest();
		request.setCardId(cardId == null || cardId.isBlank() ? cardId : cardPrefix + cardId);
		request.setDeviceName("boundary-test");
		return request;
	}

	private Student student(String name, String cardId) {
		Student student = new Student();
		student.setChineseName(name);
		student.setActive(true);
		student.setCardId(cardPrefix + cardId);
		student.setCardStatus("ACTIVE");
		Student saved = studentRepository.save(student);
		students.add(saved);
		return saved;
	}

	private ClassRoom classRoom(String classType, LocalTime startTime, LocalTime endTime) {
		Subject subject = new Subject();
		subject.setName("邊界測試科目");
		subject.setActive(true);
		Subject savedSubject = subjectRepository.save(subject);
		subjects.add(savedSubject);

		ClassRoom classRoom = new ClassRoom();
		classRoom.setGrade("國一");
		classRoom.setSubject(savedSubject);
		classRoom.setClassType(classType);
		classRoom.setActive(true);
		classRoom.addSchedule(new ClassSchedule(TUESDAY, startTime, endTime));
		ClassRoom saved = classRoomRepository.save(classRoom);
		classRooms.add(saved);
		return saved;
	}

	private ClassStudent membership(ClassRoom classRoom, Student student) {
		ClassStudent membership = new ClassStudent();
		membership.setClassRoom(classRoom);
		membership.setStudent(student);
		membership.setActive(true);
		ClassStudent saved = classStudentRepository.save(membership);
		memberships.add(saved);
		return saved;
	}
}
