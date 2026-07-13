package com.example.cramschool.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.example.cramschool.config.LineProperties;
import com.example.cramschool.dto.LineSendResult;
import com.example.cramschool.entity.AttendanceStatus;
import com.example.cramschool.entity.ClassRoom;
import com.example.cramschool.entity.LineNotificationLog;
import com.example.cramschool.entity.ParentLineBinding;
import com.example.cramschool.entity.Student;
import com.example.cramschool.entity.StudentAttendance;
import com.example.cramschool.entity.StudentLeaveRequest;
import com.example.cramschool.repository.LineNotificationLogRepository;
import com.example.cramschool.repository.ParentLineBindingRepository;

class LineNotificationServiceTests {

	@Test
	void cardCheckInNotificationAlwaysSendsArrivalMessageEvenWhenAttendanceIsLate() {
		Student student = student();
		ClassRoom classRoom = new ClassRoom();
		classRoom.setId(11L);
		classRoom.setGrade("國一");
		classRoom.setClassType("A班");
		StudentAttendance attendance = new StudentAttendance();
		attendance.setId(51L);
		attendance.setStudent(student);
		attendance.setClassRoom(classRoom);
		attendance.setStatus(AttendanceStatus.LATE);
		attendance.setCheckInTime(LocalDateTime.of(2026, 7, 5, 18, 6));
		ParentLineBinding binding = new ParentLineBinding();
		binding.setStudent(student);
		binding.setRelation("媽媽");
		binding.setLineUserId("line-user-1");
		ParentLineBindingRepository bindingRepository = mock(ParentLineBindingRepository.class);
		LineNotificationLogRepository logRepository = mock(LineNotificationLogRepository.class);
		LineMessageService lineMessageService = mock(LineMessageService.class);
		when(logRepository.existsByStudentAndNotificationTypeAndReferenceTypeAndReferenceId(
				student, "ATTENDANCE_CHECK_IN", "STUDENT_ATTENDANCE", attendance.getId())).thenReturn(false);
		when(bindingRepository.findByStudentAndStatus(student, ParentLineBinding.STATUS_BOUND))
				.thenReturn(List.of(binding));
		when(lineMessageService.pushText(eq("line-user-1"), any())).thenReturn(LineSendResult.success("request-1"));
		LineNotificationService service = new LineNotificationService(null, bindingRepository, logRepository,
				null, lineMessageService, new LineProperties());

		service.sendCardCheckInNotification(attendance);

		ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
		verify(lineMessageService).pushText(eq("line-user-1"), contentCaptor.capture());
		assertThat(contentCaptor.getValue()).contains("到班通知", "狀態：到班");
		assertThat(contentCaptor.getValue()).doesNotContain("遲到通知", "狀態：遲到");
		ArgumentCaptor<LineNotificationLog> logCaptor = ArgumentCaptor.forClass(LineNotificationLog.class);
		verify(logRepository).save(logCaptor.capture());
		assertThat(logCaptor.getValue().getNotificationType()).isEqualTo("ATTENDANCE_CHECK_IN");
	}

	@Test
	void lateArrivalReminderIsNotSentAgainForTheSameClassOccurrence() {
		Student student = student();
		ParentLineBindingRepository bindingRepository = mock(ParentLineBindingRepository.class);
		LineNotificationLogRepository logRepository = mock(LineNotificationLogRepository.class);
		LineMessageService lineMessageService = mock(LineMessageService.class);
		when(logRepository.existsByStudentAndNotificationTypeAndReferenceTypeAndReferenceId(
				student, "ATTENDANCE_LATE_REMINDER", "CLASS_SCHEDULE_OCCURRENCE", 123L)).thenReturn(true);
		LineNotificationService service = new LineNotificationService(null, bindingRepository, logRepository,
				null, lineMessageService, new LineProperties());

		service.sendLateArrivalReminder(student, 123L, "國一（A班）", LocalDateTime.of(2026, 7, 5, 18, 0));

		verifyNoInteractions(bindingRepository, lineMessageService);
	}

	@Test
	void combinesLateRemindersForMultipleStudentsBoundToTheSameParent() {
		Student firstStudent = student();
		Student secondStudent = new Student();
		secondStudent.setId(22L);
		secondStudent.setChineseName("王小華");
		ParentLineBinding firstBinding = new ParentLineBinding();
		firstBinding.setStudent(firstStudent);
		firstBinding.setLineUserId("shared-parent");
		ParentLineBinding secondBinding = new ParentLineBinding();
		secondBinding.setStudent(secondStudent);
		secondBinding.setLineUserId("shared-parent");
		ParentLineBindingRepository bindingRepository = mock(ParentLineBindingRepository.class);
		LineNotificationLogRepository logRepository = mock(LineNotificationLogRepository.class);
		LineMessageService lineMessageService = mock(LineMessageService.class);
		when(bindingRepository.findByStudentAndStatus(firstStudent, ParentLineBinding.STATUS_BOUND))
				.thenReturn(List.of(firstBinding));
		when(bindingRepository.findByStudentAndStatus(secondStudent, ParentLineBinding.STATUS_BOUND))
				.thenReturn(List.of(secondBinding));
		when(lineMessageService.pushText(eq("shared-parent"), any())).thenReturn(LineSendResult.success("request-1"));
		LineNotificationService service = new LineNotificationService(null, bindingRepository, logRepository,
				null, lineMessageService, new LineProperties());

		service.sendLateArrivalReminders(List.of(
				new LineNotificationService.LateArrivalReminder(firstStudent, 101L, "國一數學", LocalDateTime.of(2026, 7, 5, 18, 0)),
				new LineNotificationService.LateArrivalReminder(secondStudent, 102L, "國一英文", LocalDateTime.of(2026, 7, 5, 18, 0))));

		ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
		verify(lineMessageService, times(1)).pushText(eq("shared-parent"), contentCaptor.capture());
		assertThat(contentCaptor.getValue()).contains("王小明", "王小華", "國一數學", "國一英文");
		verify(logRepository, times(2)).save(any(LineNotificationLog.class));
	}

	@Test
	void studentLeaveSubmittedNotificationIncludesLeaveDetailsAndQuestionText() {
		Student student = student();
		StudentLeaveRequest leaveRequest = leaveRequest(student);
		LineNotificationLogRepository logRepository = mock(LineNotificationLogRepository.class);
		LineMessageService lineMessageService = mock(LineMessageService.class);
		when(logRepository.existsByStudentAndNotificationTypeAndReferenceTypeAndReferenceId(
				student, "STUDENT_LEAVE_SUBMITTED", "STUDENT_LEAVE_REQUEST", leaveRequest.getId())).thenReturn(false);
		when(lineMessageService.pushText(eq("line-user-1"), any())).thenReturn(LineSendResult.success("request-1"));
		LineNotificationService service = new LineNotificationService(null, mock(ParentLineBindingRepository.class),
				logRepository, null, lineMessageService, new LineProperties());

		service.sendStudentLeaveSubmittedNotification(leaveRequest);

		ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
		verify(lineMessageService).pushText(eq("line-user-1"), contentCaptor.capture());
		assertThat(contentCaptor.getValue())
				.contains("請假申請確認", "學生：王小明", "課程：國一（A班）",
						"時間：2026/07/08 18:00-20:00", "請假原因：病假：發燒",
						"若有疑問可直接詢問告知");
		ArgumentCaptor<LineNotificationLog> logCaptor = ArgumentCaptor.forClass(LineNotificationLog.class);
		verify(logRepository).save(logCaptor.capture());
		assertThat(logCaptor.getValue().getNotificationType()).isEqualTo("STUDENT_LEAVE_SUBMITTED");
		assertThat(logCaptor.getValue().getLineUserId()).isEqualTo("line-user-1");
	}

	@Test
	void studentLeaveApprovedNotificationTellsParentTeacherConfirmedLeaveRecord() {
		Student student = student();
		StudentLeaveRequest leaveRequest = leaveRequest(student);
		LineNotificationLogRepository logRepository = mock(LineNotificationLogRepository.class);
		LineMessageService lineMessageService = mock(LineMessageService.class);
		when(logRepository.existsByStudentAndNotificationTypeAndReferenceTypeAndReferenceId(
				student, "STUDENT_LEAVE_APPROVED", "STUDENT_LEAVE_REQUEST", leaveRequest.getId())).thenReturn(false);
		when(lineMessageService.pushText(eq("line-user-1"), any())).thenReturn(LineSendResult.success("request-1"));
		LineNotificationService service = new LineNotificationService(null, mock(ParentLineBindingRepository.class),
				logRepository, null, lineMessageService, new LineProperties());

		service.sendStudentLeaveApprovedNotification(leaveRequest);

		ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
		verify(lineMessageService).pushText(eq("line-user-1"), contentCaptor.capture());
		assertThat(contentCaptor.getValue())
				.contains("請假審核通知", "補習班教師已確認請假紀錄", "學生：王小明",
						"課程：國一（A班）", "時間：2026/07/08 18:00-20:00", "請假原因：病假：發燒");
		ArgumentCaptor<LineNotificationLog> logCaptor = ArgumentCaptor.forClass(LineNotificationLog.class);
		verify(logRepository).save(logCaptor.capture());
		assertThat(logCaptor.getValue().getNotificationType()).isEqualTo("STUDENT_LEAVE_APPROVED");
	}

	private Student student() {
		Student student = new Student();
		student.setId(21L);
		student.setChineseName("王小明");
		student.setActive(true);
		return student;
	}

	private StudentLeaveRequest leaveRequest(Student student) {
		ClassRoom classRoom = new ClassRoom();
		classRoom.setId(11L);
		classRoom.setGrade("國一");
		classRoom.setClassType("A班");
		StudentLeaveRequest leaveRequest = new StudentLeaveRequest();
		leaveRequest.setId(81L);
		leaveRequest.setStudent(student);
		leaveRequest.setClassRoom(classRoom);
		leaveRequest.setRequesterLineUserId("line-user-1");
		leaveRequest.setScheduledStartAt(LocalDateTime.of(2026, 7, 8, 18, 0));
		leaveRequest.setScheduledEndAt(LocalDateTime.of(2026, 7, 8, 20, 0));
		leaveRequest.setReason("病假：發燒");
		return leaveRequest;
	}
}
