package com.example.cramschool.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.entity.ClassRoom;
import com.example.cramschool.entity.ClassSchedule;
import com.example.cramschool.entity.LeaveStatus;
import com.example.cramschool.entity.Teacher;
import com.example.cramschool.entity.TeacherAttendance;
import com.example.cramschool.entity.TeacherAttendanceStatus;
import com.example.cramschool.repository.ClassScheduleRepository;
import com.example.cramschool.repository.TeacherAttendanceRepository;
import com.example.cramschool.repository.TeacherLeaveRepository;

@Service
@Transactional
public class AbsenceService {

	private static final int ABSENCE_GRACE_MINUTES = 15;
	private static final Map<DayOfWeek, String> WEEKDAY_NAMES = Map.of(
			DayOfWeek.MONDAY, "星期一",
			DayOfWeek.TUESDAY, "星期二",
			DayOfWeek.WEDNESDAY, "星期三",
			DayOfWeek.THURSDAY, "星期四",
			DayOfWeek.FRIDAY, "星期五",
			DayOfWeek.SATURDAY, "星期六",
			DayOfWeek.SUNDAY, "星期日");

	private final ClassScheduleRepository classScheduleRepository;
	private final TeacherAttendanceRepository teacherAttendanceRepository;
	private final TeacherLeaveRepository teacherLeaveRepository;
	private final MakeUpClassService makeUpClassService;

	public AbsenceService(ClassScheduleRepository classScheduleRepository,
			TeacherAttendanceRepository teacherAttendanceRepository,
			TeacherLeaveRepository teacherLeaveRepository,
			MakeUpClassService makeUpClassService) {
		this.classScheduleRepository = classScheduleRepository;
		this.teacherAttendanceRepository = teacherAttendanceRepository;
		this.teacherLeaveRepository = teacherLeaveRepository;
		this.makeUpClassService = makeUpClassService;
	}

	@Scheduled(fixedRate = 300000)
	public void autoMarkAbsences() {
		markAbsencesUntil(LocalDateTime.now());
	}

	void markAbsencesUntil(LocalDateTime now) {
		LocalDate today = now.toLocalDate();
		String weekday = WEEKDAY_NAMES.get(today.getDayOfWeek());
		for (ClassSchedule schedule : classScheduleRepository.findByWeekday(weekday)) {
			if (!shouldMarkAbsent(schedule, today, now.toLocalTime())) {
				continue;
			}
			ClassRoom classRoom = schedule.getClassRoom();
			Teacher teacher = classRoom.getTeacher();
			TeacherAttendance attendance = new TeacherAttendance();
			attendance.setTeacher(teacher);
			attendance.setDate(today);
			attendance.setStatus(TeacherAttendanceStatus.ABSENT);
			attendance.setWorkMinutes(0L);
			attendance.setMatchedCourseId(schedule.getId());
			attendance.setMatchedCourseName(classRoom.getDisplayName());
			attendance.setMatchedCourseTimeText(schedule.getTimeRangeText());
			attendance.setScheduledTimeText(schedule.getTimeRangeText());
			attendance.setNote("系統自動記錄缺勤");
			TeacherAttendance savedAttendance = teacherAttendanceRepository.save(attendance);
			makeUpClassService.createRequiredMakeUpFromAbsence(savedAttendance);
		}
	}

	private boolean shouldMarkAbsent(ClassSchedule schedule, LocalDate date, LocalTime now) {
		ClassRoom classRoom = schedule.getClassRoom();
		Teacher teacher = classRoom == null ? null : classRoom.getTeacher();
		if (classRoom == null || !classRoom.isActive() || teacher == null || schedule.getEndTime() == null) {
			return false;
		}
		if (!schedule.getEndTime().plusMinutes(ABSENCE_GRACE_MINUTES).isBefore(now)) {
			return false;
		}
		Long teacherId = teacher.getId();
		if (teacherAttendanceRepository.findByTeacherIdAndDate(teacherId, date).isPresent()) {
			return false;
		}
		return !teacherLeaveRepository.existsByTeacherIdAndLeaveDateAndCourseScheduleIdAndStatus(
				teacherId, date, schedule.getId(), LeaveStatus.APPROVED);
	}
}
