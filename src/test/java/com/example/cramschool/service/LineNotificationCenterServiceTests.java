package com.example.cramschool.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.cramschool.entity.ClassRoom;
import com.example.cramschool.entity.ClassStudent;
import com.example.cramschool.entity.Exam;
import com.example.cramschool.entity.MakeUpClassRequest;
import com.example.cramschool.entity.MakeUpStatus;
import com.example.cramschool.entity.Score;
import com.example.cramschool.entity.Student;
import com.example.cramschool.repository.ClassStudentRepository;
import com.example.cramschool.repository.LineNotificationLogRepository;
import com.example.cramschool.repository.LineNotificationTemplateRepository;
import com.example.cramschool.repository.MakeUpClassRequestRepository;
import com.example.cramschool.repository.ParentLineBindingRepository;
import com.example.cramschool.repository.ScoreRepository;
import com.example.cramschool.repository.TuitionRecordRepository;

class LineNotificationCenterServiceTests {

	@Test
	void buildCandidatesSkipsPracticeExamScores() {
		ScoreRepository scoreRepository = mock(ScoreRepository.class);
		MakeUpClassRequestRepository makeUpRepository = mock(MakeUpClassRequestRepository.class);
		TuitionRecordRepository tuitionRepository = mock(TuitionRecordRepository.class);
		LineNotificationLogRepository logRepository = mock(LineNotificationLogRepository.class);

		Score practiceScore = new Score();
		practiceScore.setId(1L);
		practiceScore.setScore(1);
		practiceScore.setStudent(student());
		practiceScore.setExam(practiceExam());
		when(scoreRepository.findAll()).thenReturn(List.of(practiceScore));
		when(makeUpRepository.findByStatusOrderByOriginalCourseDateAscIdAsc(org.mockito.ArgumentMatchers.any()))
				.thenReturn(List.of());
		when(makeUpRepository.findByStatusOrderBySelectedMakeUpStartAscIdAsc(org.mockito.ArgumentMatchers.any()))
				.thenReturn(List.of());
		when(tuitionRepository.findAllByOrderByDueDateDescIdDesc()).thenReturn(List.of());

		LineNotificationCenterService service = new LineNotificationCenterService(
				scoreRepository,
				makeUpRepository,
				tuitionRepository,
				mock(ClassStudentRepository.class),
				mock(ParentLineBindingRepository.class),
				logRepository,
				mock(LineNotificationTemplateRepository.class),
				mock(LineMessageService.class));

		assertThat(service.buildCandidates()).isEmpty();
	}

	@Test
	void buildCandidatesIncludesPendingMakeUpWithTimeToBeDetermined() {
		ScoreRepository scoreRepository = mock(ScoreRepository.class);
		MakeUpClassRequestRepository makeUpRepository = mock(MakeUpClassRequestRepository.class);
		TuitionRecordRepository tuitionRepository = mock(TuitionRecordRepository.class);
		ClassStudentRepository classStudentRepository = mock(ClassStudentRepository.class);
		LineNotificationLogRepository logRepository = mock(LineNotificationLogRepository.class);

		ClassRoom classRoom = new ClassRoom();
		classRoom.setId(8L);
		classRoom.setGrade("國一");
		classRoom.setClassType("英文");
		MakeUpClassRequest request = new MakeUpClassRequest();
		request.setId(15L);
		request.setClassRoom(classRoom);
		request.setOriginalCourseDate(LocalDate.of(2026, 7, 13));
		request.setStatus(MakeUpStatus.PENDING);
		ClassStudent enrollment = new ClassStudent();
		enrollment.setStudent(student());

		when(scoreRepository.findAll()).thenReturn(List.of());
		when(makeUpRepository.findByStatusOrderByOriginalCourseDateAscIdAsc(MakeUpStatus.PENDING))
				.thenReturn(List.of(request));
		when(makeUpRepository.findByStatusOrderBySelectedMakeUpStartAscIdAsc(MakeUpStatus.SCHEDULED))
				.thenReturn(List.of());
		when(tuitionRepository.findAllByOrderByDueDateDescIdDesc()).thenReturn(List.of());
		when(classStudentRepository.findByClassRoomIdAndActiveTrueOrderByStudentChineseNameAsc(8L))
				.thenReturn(List.of(enrollment));

		LineNotificationCenterService service = new LineNotificationCenterService(
				scoreRepository, makeUpRepository, tuitionRepository, classStudentRepository,
				mock(ParentLineBindingRepository.class), logRepository,
				mock(LineNotificationTemplateRepository.class), mock(LineMessageService.class));

		assertThat(service.buildCandidates()).singleElement().satisfies(candidate -> {
			assertThat(candidate.id()).isEqualTo("MAKE_UP_PENDING-15-1");
			assertThat(candidate.templateKey()).isEqualTo("MAKE_UP_PENDING");
			assertThat(candidate.notificationType()).isEqualTo("MANUAL_MAKE_UP_PENDING");
			assertThat(candidate.values()).containsEntry("新上課時間", "待定");
		});
		assertThat(service.templates().get("MAKE_UP_PENDING").body()).contains("補課時間待定");
	}

	private Student student() {
		Student student = new Student();
		student.setId(1L);
		student.setChineseName("王小明");
		return student;
	}

	private Exam practiceExam() {
		Exam exam = new Exam();
		exam.setId(1L);
		exam.setName("單字練習");
		exam.setFullScore(0);
		return exam;
	}
}
