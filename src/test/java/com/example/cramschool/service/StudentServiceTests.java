package com.example.cramschool.service;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import com.example.cramschool.entity.Student;
import com.example.cramschool.repository.ClassStudentRepository;
import com.example.cramschool.repository.HomeworkRecordRepository;
import com.example.cramschool.repository.LineBindCodeRepository;
import com.example.cramschool.repository.LineNotificationLogRepository;
import com.example.cramschool.repository.ParentLineBindingRepository;
import com.example.cramschool.repository.ScoreRepository;
import com.example.cramschool.repository.StudentAttendanceRepository;
import com.example.cramschool.repository.StudentLeaveRequestRepository;
import com.example.cramschool.repository.StudentRepository;
import com.example.cramschool.repository.TeacherRepository;
import com.example.cramschool.repository.TuitionRecordRepository;

class StudentServiceTests {

	@Test
	void deleteClearsStudentForeignKeyDataBeforeDeletingStudent() {
		Student student = new Student();
		student.setId(21L);
		student.setChineseName("王小明");
		StudentRepository studentRepository = mock(StudentRepository.class);
		ClassStudentRepository classStudentRepository = mock(ClassStudentRepository.class);
		ScoreRepository scoreRepository = mock(ScoreRepository.class);
		HomeworkRecordRepository homeworkRecordRepository = mock(HomeworkRecordRepository.class);
		StudentAttendanceRepository studentAttendanceRepository = mock(StudentAttendanceRepository.class);
		StudentLeaveRequestRepository studentLeaveRequestRepository = mock(StudentLeaveRequestRepository.class);
		TuitionRecordRepository tuitionRecordRepository = mock(TuitionRecordRepository.class);
		LineNotificationLogRepository lineNotificationLogRepository = mock(LineNotificationLogRepository.class);
		LineBindCodeRepository lineBindCodeRepository = mock(LineBindCodeRepository.class);
		ParentLineBindingRepository parentLineBindingRepository = mock(ParentLineBindingRepository.class);
		when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));
		StudentService service = new StudentService(
				studentRepository,
				classStudentRepository,
				scoreRepository,
				homeworkRecordRepository,
				studentAttendanceRepository,
				studentLeaveRequestRepository,
				tuitionRecordRepository,
				lineNotificationLogRepository,
				lineBindCodeRepository,
				parentLineBindingRepository,
				mock(TeacherRepository.class),
				mock(TeacherPermissionService.class),
				mock(StudentUrlSlugService.class));

		service.delete(student.getId(), 1L);

		InOrder inOrder = inOrder(
				classStudentRepository,
				scoreRepository,
				homeworkRecordRepository,
				studentAttendanceRepository,
				studentLeaveRequestRepository,
				tuitionRecordRepository,
				lineNotificationLogRepository,
				lineBindCodeRepository,
				parentLineBindingRepository,
				studentRepository);
		inOrder.verify(classStudentRepository).deleteByStudentId(student.getId());
		inOrder.verify(scoreRepository).deleteByStudentId(student.getId());
		inOrder.verify(homeworkRecordRepository).deleteByStudentId(student.getId());
		inOrder.verify(studentAttendanceRepository).deleteByStudentId(student.getId());
		inOrder.verify(studentLeaveRequestRepository).deleteByStudentId(student.getId());
		inOrder.verify(tuitionRecordRepository).deleteByStudentId(student.getId());
		inOrder.verify(lineNotificationLogRepository).deleteByStudentId(student.getId());
		inOrder.verify(lineBindCodeRepository).deleteByStudentId(student.getId());
		inOrder.verify(parentLineBindingRepository).deleteByStudentId(student.getId());
		inOrder.verify(studentRepository).delete(student);
	}
}
