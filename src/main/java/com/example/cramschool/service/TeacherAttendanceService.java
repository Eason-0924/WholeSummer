package com.example.cramschool.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.dto.TeacherAttendanceStats;
import com.example.cramschool.dto.TeacherScheduleMatch;
import com.example.cramschool.entity.Teacher;
import com.example.cramschool.entity.TeacherAttendance;
import com.example.cramschool.entity.TeacherAttendanceStatus;
import com.example.cramschool.form.TeacherAttendanceForm;
import com.example.cramschool.repository.TeacherAttendanceRepository;
import com.example.cramschool.repository.TeacherRepository;

@Service
@Transactional
public class TeacherAttendanceService {

	private final TeacherAttendanceRepository teacherAttendanceRepository;
	private final TeacherRepository teacherRepository;
	private final TeacherScheduleService teacherScheduleService;

	public TeacherAttendanceService(TeacherAttendanceRepository teacherAttendanceRepository,
			TeacherRepository teacherRepository, TeacherScheduleService teacherScheduleService) {
		this.teacherAttendanceRepository = teacherAttendanceRepository;
		this.teacherRepository = teacherRepository;
		this.teacherScheduleService = teacherScheduleService;
	}

	@Transactional(readOnly = true)
	public List<TeacherAttendance> findByTeacherId(Long teacherId) {
		return teacherAttendanceRepository.findByTeacherIdOrderByDateDescIdDesc(teacherId);
	}

	public List<TeacherAttendance> findByTeacherIdAndMonth(Long teacherId, YearMonth month) {
		YearMonth targetMonth = month == null ? YearMonth.now() : month;
		List<TeacherAttendance> attendances = teacherAttendanceRepository.findByTeacherIdAndDateBetweenOrderByDateAsc(
				teacherId, targetMonth.atDay(1), targetMonth.atEndOfMonth());
		for (TeacherAttendance attendance : attendances) {
			if (!attendance.hasMatchedCourse() && !attendance.isManualAdjusted()
					&& (attendance.getStatus() == TeacherAttendanceStatus.WORKING
							|| attendance.getStatus() == TeacherAttendanceStatus.LATE)) {
				applySchedule(attendance, attendance.getStatus());
			}
		}
		return teacherAttendanceRepository.saveAll(attendances);
	}

	@Transactional(readOnly = true)
	public List<TeacherAttendance> findByDate(LocalDate date) {
		return teacherAttendanceRepository.findByDateOrderByTeacherNameAsc(date == null ? LocalDate.now() : date);
	}

	@Transactional(readOnly = true)
	public TeacherAttendanceStats calculateMonthlyStats(Long teacherId, YearMonth month) {
		YearMonth targetMonth = month == null ? YearMonth.now() : month;
		TeacherAttendanceStats stats = new TeacherAttendanceStats();
		for (TeacherAttendance attendance : findByTeacherId(teacherId)) {
			if (!YearMonth.from(attendance.getDate()).equals(targetMonth)) {
				continue;
			}
			if (attendance.getStatus() == TeacherAttendanceStatus.WORKING
					|| attendance.getStatus() == TeacherAttendanceStatus.LATE) {
				stats.setWorkingDays(stats.getWorkingDays() + 1);
				stats.setTotalWorkMinutes(stats.getTotalWorkMinutes() + attendance.getWorkMinutes());
				if (attendance.getStatus() == TeacherAttendanceStatus.LATE) {
					stats.setLateDays(stats.getLateDays() + 1);
				}
			} else if (attendance.getStatus() == TeacherAttendanceStatus.LEAVE) {
				stats.setLeaveDays(stats.getLeaveDays() + 1);
			} else if (attendance.getStatus() == TeacherAttendanceStatus.ABSENT) {
				stats.setAbsentDays(stats.getAbsentDays() + 1);
			}
		}
		return stats;
	}

	public TeacherAttendance save(TeacherAttendanceForm form) {
		Teacher teacher = teacherRepository.findById(form.getTeacherId())
				.orElseThrow(() -> new IllegalArgumentException("找不到教師資料"));
		LocalDate date = form.getDate() == null ? LocalDate.now() : form.getDate();
		TeacherAttendance attendance = teacherAttendanceRepository.findByTeacherIdAndDate(form.getTeacherId(), date)
				.orElseGet(TeacherAttendance::new);
		attendance.setTeacher(teacher);
		attendance.setDate(date);
		attendance.setClockInTime(form.getClockInTime());
		attendance.setClockOutTime(form.getClockOutTime());
		attendance.setNote(form.getNote());
		TeacherAttendanceStatus requestedStatus = form.getStatus() == null
				? TeacherAttendanceStatus.WORKING
				: form.getStatus();
		applySchedule(attendance, requestedStatus);
		return teacherAttendanceRepository.save(attendance);
	}

	public void clockIn(Long teacherId) {
		TeacherAttendanceForm form = todayForm(teacherId);
		form.setClockInTime(LocalTime.now().withSecond(0).withNano(0));
		form.setStatus(TeacherAttendanceStatus.WORKING);
		save(form);
	}

	public void clockOut(Long teacherId) {
		TeacherAttendance attendance = teacherAttendanceRepository.findByTeacherIdAndDate(teacherId, LocalDate.now())
				.orElseThrow(() -> new IllegalArgumentException("今天尚未上班打卡"));
		TeacherAttendanceForm form = TeacherAttendanceForm.from(attendance);
		form.setClockOutTime(LocalTime.now().withSecond(0).withNano(0));
		form.setStatus(TeacherAttendanceStatus.WORKING);
		save(form);
	}

	public void markLeave(Long teacherId) {
		TeacherAttendanceForm form = todayForm(teacherId);
		form.setStatus(TeacherAttendanceStatus.LEAVE);
		form.setClockInTime(null);
		form.setClockOutTime(null);
		save(form);
	}

	public void markAbsent(Long teacherId) {
		TeacherAttendanceForm form = todayForm(teacherId);
		form.setStatus(TeacherAttendanceStatus.ABSENT);
		form.setClockInTime(null);
		form.setClockOutTime(null);
		save(form);
	}

	public void deleteByTeacherId(Long teacherId) {
		teacherAttendanceRepository.deleteByTeacherId(teacherId);
	}

	public TeacherAttendance updateManualAdjustment(Long attendanceId, String manualRemark,
			BigDecimal manualHours, Long currentTeacherId, boolean currentTeacherIsDirector) {
		TeacherAttendance attendance = teacherAttendanceRepository.findById(attendanceId)
				.orElseThrow(() -> new IllegalArgumentException("找不到打卡紀錄"));
		applyManualAdjustment(attendance, manualRemark, manualHours,
				currentTeacherId, currentTeacherIsDirector);
		return teacherAttendanceRepository.save(attendance);
	}

	void applyManualAdjustment(TeacherAttendance attendance, String manualRemark,
			BigDecimal manualHours, Long currentTeacherId, boolean currentTeacherIsDirector) {
		if (!currentTeacherIsDirector) {
			throw new IllegalArgumentException("只有主任可以調整打卡紀錄");
		}
		if (attendance.hasMatchedCourse()) {
			throw new IllegalArgumentException("已有對應課程的打卡紀錄不可手動調整");
		}
		if (manualHours == null) {
			throw new IllegalArgumentException("請輸入上課時數");
		}
		if (manualHours.compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException("上課時數不可小於 0");
		}
		if (manualHours.compareTo(BigDecimal.valueOf(24)) > 0) {
			throw new IllegalArgumentException("上課時數不可超過 24 小時");
		}
		String normalizedRemark = manualRemark == null ? null : manualRemark.trim();
		if (normalizedRemark != null && normalizedRemark.length() > 255) {
			throw new IllegalArgumentException("備註不可超過 255 個字");
		}
		attendance.setManualRemark(normalizedRemark == null || normalizedRemark.isBlank()
				? null : normalizedRemark);
		attendance.setManualHours(manualHours);
		attendance.setManualAdjusted(true);
		attendance.setAdjustedByTeacherId(currentTeacherId);
		attendance.setAdjustedAt(LocalDateTime.now());
		attendance.setWorkMinutes(0L);
	}

	private TeacherAttendanceForm todayForm(Long teacherId) {
		TeacherAttendance existing = teacherAttendanceRepository.findByTeacherIdAndDate(teacherId, LocalDate.now())
				.orElse(null);
		if (existing != null) {
			return TeacherAttendanceForm.from(existing);
		}
		TeacherAttendanceForm form = new TeacherAttendanceForm();
		form.setTeacherId(teacherId);
		form.setDate(LocalDate.now());
		return form;
	}

	private void applySchedule(TeacherAttendance attendance, TeacherAttendanceStatus requestedStatus) {
		if (requestedStatus == TeacherAttendanceStatus.LEAVE || requestedStatus == TeacherAttendanceStatus.ABSENT) {
			attendance.setStatus(requestedStatus);
			attendance.setWorkMinutes(0L);
			attendance.setScheduledTimeText(null);
			clearCourseMatch(attendance);
			return;
		}

		TeacherScheduleMatch match = teacherScheduleService.findMatchedSchedule(
				attendance.getTeacher().getId(), attendance.getDate(),
				attendance.getClockInTime(), attendance.getClockOutTime());
		attendance.setMatchedCourseId(match.firstScheduleId());
		attendance.setMatchedCourseName(match.courseNames());
		attendance.setMatchedCourseTimeText(match.timeRangeText());
		attendance.setWorkMinutes(match.workMinutes());
		attendance.setScheduledTimeText(match.timeRangeText());
		attendance.setStatus(resolveWorkingStatus(
				attendance.getClockInTime(), match.firstStartTime()));
	}

	private void clearCourseMatch(TeacherAttendance attendance) {
		attendance.setMatchedCourseId(null);
		attendance.setMatchedCourseName(null);
		attendance.setMatchedCourseTimeText(null);
	}

	TeacherAttendanceStatus resolveWorkingStatus(LocalTime clockInTime, LocalTime firstClassStart) {
		boolean late = clockInTime != null
				&& firstClassStart != null
				&& clockInTime.isAfter(firstClassStart);
		return late ? TeacherAttendanceStatus.LATE : TeacherAttendanceStatus.WORKING;
	}
}
