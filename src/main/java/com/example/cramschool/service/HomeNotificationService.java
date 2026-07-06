package com.example.cramschool.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.dto.HomeNotification;
import com.example.cramschool.entity.MakeUpClassRequest;
import com.example.cramschool.entity.MakeUpSourceType;
import com.example.cramschool.entity.StudentLeaveStatus;
import com.example.cramschool.entity.TeacherPermissionType;
import com.example.cramschool.repository.StudentRepository;
import com.example.cramschool.repository.StudentLeaveRequestRepository;

@Service
@Transactional(readOnly = true)
public class HomeNotificationService {

	private final HomeworkService homeworkService;
	private final ExamService examService;
	private final TuitionRecordService tuitionRecordService;
	private final StudentRepository studentRepository;
	private final TeacherPermissionService teacherPermissionService;
	private final MakeUpClassService makeUpClassService;
	private final StudentLeaveRequestRepository studentLeaveRequestRepository;

	public HomeNotificationService(HomeworkService homeworkService, ExamService examService,
			TuitionRecordService tuitionRecordService, StudentRepository studentRepository,
			TeacherPermissionService teacherPermissionService,
			MakeUpClassService makeUpClassService,
			StudentLeaveRequestRepository studentLeaveRequestRepository) {
		this.homeworkService = homeworkService;
		this.examService = examService;
		this.tuitionRecordService = tuitionRecordService;
		this.studentRepository = studentRepository;
		this.teacherPermissionService = teacherPermissionService;
		this.makeUpClassService = makeUpClassService;
		this.studentLeaveRequestRepository = studentLeaveRequestRepository;
	}

	public List<HomeNotification> buildNotifications(Long teacherId, int warningDays) {
		LocalDate today = LocalDate.now();
		LocalDate endDate = today.plusDays(Math.max(0, warningDays));
		List<HomeNotification> notifications = new ArrayList<>();
		boolean director = teacherPermissionService.hasPermission(
				teacherId, TeacherPermissionType.MANAGE_ALL_ATTENDANCE);
		if (teacherPermissionService.hasPermission(teacherId, TeacherPermissionType.STUDENT_UPDATE)) {
			List<com.example.cramschool.entity.StudentLeaveRequest> pendingLeaves = studentLeaveRequestRepository
					.findByStatusOrderByCourseDateAscScheduledStartAtAsc(StudentLeaveStatus.PENDING);
			if (!pendingLeaves.isEmpty()) {
				com.example.cramschool.entity.StudentLeaveRequest first = pendingLeaves.getFirst();
				notifications.add(new HomeNotification(
						"leave", "學生請假", "有 " + pendingLeaves.size() + " 筆學生請假待審核",
						first.getStudent().getDisplayName() + "｜" + first.getClassRoom().getDisplayName(),
						"點擊前往確認請假紀錄",
						"/student-leaves",
						first.getCourseDate() == null ? today : first.getCourseDate()));
			}
		}
		List<MakeUpClassRequest> pendingMakeUpRequests = makeUpClassService.findPendingForHome(teacherId, director);
		makeUpClassService.warmUpPendingCalendarCache(pendingMakeUpRequests);
		for (MakeUpClassRequest request : pendingMakeUpRequests) {
			String courseName = request.getClassRoom() == null ? "未指定班級" : request.getClassRoom().getDisplayName();
			String teacherName = request.getTeacher() == null ? "未指定教師" : request.getTeacher().getDisplayName();
			boolean reschedule = request.getSourceType() == MakeUpSourceType.RESCHEDULE;
			String notificationDetailPrefix = reschedule
					? (request.getNote() == null || request.getNote().isBlank() ? "調課" : request.getNote().trim())
					: request.getSourceType().getDisplayName();
			notifications.add(new HomeNotification(
					"makeup", reschedule ? "調課" : "補課",
					director ? teacherName + "老師的「" + courseName + "」課程"
							+ (reschedule ? "尚未安排調課時間" : "需要補課")
							: "你的「" + courseName + "」課程" + (reschedule ? "尚未安排調課時間" : "需要補課"),
					reschedule ? "點擊前往安排調課時間" : "點擊前往安排補課時間",
					notificationDetailPrefix + "｜原課程日期：" + request.getOriginalCourseDate(),
					"/make-up/" + request.getId(),
					request.getOriginalCourseDate()));
		}

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
						"/students/" + student.getUrlSlug(), today)));

		if (teacherPermissionService.hasPermission(teacherId, TeacherPermissionType.MANAGE_TUITION)) {
			tuitionRecordService.findAll().stream()
					.filter(record -> record.isOverdue())
					.forEach(record -> notifications.add(new HomeNotification(
							"tuition", "學費", record.getStudent().getDisplayName() + " 學費逾期",
							record.getTitle(),
							"未繳 NT$ " + record.getOutstandingAmount(),
							"/students/" + record.getStudent().getUrlSlug(),
							record.getDueDate())));
		}

		return notifications.stream()
				.sorted(Comparator.comparing(HomeNotification::date)
						.thenComparing(HomeNotification::type)
						.thenComparing(HomeNotification::title))
				.toList();
	}
}
