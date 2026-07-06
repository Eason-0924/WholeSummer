package com.example.cramschool.service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.dto.TeacherDailySchedule;
import com.example.cramschool.dto.TeacherDailySchedule.TimeRange;
import com.example.cramschool.dto.TeacherScheduleMatch;
import com.example.cramschool.entity.ClassRoom;
import com.example.cramschool.entity.ClassSchedule;
import com.example.cramschool.repository.ClassRoomRepository;

@Service
@Transactional(readOnly = true)
public class TeacherScheduleService {

	private static final Map<DayOfWeek, String> WEEKDAY_NAMES = Map.of(
			DayOfWeek.MONDAY, "星期一",
			DayOfWeek.TUESDAY, "星期二",
			DayOfWeek.WEDNESDAY, "星期三",
			DayOfWeek.THURSDAY, "星期四",
			DayOfWeek.FRIDAY, "星期五",
			DayOfWeek.SATURDAY, "星期六",
			DayOfWeek.SUNDAY, "星期日");

	private final ClassRoomRepository classRoomRepository;

	public TeacherScheduleService(ClassRoomRepository classRoomRepository) {
		this.classRoomRepository = classRoomRepository;
	}

	public TeacherDailySchedule findDailySchedule(Long teacherId, LocalDate date) {
		if (teacherId == null || date == null) {
			return TeacherDailySchedule.empty();
		}
		String weekday = WEEKDAY_NAMES.get(date.getDayOfWeek());
		List<TimeRange> ranges = classRoomRepository
				.findByTeacherIdAndActiveTrueOrderByGradeAscIdAsc(teacherId)
				.stream()
				.flatMap(classRoom -> classRoom.getEffectiveSchedules().stream())
				.filter(schedule -> weekday.equals(schedule.getWeekday()))
				.filter(schedule -> !schedule.isWeeklyExam())
				.filter(this::hasValidTimeRange)
				.map(schedule -> new TimeRange(schedule.getStartTime(), schedule.getEndTime()))
				.sorted(Comparator.comparing(TimeRange::getStartTime)
						.thenComparing(TimeRange::getEndTime))
				.toList();
		return mergeTimeRanges(ranges);
	}

	public TeacherScheduleMatch findMatchedSchedule(Long teacherId, LocalDate date,
			LocalTime clockInTime, LocalTime clockOutTime) {
		if (teacherId == null || date == null || clockInTime == null || clockOutTime == null
				|| !clockOutTime.isAfter(clockInTime)) {
			return TeacherScheduleMatch.empty();
		}
		return matchSchedules(
				classRoomRepository.findByTeacherIdAndActiveTrueOrderByGradeAscIdAsc(teacherId),
				date, clockInTime, clockOutTime);
	}

	TeacherScheduleMatch matchSchedules(List<ClassRoom> classRooms, LocalDate date,
			LocalTime clockInTime, LocalTime clockOutTime) {
		if (date == null || clockInTime == null || clockOutTime == null
				|| !clockOutTime.isAfter(clockInTime)) {
			return TeacherScheduleMatch.empty();
		}
		String weekday = WEEKDAY_NAMES.get(date.getDayOfWeek());
		List<MatchedClassSchedule> matchedSchedules = classRooms.stream()
				.flatMap(classRoom -> classRoom.getEffectiveSchedules().stream()
						.filter(schedule -> weekday.equals(schedule.getWeekday()))
						.filter(schedule -> !schedule.isWeeklyExam())
						.filter(this::hasValidTimeRange)
						.filter(schedule -> overlaps(
								clockInTime, clockOutTime,
								schedule.getStartTime(), schedule.getEndTime()))
						.map(schedule -> new MatchedClassSchedule(classRoom, schedule)))
				.sorted(Comparator.comparing(
						matched -> matched.schedule().getStartTime()))
				.toList();
		if (matchedSchedules.isEmpty()) {
			return TeacherScheduleMatch.empty();
		}
		TeacherDailySchedule mergedSchedule = mergeTimeRanges(matchedSchedules.stream()
				.map(matched -> new TimeRange(
						matched.schedule().getStartTime(), matched.schedule().getEndTime()))
				.toList());
		String courseNames = matchedSchedules.stream()
				.map(matched -> matched.classRoom().getDisplayName())
				.distinct()
				.collect(java.util.stream.Collectors.joining("、"));
		return new TeacherScheduleMatch(
				matchedSchedules.getFirst().schedule().getId(),
				courseNames,
				mergedSchedule.getTimeRangeText(),
				mergedSchedule.getWorkMinutes(),
				mergedSchedule.getFirstStartTime());
	}

	TeacherDailySchedule mergeTimeRanges(List<TimeRange> ranges) {
		if (ranges.isEmpty()) {
			return TeacherDailySchedule.empty();
		}

		List<TimeRange> sortedRanges = ranges.stream()
				.sorted(Comparator.comparing(TimeRange::getStartTime)
						.thenComparing(TimeRange::getEndTime))
				.toList();
		List<TimeRange> merged = new ArrayList<>();
		LocalTime currentStart = sortedRanges.getFirst().getStartTime();
		LocalTime currentEnd = sortedRanges.getFirst().getEndTime();
		for (int index = 1; index < sortedRanges.size(); index++) {
			TimeRange next = sortedRanges.get(index);
			if (!next.getStartTime().isAfter(currentEnd)) {
				if (next.getEndTime().isAfter(currentEnd)) {
					currentEnd = next.getEndTime();
				}
				continue;
			}
			merged.add(new TimeRange(currentStart, currentEnd));
			currentStart = next.getStartTime();
			currentEnd = next.getEndTime();
		}
		merged.add(new TimeRange(currentStart, currentEnd));

		long workMinutes = merged.stream()
				.mapToLong(range -> Duration.between(range.getStartTime(), range.getEndTime()).toMinutes())
				.sum();
		return new TeacherDailySchedule(merged, workMinutes);
	}

	private boolean hasValidTimeRange(ClassSchedule schedule) {
		return schedule.getStartTime() != null
				&& schedule.getEndTime() != null
				&& schedule.getEndTime().isAfter(schedule.getStartTime());
	}

	private boolean overlaps(LocalTime attendanceStart, LocalTime attendanceEnd,
			LocalTime courseStart, LocalTime courseEnd) {
		return attendanceStart.isBefore(courseEnd) && attendanceEnd.isAfter(courseStart);
	}

	private record MatchedClassSchedule(ClassRoom classRoom, ClassSchedule schedule) {
	}
}
