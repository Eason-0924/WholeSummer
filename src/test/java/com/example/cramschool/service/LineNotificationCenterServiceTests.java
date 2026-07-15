package com.example.cramschool.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.example.cramschool.entity.ClassRoom;
import com.example.cramschool.entity.ClassStudent;
import com.example.cramschool.entity.Exam;
import com.example.cramschool.entity.MakeUpClassRequest;
import com.example.cramschool.entity.MakeUpStatus;
import com.example.cramschool.entity.ParentLineBinding;
import com.example.cramschool.entity.LineNotificationTemplate;
import com.example.cramschool.entity.Score;
import com.example.cramschool.entity.Student;
import com.example.cramschool.dto.LineSendResult;
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

	@Test
	void sendCandidatesUsesSameMergedTextAsPreviewWhenTemplateUsesCrLf() {
		Score first = score(1L, "第一次測驗", 80);
		Score second = score(2L, "第二次測驗", 90);
		Student student = first.getStudent();
		second.setStudent(student);
		ParentLineBinding binding = new ParentLineBinding();
		binding.setStudent(student);
		binding.setLineUserId("U-parent");
		binding.setRelation("媽媽");

		ScoreRepository scoreRepository = mock(ScoreRepository.class);
		MakeUpClassRequestRepository makeUpRepository = mock(MakeUpClassRequestRepository.class);
		TuitionRecordRepository tuitionRepository = mock(TuitionRecordRepository.class);
		ClassStudentRepository classStudentRepository = mock(ClassStudentRepository.class);
		ParentLineBindingRepository bindingRepository = mock(ParentLineBindingRepository.class);
		LineNotificationLogRepository logRepository = mock(LineNotificationLogRepository.class);
		LineNotificationTemplateRepository templateRepository = mock(LineNotificationTemplateRepository.class);
		LineMessageService messageService = mock(LineMessageService.class);

		when(scoreRepository.findAll()).thenReturn(List.of(first, second));
		when(makeUpRepository.findByStatusOrderByOriginalCourseDateAscIdAsc(org.mockito.ArgumentMatchers.any()))
				.thenReturn(List.of());
		when(makeUpRepository.findByStatusOrderBySelectedMakeUpStartAscIdAsc(org.mockito.ArgumentMatchers.any()))
				.thenReturn(List.of());
		when(tuitionRepository.findAllByOrderByDueDateDescIdDesc()).thenReturn(List.of());
		when(bindingRepository.findByStudentAndStatus(student, ParentLineBinding.STATUS_BOUND))
				.thenReturn(List.of(binding));
		when(logRepository.existsByStudentAndNotificationTypeAndReferenceTypeAndReferenceId(
				org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString(),
				org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyLong())).thenReturn(false);
		when(templateRepository.findByTemplateKey("SCORE"))
				.thenReturn(Optional.of(template("【Whole Summer 成績通知】\r\n\r\n{稱謂}您好：\r\n{項目名稱}：{成績}")));
		when(messageService.pushText(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
				.thenReturn(LineSendResult.success("request-1"));

		LineNotificationCenterService service = new LineNotificationCenterService(
				scoreRepository, makeUpRepository, tuitionRepository, classStudentRepository,
				bindingRepository, logRepository, templateRepository, messageService);

		service.sendCandidates(List.of("SCORE-1", "SCORE-2"), List.of(-1L));

		verify(messageService).pushText("U-parent",
				"【Whole Summer 成績通知】\n\n小明媽媽您好：\n第一次測驗：80\n第二次測驗：90");
	}

	@Test
	void removesRepeatedIntroForTuitionMakeUpAndRescheduleTemplates() {
		List<String> repeatedMessages = List.of(
				"【Whole Summer 繳費提醒】\r\n\r\n小明媽媽您好：\r\n繳費項目：一月學費",
				"【Whole Summer 補課通知】\r\n\r\n小明媽媽您好：\r\n補課時間：2026/07/20",
				"【Whole Summer 調課通知】\r\n\r\n小明媽媽您好：\r\n調課時間：2026/07/21");

		assertThat(repeatedMessages.stream()
				.map(LineNotificationCenterService::removeRepeatedTemplateIntro).toList())
				.containsExactly(
						"繳費項目：一月學費",
						"補課時間：2026/07/20",
						"調課時間：2026/07/21");
	}

	private Score score(Long id, String examName, int value) {
		Score score = new Score();
		score.setId(id);
		score.setScore(value);
		score.setStudent(student());
		Exam exam = new Exam();
		exam.setId(id);
		exam.setName(examName);
		exam.setFullScore(100);
		score.setExam(exam);
		return score;
	}

	private LineNotificationTemplate template(String body) {
		LineNotificationTemplate template = new LineNotificationTemplate();
		template.setTemplateKey("SCORE");
		template.setTitle("成績通知");
		template.setBody(body);
		return template;
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
