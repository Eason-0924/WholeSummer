package com.example.cramschool.service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.entity.ClassRoom;
import com.example.cramschool.entity.ClassSchedule;
import com.example.cramschool.entity.ClassStudent;
import com.example.cramschool.entity.MakeUpClassRequest;
import com.example.cramschool.entity.MakeUpStatus;
import com.example.cramschool.entity.ScheduleType;
import com.example.cramschool.repository.ClassRoomRepository;
import com.example.cramschool.repository.ClassScheduleRepository;
import com.example.cramschool.repository.ClassStudentRepository;
import com.example.cramschool.repository.MakeUpClassRequestRepository;

@Service
@Transactional(readOnly = true)
public class ScheduleConflictService {

	private static final Map<DayOfWeek, String> WEEKDAY_NAMES = Map.of(
			DayOfWeek.MONDAY, "星期一",
			DayOfWeek.TUESDAY, "星期二",
			DayOfWeek.WEDNESDAY, "星期三",
			DayOfWeek.THURSDAY, "星期四",
			DayOfWeek.FRIDAY, "星期五",
			DayOfWeek.SATURDAY, "星期六",
			DayOfWeek.SUNDAY, "星期日");

	private final ClassStudentRepository classStudentRepository;
	private final ClassRoomRepository classRoomRepository;
	private final MakeUpClassRequestRepository makeUpClassRequestRepository;
	private final ClassScheduleRepository classScheduleRepository;

	public ScheduleConflictService(ClassStudentRepository classStudentRepository,
			ClassRoomRepository classRoomRepository,
			MakeUpClassRequestRepository makeUpClassRequestRepository,
			ClassScheduleRepository classScheduleRepository) {
		this.classStudentRepository = classStudentRepository;
		this.classRoomRepository = classRoomRepository;
		this.makeUpClassRequestRepository = makeUpClassRequestRepository;
		this.classScheduleRepository = classScheduleRepository;
	}

	public boolean hasConflict(Long classId, Long teacherId, LocalDateTime start, LocalDateTime end) {
		ScheduleConflictContext context = buildContext(classId, teacherId);
		return hasStudentClassConflict(context, start, end)
				|| hasTeacherConflict(context, start, end)
				|| hasMakeUpConflict(context, classId, teacherId, start, end)
				|| hasRescheduleConflict(classId, teacherId, start, end);
	}

	public ScheduleConflictContext buildContext(Long classId, Long teacherId) {
		List<ClassRoom> studentClassRooms = studentClassRooms(classId);
		List<ClassRoom> teacherClassRooms = teacherId == null
				? List.of()
				: classRoomRepository.findByTeacherIdAndActiveTrue(teacherId);
		List<MakeUpClassRequest> scheduledMakeUps = makeUpClassRequestRepository
				.findByStatusOrderByOriginalCourseDateAscIdAsc(MakeUpStatus.SCHEDULED);
		return new ScheduleConflictContext(studentClassRooms, teacherClassRooms, scheduledMakeUps);
	}

	public boolean hasStudentClassConflict(Long classId, LocalDateTime start, LocalDateTime end) {
		return hasStudentClassConflict(buildContext(classId, null), start, end);
	}

	public boolean hasStudentClassConflict(ScheduleConflictContext context, LocalDateTime start, LocalDateTime end) {
		return context.studentClassRooms().stream()
				.anyMatch(classRoom -> classRoomConflicts(classRoom, start, end));
	}

	private List<ClassRoom> studentClassRooms(Long classId) {
		if (classId == null) {
			return List.of();
		}
		List<Long> studentIds = classStudentRepository
				.findByClassRoomIdAndActiveTrueOrderByStudentChineseNameAsc(classId)
				.stream()
				.map(classStudent -> classStudent.getStudent().getId())
				.toList();
		if (studentIds.isEmpty()) {
			return classRoomRepository.findById(classId)
					.filter(ClassRoom::isActive)
					.map(List::of)
					.orElseGet(List::of);
		}
		Set<ClassRoom> checkedClassRooms = classStudentRepository.findByStudentIdInAndActiveTrue(studentIds)
				.stream()
				.map(ClassStudent::getClassRoom)
				.filter(ClassRoom::isActive)
				.collect(Collectors.toSet());
		return List.copyOf(checkedClassRooms);
	}

	public boolean hasTeacherConflict(Long teacherId, LocalDateTime start, LocalDateTime end) {
		return hasTeacherConflict(buildContext(null, teacherId), start, end);
	}

	public boolean hasTeacherConflict(ScheduleConflictContext context, LocalDateTime start, LocalDateTime end) {
		return context.teacherClassRooms().stream()
				.anyMatch(classRoom -> classRoomConflicts(classRoom, start, end));
	}

	public List<String> teacherConflictDetails(Long teacherId, LocalDateTime start, LocalDateTime end) {
		if (teacherId == null) {
			return List.of();
		}
		List<String> recurringConflicts = classRoomRepository.findByTeacherIdAndActiveTrue(teacherId).stream()
				.flatMap(classRoom -> classRoom.getEffectiveSchedules().stream()
						.filter(schedule -> scheduleConflicts(schedule, start, end))
						.map(schedule -> classRoom.getDisplayName() + " "
								+ schedule.getWeekday() + " " + schedule.getTimeRangeText()))
				.toList();
		List<String> eventConflicts = classScheduleRepository.findByScheduledStartAtBetweenOrderByScheduledStartAtAsc(
				start.minusDays(1), end.plusDays(1)).stream()
				.filter(schedule -> schedule.getScheduleType() != ScheduleType.CANCELLED)
				.filter(schedule -> schedule.getClassRoom() != null
						&& schedule.getClassRoom().getTeacher() != null
						&& teacherId.equals(schedule.getClassRoom().getTeacher().getId()))
				.filter(schedule -> schedule.getScheduledStartAt() != null && schedule.getScheduledEndAt() != null)
				.filter(schedule -> start.isBefore(schedule.getScheduledEndAt())
						&& end.isAfter(schedule.getScheduledStartAt()))
				.map(schedule -> schedule.getScheduleType().getDisplayName() + "："
						+ schedule.getClassRoom().getDisplayName() + " "
						+ schedule.getScheduledStartAt().toLocalDate() + " "
						+ schedule.getStartTime() + " ~ " + schedule.getEndTime())
				.toList();
		return java.util.stream.Stream.concat(recurringConflicts.stream(), eventConflicts.stream())
				.distinct()
				.toList();
	}

	public boolean hasMakeUpConflict(Long classId, Long teacherId, LocalDateTime start, LocalDateTime end) {
		return hasMakeUpConflict(buildContext(classId, teacherId), classId, teacherId, start, end);
	}

	public boolean hasMakeUpConflict(ScheduleConflictContext context, Long classId, Long teacherId,
			LocalDateTime start, LocalDateTime end) {
		return hasMakeUpConflict(context, classId, teacherId, start, end, null);
	}

	public boolean hasMakeUpConflict(ScheduleConflictContext context, Long classId, Long teacherId,
			LocalDateTime start, LocalDateTime end, Long excludedRequestId) {
		return context.scheduledMakeUps().stream()
				.filter(request -> excludedRequestId == null || !excludedRequestId.equals(request.getId()))
				.filter(request -> (classId != null && request.getClassRoom() != null
						&& classId.equals(request.getClassRoom().getId()))
						|| (teacherId != null && request.getTeacher() != null
								&& teacherId.equals(request.getTeacher().getId())))
				.anyMatch(request -> request.getSelectedMakeUpStart() != null
						&& request.getSelectedMakeUpEnd() != null
						&& overlaps(start.toLocalTime(), end.toLocalTime(),
								request.getSelectedMakeUpStart().toLocalTime(),
								request.getSelectedMakeUpEnd().toLocalTime())
						&& start.toLocalDate().equals(request.getSelectedMakeUpStart().toLocalDate()));
	}

	public boolean hasRescheduleConflict(Long classId, Long teacherId, LocalDateTime start, LocalDateTime end) {
		return hasRescheduleConflict(classId, teacherId, start, end, null);
	}

	public boolean hasRescheduleConflict(Long classId, Long teacherId, LocalDateTime start, LocalDateTime end,
			Long excludedScheduleId) {
		return classScheduleRepository.findByScheduledStartAtBetweenOrderByScheduledStartAtAsc(
				start.minusDays(1), end.plusDays(1)).stream()
				.filter(schedule -> excludedScheduleId == null || !excludedScheduleId.equals(schedule.getId()))
				.filter(schedule -> schedule.getScheduleType() != ScheduleType.CANCELLED)
				.filter(schedule -> schedule.getScheduledStartAt() != null && schedule.getScheduledEndAt() != null)
				.filter(schedule -> (classId != null && schedule.getClassRoom() != null
						&& classId.equals(schedule.getClassRoom().getId()))
						|| (teacherId != null && schedule.getClassRoom() != null
								&& schedule.getClassRoom().getTeacher() != null
								&& teacherId.equals(schedule.getClassRoom().getTeacher().getId())))
				.anyMatch(schedule -> start.isBefore(schedule.getScheduledEndAt())
						&& end.isAfter(schedule.getScheduledStartAt()));
	}

	private boolean classRoomConflicts(ClassRoom classRoom, LocalDateTime start, LocalDateTime end) {
		return classRoom.getEffectiveSchedules().stream()
				.anyMatch(schedule -> scheduleConflicts(schedule, start, end));
	}

	private boolean scheduleConflicts(ClassSchedule schedule, LocalDateTime start, LocalDateTime end) {
		String weekday = WEEKDAY_NAMES.get(start.getDayOfWeek());
		return weekday.equals(schedule.getWeekday())
				&& overlaps(start.toLocalTime(), end.toLocalTime(),
						schedule.getStartTime(), schedule.getEndTime());
	}

	private boolean overlaps(LocalTime candidateStart, LocalTime candidateEnd,
			LocalTime existingStart, LocalTime existingEnd) {
		return candidateStart.isBefore(existingEnd) && candidateEnd.isAfter(existingStart);
	}

	public record ScheduleConflictContext(
			List<ClassRoom> studentClassRooms,
			List<ClassRoom> teacherClassRooms,
			List<MakeUpClassRequest> scheduledMakeUps) {
	}
}
