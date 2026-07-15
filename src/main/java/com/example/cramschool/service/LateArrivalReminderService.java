package com.example.cramschool.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Collections;

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
	private final Set<String> sentWebPushReminders = Collections.synchronizedSet(new HashSet<>());

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

	@Scheduled(cron = "${line.late-reminder.cron:0 * * * * *}", zone = "Asia/Taipei")
	public void sendDueLateArrivalReminders() {
		sendDueLateArrivalReminders(LocalDateTime.now(clock), true, true);
	}

	/**
	 * Kept as a compatibility entry point for existing scheduler/test wiring.
	 * The minute scheduler above already sends the browser notification together
	 * with LINE; the occurrence key prevents this fallback schedule from sending
	 * it a second time.
	 */
	@Scheduled(cron = "${webpush.late-arrival.cron:0 */10 * * * *}", zone = "Asia/Taipei")
	public void sendDueLateArrivalWebPushNotifications() {
		sendDueLateArrivalReminders(LocalDateTime.now(clock), false, true);
	}

	void sendDueLateArrivalReminders(LocalDateTime now) {
		sendDueLateArrivalReminders(now, true, true);
	}

	private void sendDueLateArrivalReminders(LocalDateTime now, boolean sendLine, boolean sendWebPush) {
		if (now == null) {
			return;
		}
		LocalDate today = now.toLocalDate();
		Set<Long> processedScheduleIds = new HashSet<>();
		List<LineNotificationService.LateArrivalReminder> lineReminders = new ArrayList<>();
		for (WeeklyScheduleDto schedule : weeklyScheduleService.findWeeklySchedules(today, null, true, null, null)) {
			if (schedule == null || !processedScheduleIds.add(schedule.getScheduleId())) {
				continue;
			}
			if (!isDueSchedule(schedule, now)) {
				continue;
			}
			sendReminderForSchedule(schedule, sendLine, sendWebPush, lineReminders);
		}
		if (sendLine && !lineReminders.isEmpty() && lineNotificationService != null && lineNotificationService.isLineEnabled()) {
			lineNotificationService.sendLateArrivalReminders(lineReminders);
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
				&& now.isAfter(schedule.getStartTime().plusMinutes(5))
				&& !now.isAfter(schedule.getEndTime());
	}

	private void sendReminderForSchedule(WeeklyScheduleDto schedule, boolean sendLine, boolean sendWebPush,
			List<LineNotificationService.LateArrivalReminder> lineReminders) {
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
					schedule.getClassRoomId(), classStudent.getStudent().getId(), schedule.getCourseDate())
					|| studentAttendanceRepository.existsByStudentIdAndAttendanceDateAndCheckInTimeIsNotNullAndCheckOutTimeIsNull(
							classStudent.getStudent().getId(), schedule.getCourseDate());
			if (arrived) {
				continue;
			}
			if (sendLine) {
				lineReminders.add(new LineNotificationService.LateArrivalReminder(
						classStudent.getStudent(), referenceId, schedule.getClassName(), schedule.getStartTime()));
			}
			if (sendWebPush && sentWebPushReminders.add(webPushReference(schedule, classStudent.getStudent().getId()))) {
				webPushEventNotificationService.notifyLateArrival(
						classStudent.getStudent().getDisplayName(), schedule.getClassName(), responsibleTeacherId);
			}
		}
	}

	private String webPushReference(WeeklyScheduleDto schedule, Long studentId) {
		return schedule.getCourseDate() + ":" + schedule.getScheduleId() + ":" + studentId;
	}

	private Long occurrenceReferenceId(WeeklyScheduleDto schedule) {
		return schedule.getCourseDate().toEpochDay() * OCCURRENCE_REFERENCE_MULTIPLIER
				+ schedule.getScheduleId();
	}

	void setClock(Clock clock) {
		this.clock = clock == null ? Clock.systemDefaultZone() : clock;
	}
}
