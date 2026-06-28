package com.example.cramschool.service;

import java.time.DayOfWeek;
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

	private static final Map<DayOfWeek, String> WEEKDAY_NAMES = Map.of(
			DayOfWeek.MONDAY, "星期一",
			DayOfWeek.TUESDAY, "星期二",
			DayOfWeek.WEDNESDAY, "星期三",
			DayOfWeek.THURSDAY, "星期四",
			DayOfWeek.FRIDAY, "星期五",
			DayOfWeek.SATURDAY, "星期六",
			DayOfWeek.SUNDAY, "星期日");

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

	@Transactional(readOnly = true)
	public boolean isClassDay(Long classRoomId, LocalDate attendanceDate) {
		ClassRoom classRoom = classRoomService.findById(classRoomId);
		LocalDate targetDate = attendanceDate == null ? LocalDate.now() : attendanceDate;
		String weekday = WEEKDAY_NAMES.get(targetDate.getDayOfWeek());
		return classRoom.getEffectiveSchedules().stream()
				.anyMatch(schedule -> weekday.equals(schedule.getWeekday()));
	}

	@Transactional(readOnly = true)
	public LocalDate resolveClassDay(Long classRoomId, LocalDate attendanceDate) {
		LocalDate targetDate = attendanceDate == null ? LocalDate.now() : attendanceDate;
		if (isClassDay(classRoomId, targetDate)) {
			return targetDate;
		}
		List<DayOfWeek> classDays = classDays(classRoomId);
		if (classDays.isEmpty()) {
			return targetDate;
		}
		return shiftToClassDay(targetDate, classDays, 1, true);
	}

	@Transactional(readOnly = true)
	public LocalDate previousClassDay(Long classRoomId, LocalDate attendanceDate) {
		LocalDate targetDate = attendanceDate == null ? LocalDate.now() : attendanceDate;
		List<DayOfWeek> classDays = classDays(classRoomId);
		return classDays.isEmpty() ? targetDate.minusDays(1) : shiftToClassDay(targetDate.minusDays(1), classDays, -1, true);
	}

	@Transactional(readOnly = true)
	public LocalDate nextClassDay(Long classRoomId, LocalDate attendanceDate) {
		LocalDate targetDate = attendanceDate == null ? LocalDate.now() : attendanceDate;
		List<DayOfWeek> classDays = classDays(classRoomId);
		return classDays.isEmpty() ? targetDate.plusDays(1) : shiftToClassDay(targetDate.plusDays(1), classDays, 1, true);
	}

	@Transactional(readOnly = true)
	public List<Integer> classDayValues(Long classRoomId) {
		return classDays(classRoomId).stream()
				.map(DayOfWeek::getValue)
				.toList();
	}

	public void saveAttendance(Long classRoomId, StudentAttendanceForm form) {
		ClassRoom classRoom = classRoomService.findById(classRoomId);
		LocalDate attendanceDate = form.getAttendanceDate() == null ? LocalDate.now() : form.getAttendanceDate();
		if (!isClassDay(classRoomId, attendanceDate)) {
			throw new IllegalArgumentException("此日期不是班級上課日，無法儲存點名");
		}
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

	private List<DayOfWeek> classDays(Long classRoomId) {
		ClassRoom classRoom = classRoomService.findById(classRoomId);
		return classRoom.getEffectiveSchedules().stream()
				.map(schedule -> weekday(schedule.getWeekday()))
				.filter(day -> day != null)
				.distinct()
				.toList();
	}

	private LocalDate shiftToClassDay(LocalDate date, List<DayOfWeek> classDays, int direction, boolean includeDate) {
		LocalDate candidate = date;
		if (!includeDate) {
			candidate = candidate.plusDays(direction);
		}
		for (int i = 0; i < 7; i += 1) {
			if (classDays.contains(candidate.getDayOfWeek())) {
				return candidate;
			}
			candidate = candidate.plusDays(direction);
		}
		return date;
	}

	private DayOfWeek weekday(String weekdayName) {
		for (Map.Entry<DayOfWeek, String> entry : WEEKDAY_NAMES.entrySet()) {
			if (entry.getValue().equals(weekdayName)) {
				return entry.getKey();
			}
		}
		return null;
	}
}
