package com.example.cramschool.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.cramschool.entity.Exam;
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
