package com.example.cramschool.service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.dto.AttendanceStats;
import com.example.cramschool.entity.AttendanceStatus;
import com.example.cramschool.entity.ClassRoom;
import com.example.cramschool.entity.ClassStudent;
import com.example.cramschool.entity.Student;
import com.example.cramschool.entity.StudentAttendance;
import com.example.cramschool.form.StudentAttendanceEntryForm;
import com.example.cramschool.form.StudentAttendanceForm;
import com.example.cramschool.repository.StudentAttendanceRepository;
import com.example.cramschool.repository.StudentRepository;

@Service
@Transactional
public class StudentAttendanceService {

	private final StudentAttendanceRepository studentAttendanceRepository;
	private final StudentRepository studentRepository;
	private final ClassRoomService classRoomService;
	private final ClassStudentService classStudentService;

	public StudentAttendanceService(StudentAttendanceRepository studentAttendanceRepository,
			StudentRepository studentRepository, ClassRoomService classRoomService,
			ClassStudentService classStudentService) {
		this.studentAttendanceRepository = studentAttendanceRepository;
		this.studentRepository = studentRepository;
		this.classRoomService = classRoomService;
		this.classStudentService = classStudentService;
	}

	@Transactional(readOnly = true)
	public List<StudentAttendance> findByStudentId(Long studentId) {
		return studentAttendanceRepository.findByStudentIdOrderByAttendanceDateDescIdDesc(studentId);
	}

	@Transactional(readOnly = true)
	public List<StudentAttendance> findByClassRoomId(Long classRoomId) {
		return studentAttendanceRepository.findByClassRoomIdOrderByAttendanceDateDescStudentChineseNameAsc(classRoomId);
	}

	@Transactional(readOnly = true)
	public StudentAttendanceForm buildForm(Long classRoomId, LocalDate attendanceDate) {
		LocalDate targetDate = attendanceDate == null ? LocalDate.now() : attendanceDate;
		Map<Long, StudentAttendance> attendancesByStudentId = new LinkedHashMap<>();
		for (StudentAttendance attendance : studentAttendanceRepository
				.findByClassRoomIdAndAttendanceDateOrderByStudentChineseNameAsc(classRoomId, targetDate)) {
			attendancesByStudentId.put(attendance.getStudent().getId(), attendance);
		}

		StudentAttendanceForm form = new StudentAttendanceForm();
		form.setAttendanceDate(targetDate);
		for (ClassStudent classStudent : classStudentService.findActiveByClassRoomId(classRoomId)) {
			Student student = classStudent.getStudent();
			StudentAttendance attendance = attendancesByStudentId.get(student.getId());
			StudentAttendanceEntryForm entry = new StudentAttendanceEntryForm();
			entry.setStudentId(student.getId());
			entry.setStudentName(student.getDisplayName());
			entry.setStudentGrade(student.getGrade());
			if (attendance != null) {
				entry.setStatus(attendance.getStatus());
				entry.setNote(attendance.getNote());
			}
			form.getEntries().add(entry);
		}
		return form;
	}

	public void saveAttendance(Long classRoomId, StudentAttendanceForm form) {
		ClassRoom classRoom = classRoomService.findById(classRoomId);
		LocalDate attendanceDate = form.getAttendanceDate() == null ? LocalDate.now() : form.getAttendanceDate();
		for (StudentAttendanceEntryForm entry : form.getEntries()) {
			Student student = studentRepository.findById(entry.getStudentId())
					.orElseThrow(() -> new IllegalArgumentException("找不到學生資料"));
			StudentAttendance attendance = studentAttendanceRepository
					.findByClassRoomIdAndStudentIdAndAttendanceDate(classRoomId, entry.getStudentId(), attendanceDate)
					.orElseGet(StudentAttendance::new);
			attendance.setClassRoom(classRoom);
			attendance.setStudent(student);
			attendance.setAttendanceDate(attendanceDate);
			attendance.setStatus(entry.getStatus() == null ? AttendanceStatus.PRESENT : entry.getStatus());
			attendance.setNote(entry.getNote());
			studentAttendanceRepository.save(attendance);
		}
	}

	@Transactional(readOnly = true)
	public AttendanceStats calculateStatsByClassRoomId(Long classRoomId) {
		return calculateStats(findByClassRoomId(classRoomId));
	}

	private AttendanceStats calculateStats(List<StudentAttendance> attendances) {
		AttendanceStats stats = new AttendanceStats();
		stats.setTotalCount(attendances.size());
		for (StudentAttendance attendance : attendances) {
			if (attendance.getStatus() == AttendanceStatus.PRESENT) {
				stats.setPresentCount(stats.getPresentCount() + 1);
			} else if (attendance.getStatus() == AttendanceStatus.LATE) {
				stats.setLateCount(stats.getLateCount() + 1);
			} else if (attendance.getStatus() == AttendanceStatus.ABSENT) {
				stats.setAbsentCount(stats.getAbsentCount() + 1);
			} else if (attendance.getStatus() == AttendanceStatus.LEAVE) {
				stats.setLeaveCount(stats.getLeaveCount() + 1);
			}
		}
		return stats;
	}
}
