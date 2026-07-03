package com.example.cramschool.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.dto.TeacherAttendanceStats;
import com.example.cramschool.dto.TeacherDailySchedule;
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
	private final MakeUpClassService makeUpClassService;

	@Autowired
	public TeacherAttendanceService(TeacherAttendanceRepository teacherAttendanceRepository,
			TeacherRepository teacherRepository, TeacherScheduleService teacherScheduleService,
			MakeUpClassService makeUpClassService) {
		this.teacherAttendanceRepository = teacherAttendanceRepository;
		this.teacherRepository = teacherRepository;
		this.teacherScheduleService = teacherScheduleService;
		this.makeUpClassService = makeUpClassService;
	}

	TeacherAttendanceService(TeacherAttendanceRepository teacherAttendanceRepository,
			TeacherRepository teacherRepository, TeacherScheduleService teacherScheduleService) {
		this(teacherAttendanceRepository, teacherRepository, teacherScheduleService, null);
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
			attendance.setManualAdjustmentAllowed(canManuallyAdjust(attendance));
		}
		return teacherAttendanceRepository.saveAll(attendances);
	}

	@Transactional(readOnly = true)
	public List<TeacherAttendance> findByDate(LocalDate date) {
		return teacherAttendanceRepository.findByDateOrderByTeacherNameAsc(date == null ? LocalDate.now() : date);
	}

	@Transactional(readOnly = true)
	public Optional<TeacherAttendance> findByTeacherIdAndDate(Long teacherId, LocalDate date) {
		return teacherAttendanceRepository.findByTeacherIdAndDate(
				teacherId, date == null ? LocalDate.now() : date);
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
		Optional<TeacherAttendance> existingAttendance = teacherAttendanceRepository
				.findByTeacherIdAndDate(form.getTeacherId(), date);
		TeacherAttendance attendance = existingAttendance.orElseGet(TeacherAttendance::new);
		TeacherAttendanceStatus previousStatus = existingAttendance
				.map(TeacherAttendance::getStatus)
				.orElse(null);
		attendance.setTeacher(teacher);
		attendance.setDate(date);
		attendance.setClockInTime(form.getClockInTime());
		attendance.setClockOutTime(form.getClockOutTime());
		attendance.setNote(form.getNote());
		TeacherAttendanceStatus requestedStatus = form.getStatus() == null
				? TeacherAttendanceStatus.WORKING
				: form.getStatus();
		applySchedule(attendance, requestedStatus);
		TeacherAttendance savedAttendance = teacherAttendanceRepository.save(attendance);
		reconcileAbsenceMakeUp(previousStatus, savedAttendance);
		return savedAttendance;
	}

	public TeacherAttendance quickClock(Long currentTeacherId) {
		Teacher teacher = teacherRepository.findById(currentTeacherId)
				.orElseThrow(() -> new IllegalArgumentException("找不到教師資料"));
		LocalDate today = LocalDate.now();
		Optional<TeacherAttendance> existingAttendance = teacherAttendanceRepository
				.findByTeacherIdAndDate(currentTeacherId, today);
		TeacherAttendance attendance = existingAttendance.orElseGet(TeacherAttendance::new);
		TeacherAttendanceStatus previousStatus = existingAttendance
				.map(TeacherAttendance::getStatus)
				.orElse(null);
		attendance.setTeacher(teacher);
		attendance.setDate(today);
		if (attendance.getClockInTime() == null) {
			attendance.setClockInTime(LocalTime.now().withSecond(0).withNano(0));
		} else if (attendance.getClockOutTime() == null) {
			attendance.setClockOutTime(LocalTime.now().withSecond(0).withNano(0));
		} else {
			throw new IllegalStateException("今日已完成上下班打卡");
		}
		applySchedule(attendance, TeacherAttendanceStatus.WORKING);
		TeacherAttendance savedAttendance = teacherAttendanceRepository.save(attendance);
		reconcileAbsenceMakeUp(previousStatus, savedAttendance);
		return savedAttendance;
	}

	public TeacherAttendance cardClock(Teacher teacher, String cardId, String deviceName, LocalDateTime clockAt) {
		if (teacher == null || teacher.getId() == null) {
			throw new IllegalArgumentException("找不到教師資料");
		}
		LocalDateTime targetTime = clockAt == null ? LocalDateTime.now() : clockAt;
		LocalDate today = targetTime.toLocalDate();
		Optional<TeacherAttendance> existingAttendance = teacherAttendanceRepository
				.findByTeacherIdAndDate(teacher.getId(), today);
		TeacherAttendance attendance = existingAttendance.orElseGet(TeacherAttendance::new);
		TeacherAttendanceStatus previousStatus = existingAttendance
				.map(TeacherAttendance::getStatus)
				.orElse(null);
		attendance.setTeacher(teacher);
		attendance.setDate(today);
		if (attendance.getClockInTime() == null) {
			attendance.setClockInTime(targetTime.toLocalTime().withSecond(0).withNano(0));
		} else if (attendance.getClockOutTime() == null) {
			attendance.setClockOutTime(targetTime.toLocalTime().withSecond(0).withNano(0));
		} else {
			throw new IllegalStateException("今日已完成上下班打卡");
		}
		attendance.setCheckMethod("CARD");
		attendance.setDeviceName(normalizeDeviceName(deviceName));
		attendance.setCardId(cardId);
		applySchedule(attendance, TeacherAttendanceStatus.WORKING);
		TeacherAttendance savedAttendance = teacherAttendanceRepository.save(attendance);
		reconcileAbsenceMakeUp(previousStatus, savedAttendance);
		return savedAttendance;
	}

	public void clockIn(Long teacherId) {
		TeacherAttendance attendance = teacherAttendanceRepository.findByTeacherIdAndDate(teacherId, LocalDate.now())
				.orElse(null);
		if (attendance != null && attendance.getClockInTime() != null) {
			throw new IllegalStateException("今天已完成上班打卡");
		}
		quickClock(teacherId);
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
		recordLeave(teacherId, LocalDate.now(), "請假");
	}

	public TeacherAttendance recordLeave(Long teacherId, LocalDate leaveDate, String note) {
		Teacher teacher = teacherRepository.findById(teacherId)
				.orElseThrow(() -> new IllegalArgumentException("找不到教師資料"));
		LocalDate targetDate = leaveDate == null ? LocalDate.now() : leaveDate;
		TeacherAttendance attendance = teacherAttendanceRepository.findByTeacherIdAndDate(teacherId, targetDate)
				.orElseGet(TeacherAttendance::new);
		attendance.setTeacher(teacher);
		attendance.setDate(targetDate);
		attendance.setClockInTime(null);
		attendance.setClockOutTime(null);
		attendance.setNote(note);
		applySchedule(attendance, TeacherAttendanceStatus.LEAVE);
		return teacherAttendanceRepository.save(attendance);
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
		if (!canManuallyAdjust(attendance)) {
			throw new IllegalArgumentException(
					"只有無對應課程，或打卡時間超出課程前後一小時的紀錄可手動調整");
		}
		if (manualHours == null) {
			throw new IllegalArgumentException("請輸入增加時數");
		}
		if (manualHours.compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException("增加時數不可小於 0");
		}
		if (manualHours.compareTo(BigDecimal.valueOf(24)) > 0) {
			throw new IllegalArgumentException("增加時數不可超過 24 小時");
		}
		if (manualHours.remainder(new BigDecimal("0.5")).compareTo(BigDecimal.ZERO) != 0) {
			throw new IllegalArgumentException("增加時數須以 0.5 小時為間距");
		}
		BigDecimal normalizedManualHours = manualHours.setScale(1);
		String normalizedRemark = manualRemark == null ? null : manualRemark.trim();
		if (normalizedRemark != null && normalizedRemark.length() > 255) {
			throw new IllegalArgumentException("備註不可超過 255 個字");
		}
		attendance.setManualRemark(normalizedRemark == null || normalizedRemark.isBlank()
				? null : normalizedRemark);
		attendance.setManualHours(normalizedManualHours);
		attendance.setManualAdjusted(true);
		attendance.setAdjustedByTeacherId(currentTeacherId);
		attendance.setAdjustedAt(LocalDateTime.now());
	}

	boolean canManuallyAdjust(TeacherAttendance attendance) {
		if (attendance == null) {
			return false;
		}
		if (attendance.isManualAdjusted() || !attendance.hasMatchedCourse()) {
			return true;
		}
		TeacherDailySchedule schedule = teacherScheduleService.findDailySchedule(
				attendance.getTeacher().getId(), attendance.getDate());
		return isMoreThanOneHourOutsideSchedule(attendance, schedule);
	}

	boolean isMoreThanOneHourOutsideSchedule(TeacherAttendance attendance,
			TeacherDailySchedule schedule) {
		if (attendance == null || schedule == null) {
			return false;
		}
		LocalTime firstStartTime = schedule.getFirstStartTime();
		LocalTime lastEndTime = schedule.getLastEndTime();
		boolean clockedInTooEarly = attendance.getClockInTime() != null
				&& firstStartTime != null
				&& Duration.between(attendance.getClockInTime(), firstStartTime).toMinutes() > 60;
		boolean clockedOutTooLate = attendance.getClockOutTime() != null
				&& lastEndTime != null
				&& Duration.between(lastEndTime, attendance.getClockOutTime()).toMinutes() > 60;
		return clockedInTooEarly || clockedOutTooLate;
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

	private void reconcileAbsenceMakeUp(TeacherAttendanceStatus previousStatus, TeacherAttendance attendance) {
		if (makeUpClassService == null || attendance == null || attendance.getId() == null) {
			return;
		}
		if (previousStatus == TeacherAttendanceStatus.ABSENT
				&& (attendance.getStatus() == TeacherAttendanceStatus.WORKING
						|| attendance.getStatus() == TeacherAttendanceStatus.LATE)) {
			makeUpClassService.deleteAbsenceMakeUpForAttendance(attendance.getId());
		}
	}

	private String normalizeDeviceName(String deviceName) {
		if (deviceName == null || deviceName.isBlank()) {
			return null;
		}
		String normalized = deviceName.trim();
		return normalized.length() > 100 ? normalized.substring(0, 100) : normalized;
	}

	TeacherAttendanceStatus resolveWorkingStatus(LocalTime clockInTime, LocalTime firstClassStart) {
		boolean late = clockInTime != null
				&& firstClassStart != null
				&& clockInTime.isAfter(firstClassStart);
		return late ? TeacherAttendanceStatus.LATE : TeacherAttendanceStatus.WORKING;
	}
}
