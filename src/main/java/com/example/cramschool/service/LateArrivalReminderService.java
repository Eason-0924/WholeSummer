package com.example.cramschool.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.dto.WeeklyScheduleDto;
import com.example.cramschool.entity.ClassStudent;
import com.example.cramschool.repository.ClassRoomRepository;
import com.example.cramschool.repository.ClassStudentRepository;
import com.example.cramschool.repository.StudentAttendanceRepository;

@Service
@Transactional
public class LateArrivalReminderService {

	private static final long OCCURRENCE_REFERENCE_MULTIPLIER = 1_000_000_000L;

	private final WeeklyScheduleService weeklyScheduleService;
	private final ClassStudentRepository classStudentRepository;
	private final StudentAttendanceRepository studentAttendanceRepository;
	private final LineNotificationService lineNotificationService;
	private final ClassRoomRepository classRoomRepository;
	private final WebPushEventNotificationService webPushEventNotificationService;
	private Clock clock = Clock.systemDefaultZone();

	public LateArrivalReminderService(WeeklyScheduleService weeklyScheduleService,
			ClassStudentRepository classStudentRepository,
			StudentAttendanceRepository studentAttendanceRepository,
			LineNotificationService lineNotificationService, ClassRoomRepository classRoomRepository,
			WebPushEventNotificationService webPushEventNotificationService) {
		this.weeklyScheduleService = weeklyScheduleService;
		this.classStudentRepository = classStudentRepository;
		this.studentAttendanceRepository = studentAttendanceRepository;
		this.lineNotificationService = lineNotificationService;
		this.classRoomRepository = classRoomRepository;
		this.webPushEventNotificationService = webPushEventNotificationService;
	}

	@Scheduled(fixedDelayString = "${line.late-reminder.scan-delay-ms:60000}",
			initialDelayString = "${line.late-reminder.initial-delay-ms:60000}")
	public void sendDueLateArrivalReminders() {
		sendDueLateArrivalReminders(LocalDateTime.now(clock));
	}

	void sendDueLateArrivalReminders(LocalDateTime now) {
		if (now == null) {
			return;
		}
		LocalDate today = now.toLocalDate();
		for (WeeklyScheduleDto schedule : weeklyScheduleService.findWeeklySchedules(today, null, true, null, null)) {
			if (!isDueSchedule(schedule, now)) {
				continue;
			}
			sendReminderForSchedule(schedule);
		}
	}

	private boolean isDueSchedule(WeeklyScheduleDto schedule, LocalDateTime now) {
		if (schedule == null
				|| schedule.getScheduleId() == null
				|| schedule.getClassRoomId() == null
				|| schedule.getCourseDate() == null
				|| schedule.getStartTime() == null
				|| schedule.getEndTime() == null
				|| Boolean.TRUE.equals(schedule.getIsCancelled())) {
			return false;
		}
		return schedule.getCourseDate().equals(now.toLocalDate())
				&& !now.isBefore(schedule.getStartTime().plusMinutes(5))
				&& !now.isAfter(schedule.getEndTime());
	}

	private void sendReminderForSchedule(WeeklyScheduleDto schedule) {
		Long referenceId = occurrenceReferenceId(schedule);
		Long responsibleTeacherId = classRoomRepository.findById(schedule.getClassRoomId())
				.map(classRoom -> classRoom.getTeacher() == null ? null : classRoom.getTeacher().getId())
				.orElse(null);
		for (ClassStudent classStudent : classStudentRepository
				.findByClassRoomIdAndActiveTrueOrderByStudentChineseNameAsc(schedule.getClassRoomId())) {
			if (classStudent.getStudent() == null || !classStudent.getStudent().isActive()) {
				continue;
			}
			boolean arrived = studentAttendanceRepository.existsByClassRoomIdAndStudentIdAndAttendanceDate(
					schedule.getClassRoomId(), classStudent.getStudent().getId(), schedule.getCourseDate());
			if (arrived) {
				continue;
			}
			if (lineNotificationService != null && lineNotificationService.isLineEnabled()) {
				lineNotificationService.sendLateArrivalReminder(
						classStudent.getStudent(), referenceId, schedule.getClassName(), schedule.getStartTime());
			}
			webPushEventNotificationService.notifyLateArrival(
					classStudent.getStudent().getDisplayName(), schedule.getClassName(), responsibleTeacherId);
		}
	}

	private Long occurrenceReferenceId(WeeklyScheduleDto schedule) {
		return schedule.getCourseDate().toEpochDay() * OCCURRENCE_REFERENCE_MULTIPLIER
				+ schedule.getScheduleId();
	}

	void setClock(Clock clock) {
		this.clock = clock == null ? Clock.systemDefaultZone() : clock;
	}
}
