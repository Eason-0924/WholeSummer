package com.example.cramschool.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.entity.StudentLeaveRequest;
import com.example.cramschool.entity.StudentLeaveStatus;
import com.example.cramschool.entity.Teacher;
import com.example.cramschool.entity.TeacherPermissionType;
import com.example.cramschool.repository.StudentLeaveRequestRepository;
import com.example.cramschool.repository.TeacherRepository;

@Service
@Transactional
public class StudentLeaveManagementService {

	private final StudentLeaveRequestRepository studentLeaveRequestRepository;
	private final TeacherRepository teacherRepository;
	private final TeacherPermissionService teacherPermissionService;
	private final LineNotificationService lineNotificationService;
	private final StudentAttendanceService studentAttendanceService;

	public StudentLeaveManagementService(StudentLeaveRequestRepository studentLeaveRequestRepository,
			TeacherRepository teacherRepository, TeacherPermissionService teacherPermissionService,
			LineNotificationService lineNotificationService, StudentAttendanceService studentAttendanceService) {
		this.studentLeaveRequestRepository = studentLeaveRequestRepository;
		this.teacherRepository = teacherRepository;
		this.teacherPermissionService = teacherPermissionService;
		this.lineNotificationService = lineNotificationService;
		this.studentAttendanceService = studentAttendanceService;
	}

	@Transactional(readOnly = true)
	public List<StudentLeaveRequest> findPendingRequests() {
		return studentLeaveRequestRepository.findByStatusOrderByCourseDateAscScheduledStartAtAsc(
				StudentLeaveStatus.PENDING);
	}

	@Transactional(readOnly = true)
	public List<StudentLeaveRequest> findConfirmedRequests() {
		return studentLeaveRequestRepository.findByStatusOrderByCourseDateAscScheduledStartAtAsc(
				StudentLeaveStatus.APPROVED);
	}

	public StudentLeaveRequest confirm(Long requestId, Long currentTeacherId) {
		teacherPermissionService.requirePermission(currentTeacherId, TeacherPermissionType.STUDENT_UPDATE,
				"權限不足，無法確認學生請假");
		StudentLeaveRequest request = studentLeaveRequestRepository.findById(requestId)
				.orElseThrow(() -> new IllegalArgumentException("找不到請假申請"));
		if (request.getStatus() != StudentLeaveStatus.PENDING) {
			throw new IllegalArgumentException("此請假申請已處理");
		}
		Teacher teacher = teacherRepository.findById(currentTeacherId)
				.orElseThrow(() -> new IllegalArgumentException("找不到目前登入教師"));
		request.setStatus(StudentLeaveStatus.APPROVED);
		request.setReviewedByTeacher(teacher);
		request.setReviewedAt(LocalDateTime.now());
		StudentLeaveRequest saved = studentLeaveRequestRepository.save(request);
		studentAttendanceService.markApprovedLeaveAttendance(saved);
		lineNotificationService.sendStudentLeaveApprovedNotification(saved);
		return saved;
	}
}
