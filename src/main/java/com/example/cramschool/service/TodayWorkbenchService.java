package com.example.cramschool.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import com.example.cramschool.dto.TodayWorkbenchView;
import com.example.cramschool.dto.WeeklyScheduleDto;
import com.example.cramschool.entity.AttendanceStatus;
import com.example.cramschool.entity.ClassStudent;
import com.example.cramschool.entity.Exam;
import com.example.cramschool.entity.Homework;
import com.example.cramschool.entity.StudentAttendance;
import com.example.cramschool.repository.ExamRepository;
import com.example.cramschool.repository.HomeworkRepository;
import com.example.cramschool.repository.StudentAttendanceRepository;

@Service
@Transactional(readOnly = true)
public class TodayWorkbenchService {

	private final WeeklyScheduleService weeklyScheduleService;
	private final ClassStudentService classStudentService;
	private final StudentAttendanceRepository studentAttendanceRepository;
	private final HomeworkRepository homeworkRepository;
	private final ExamRepository examRepository;
	private final Clock clock;

	@Autowired
	public TodayWorkbenchService(WeeklyScheduleService weeklyScheduleService,
			ClassStudentService classStudentService,
			StudentAttendanceRepository studentAttendanceRepository,
			HomeworkRepository homeworkRepository,
			ExamRepository examRepository) {
		this(weeklyScheduleService, classStudentService, studentAttendanceRepository,
				homeworkRepository, examRepository, Clock.systemDefaultZone());
	}

	TodayWorkbenchService(WeeklyScheduleService weeklyScheduleService,
			ClassStudentService classStudentService,
			StudentAttendanceRepository studentAttendanceRepository,
			HomeworkRepository homeworkRepository,
			ExamRepository examRepository,
			Clock clock) {
		this.weeklyScheduleService = weeklyScheduleService;
		this.classStudentService = classStudentService;
		this.studentAttendanceRepository = studentAttendanceRepository;
		this.homeworkRepository = homeworkRepository;
		this.examRepository = examRepository;
		this.clock = clock;
	}

	public TodayWorkbenchView build(Long teacherId, boolean director) {
		LocalDate today = LocalDate.now(clock);
		List<WeeklyScheduleDto> todaySchedules = weeklyScheduleService.findWeeklySchedules(
				today, teacherId, director, null, null).stream()
				.filter(schedule -> today.equals(schedule.getCourseDate()))
				.filter(schedule -> !Boolean.TRUE.equals(schedule.getIsCancelled()))
				.filter(schedule -> schedule.getStartTime() != null && schedule.getEndTime() != null)
				.sorted(Comparator.comparing(WeeklyScheduleDto::getStartTime, Comparator.nullsLast(LocalDateTime::compareTo))
						.thenComparing(WeeklyScheduleDto::getClassName, Comparator.nullsLast(String::compareTo)))
				.toList();

		List<TodayWorkbenchView.TodayCourseItem> courses = todaySchedules.stream()
				.map(schedule -> new TodayWorkbenchView.TodayCourseItem(
						schedule.getClassRoomId(),
						schedule.getClassName(),
						schedule.getTeacherName(),
						schedule.getStartTime(),
						schedule.getEndTime(),
						Boolean.TRUE.equals(schedule.getIsWeeklyExam()) ? "週考" : schedule.getScheduleTypeDisplayName()))
				.toList();

		TodayWorkbenchView.TodayAttendanceSummary attendance = summarizeAttendance(
				todaySchedules, today, LocalDateTime.now(clock));

		List<TodayWorkbenchView.TodayHomeworkItem> homeworks = homeworkRepository
				.findByDueDateBetweenOrderByDueDateAscIdAsc(today, today).stream()
				.filter(homework -> director || belongsToTeacher(homework, teacherId))
				.sorted(Comparator.comparing((Homework homework) -> homework.getClassRoom().getDisplayName())
						.thenComparing(Homework::getTitle, Comparator.nullsLast(String::compareTo)))
				.map(homework -> new TodayWorkbenchView.TodayHomeworkItem(
						homework.getId(),
						homework.getTitle(),
						homework.getClassRoom().getDisplayName(),
						homework.getClassRoom().getTeacherName()))
				.toList();

		List<TodayWorkbenchView.TodayExamItem> exams = examRepository.findAllByOrderByExamDateDescIdDesc().stream()
				.filter(exam -> today.equals(exam.getExamDate()))
				.filter(exam -> director || belongsToTeacher(exam, teacherId))
				.sorted(Comparator.comparing((Exam exam) -> exam.getClassRoom().getDisplayName())
						.thenComparing(Exam::getName, Comparator.nullsLast(String::compareTo)))
				.map(exam -> new TodayWorkbenchView.TodayExamItem(
						exam.getId(),
						exam.getName(),
						exam.getClassRoom().getDisplayName(),
						exam.getClassRoom().getTeacherName()))
				.toList();

		return new TodayWorkbenchView(today, courses, attendance, homeworks, exams);
	}

	private TodayWorkbenchView.TodayAttendanceSummary summarizeAttendance(List<WeeklyScheduleDto> todaySchedules,
			LocalDate today, LocalDateTime now) {
		long presentCount = 0;
		long lateCount = 0;
		long missingCount = 0;
		long earlyLeaveCount = 0;
		List<String> presentNames = new ArrayList<>();
		List<String> lateNames = new ArrayList<>();
		List<String> missingNames = new ArrayList<>();
		List<String> earlyLeaveNames = new ArrayList<>();

		Map<Long, List<ClassStudent>> studentsByClassId = new LinkedHashMap<>();
		Map<Long, Map<Long, StudentAttendance>> attendancesByClassId = new LinkedHashMap<>();
		java.util.Set<Long> studentsOnCampus = studentAttendanceRepository
				.findByAttendanceDateAndCheckInTimeIsNotNullAndCheckOutTimeIsNull(today).stream()
				.map(attendance -> attendance.getStudent() == null ? null : attendance.getStudent().getId())
				.filter(java.util.Objects::nonNull)
				.collect(java.util.stream.Collectors.toSet());

		for (WeeklyScheduleDto schedule : todaySchedules) {
			Long classRoomId = schedule.getClassRoomId();
			if (classRoomId == null) {
				continue;
			}
			List<ClassStudent> students = studentsByClassId.computeIfAbsent(
					classRoomId, classStudentService::findActiveByClassRoomId);
			Map<Long, StudentAttendance> attendanceByStudentId = attendancesByClassId.computeIfAbsent(
					classRoomId, targetClassRoomId -> attendanceByStudentId(targetClassRoomId, today));
			for (ClassStudent classStudent : students) {
				StudentAttendance attendance = attendanceByStudentId.get(classStudent.getStudent().getId());
				if (attendance == null) {
					if (studentsOnCampus.contains(classStudent.getStudent().getId())) {
						presentCount++;
						presentNames.add(classStudent.getStudent().getDisplayName());
						continue;
					}
					if (hasClassStarted(schedule.getStartTime(), now)) {
						lateCount++;
						lateNames.add(classStudent.getStudent().getDisplayName());
					} else {
						missingCount++;
						missingNames.add(classStudent.getStudent().getDisplayName());
					}
					continue;
				}
				if (attendance.getStatus() == AttendanceStatus.PRESENT) {
					presentCount++;
					presentNames.add(classStudent.getStudent().getDisplayName());
				}
				if (attendance.getStatus() == AttendanceStatus.LATE) {
					lateCount++;
					lateNames.add(classStudent.getStudent().getDisplayName());
				}
				if (attendance.getStatus() == AttendanceStatus.ABSENT || attendance.getStatus() == AttendanceStatus.LEAVE) {
					missingCount++;
					missingNames.add(classStudent.getStudent().getDisplayName());
				}
				if (isEarlyLeave(attendance, schedule.getEndTime())) {
					earlyLeaveCount++;
					earlyLeaveNames.add(classStudent.getStudent().getDisplayName());
				}
			}
		}

		return new TodayWorkbenchView.TodayAttendanceSummary(
				presentCount, missingCount, lateCount, earlyLeaveCount,
				presentNames, missingNames, lateNames, earlyLeaveNames);
	}

	private Map<Long, StudentAttendance> attendanceByStudentId(Long classRoomId, LocalDate today) {
		Map<Long, StudentAttendance> records = new LinkedHashMap<>();
		for (StudentAttendance attendance : studentAttendanceRepository
				.findByClassRoomIdAndAttendanceDateOrderByStudentChineseNameAsc(classRoomId, today)) {
			records.putIfAbsent(attendance.getStudent().getId(), attendance);
		}
		return records;
	}

	private boolean hasClassStarted(LocalDateTime startTime, LocalDateTime now) {
		return startTime != null && !now.isBefore(startTime);
	}

	private boolean isEarlyLeave(StudentAttendance attendance, LocalDateTime latestEndTime) {
		if (attendance == null || latestEndTime == null || attendance.getCheckOutTime() == null) {
			return false;
		}
		return attendance.getCheckOutTime().isBefore(latestEndTime);
	}

	private boolean belongsToTeacher(Homework homework, Long teacherId) {
		return homework.getClassRoom() != null
				&& homework.getClassRoom().getTeacher() != null
				&& teacherId != null
				&& teacherId.equals(homework.getClassRoom().getTeacher().getId());
	}

	private boolean belongsToTeacher(Exam exam, Long teacherId) {
		return exam.getClassRoom() != null
				&& exam.getClassRoom().getTeacher() != null
				&& teacherId != null
				&& teacherId.equals(exam.getClassRoom().getTeacher().getId());
	}
}
