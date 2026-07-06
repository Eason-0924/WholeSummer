package com.example.cramschool.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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

	private Student student() {
		Student student = new Student();
		student.setId(21L);
		student.setChineseName("王小明");
		student.setActive(true);
		return student;
	}
}
