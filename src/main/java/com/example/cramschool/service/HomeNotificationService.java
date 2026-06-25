package com.example.cramschool.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.dto.HomeNotification;
import com.example.cramschool.entity.TeacherPermissionType;
import com.example.cramschool.repository.StudentRepository;

@Service
@Transactional(readOnly = true)
public class HomeNotificationService {

	private final HomeworkService homeworkService;
	private final ExamService examService;
	private final TuitionRecordService tuitionRecordService;
	private final StudentRepository studentRepository;
	private final TeacherPermissionService teacherPermissionService;

	public HomeNotificationService(HomeworkService homeworkService, ExamService examService,
			TuitionRecordService tuitionRecordService, StudentRepository studentRepository,
			TeacherPermissionService teacherPermissionService) {
		this.homeworkService = homeworkService;
		this.examService = examService;
		this.tuitionRecordService = tuitionRecordService;
		this.studentRepository = studentRepository;
		this.teacherPermissionService = teacherPermissionService;
	}

	public List<HomeNotification> buildNotifications(Long teacherId, int warningDays) {
		LocalDate today = LocalDate.now();
		LocalDate endDate = today.plusDays(Math.max(0, warningDays));
		List<HomeNotification> notifications = new ArrayList<>();

		homeworkService.findUpcomingNotSubmittedSummaries(warningDays).forEach(summary -> {
			String studentNames = summary.notSubmittedRecords().stream()
					.map(record -> record.getStudent().getDisplayName())
					.collect(java.util.stream.Collectors.joining("、"));
			notifications.add(new HomeNotification(
					"homework", "作業", summary.homework().getTitle(),
					summary.homework().getClassRoom().getDisplayName(),
					"未完成：" + studentNames,
					"/homeworks/" + summary.homework().getId(),
					summary.homework().getDueDate()));
		});

		examService.findAll().stream()
				.filter(exam -> !exam.getExamDate().isBefore(today) && !exam.getExamDate().isAfter(endDate))
				.forEach(exam -> notifications.add(new HomeNotification(
						"exam", exam.getFullScore() == 0 ? "課堂練習" : "測驗",
						exam.getName(), exam.getClassRoom().getDisplayName(),
						"日期：" + exam.getExamDate(),
						"/exams/" + exam.getId(), exam.getExamDate())));

		studentRepository.findByActiveTrue().stream()
				.filter(student -> student.getBirthday() != null
						&& student.getBirthday().getMonthValue() == today.getMonthValue()
						&& student.getBirthday().getDayOfMonth() == today.getDayOfMonth())
				.forEach(student -> notifications.add(new HomeNotification(
						"birthday", "生日", student.getDisplayName() + " 今天生日",
						student.getGrade() == null ? "學生生日" : student.getGrade(),
						"生日：" + student.getBirthday(),
						"/students/" + student.getId(), today)));

		if (teacherPermissionService.hasPermission(teacherId, TeacherPermissionType.MANAGE_TUITION)) {
			tuitionRecordService.findAll().stream()
					.filter(record -> record.isOverdue())
					.forEach(record -> notifications.add(new HomeNotification(
							"tuition", "學費", record.getStudent().getDisplayName() + " 學費逾期",
							record.getTitle(),
							"未繳 NT$ " + record.getOutstandingAmount(),
							"/students/" + record.getStudent().getId(),
							record.getDueDate())));
		}

		return notifications.stream()
				.sorted(Comparator.comparing(HomeNotification::date)
						.thenComparing(HomeNotification::type)
						.thenComparing(HomeNotification::title))
				.toList();
	}
}
