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
				.filter(this::hasValidTimeRange)
				.map(schedule -> new TimeRange(schedule.getStartTime(), schedule.getEndTime()))
				.sorted(Comparator.comparing(TimeRange::getStartTime)
						.thenComparing(TimeRange::getEndTime))
				.toList();
		return mergeTimeRanges(ranges);
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
}
