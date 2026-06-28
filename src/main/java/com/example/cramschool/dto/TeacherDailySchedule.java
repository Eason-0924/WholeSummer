package com.example.cramschool.dto;

import java.time.LocalTime;
import java.util.List;

public class TeacherDailySchedule {

	private final List<TimeRange> timeRanges;
	private final long workMinutes;

	public TeacherDailySchedule(List<TimeRange> timeRanges, long workMinutes) {
		this.timeRanges = List.copyOf(timeRanges);
		this.workMinutes = workMinutes;
	}

	public static TeacherDailySchedule empty() {
		return new TeacherDailySchedule(List.of(), 0);
	}

	public List<TimeRange> getTimeRanges() {
		return timeRanges;
	}

	public long getWorkMinutes() {
		return workMinutes;
	}

	public LocalTime getFirstStartTime() {
		return timeRanges.isEmpty() ? null : timeRanges.getFirst().getStartTime();
	}

	public LocalTime getLastEndTime() {
		return timeRanges.isEmpty() ? null : timeRanges.getLast().getEndTime();
	}

	public String getTimeRangeText() {
		if (timeRanges.isEmpty()) {
			return "當日無課程";
		}
		return timeRanges.stream()
				.map(TimeRange::getDisplayText)
				.collect(java.util.stream.Collectors.joining("、"));
	}

	public static class TimeRange {

		private final LocalTime startTime;
		private final LocalTime endTime;

		public TimeRange(LocalTime startTime, LocalTime endTime) {
			this.startTime = startTime;
			this.endTime = endTime;
		}

		public LocalTime getStartTime() {
			return startTime;
		}

		public LocalTime getEndTime() {
			return endTime;
		}

		public String getDisplayText() {
			return startTime + " ~ " + endTime;
		}
	}
}
