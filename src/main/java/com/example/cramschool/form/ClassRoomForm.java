package com.example.cramschool.form;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.time.LocalTime;

import org.springframework.format.annotation.DateTimeFormat;

import com.example.cramschool.entity.ClassSchedule;
import com.example.cramschool.entity.ClassRoom;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ClassRoomForm {

	@NotBlank(message = "請選擇年級")
	@Size(max = 50, message = "年級不可超過 50 個字")
	private String grade;

	@NotNull(message = "請選擇科目")
	private Long subjectId;

	@Size(max = 100, message = "班別不可超過 100 個字")
	private String classType;

	private Long teacherId;

	@Size(max = 20, message = "星期不可超過 20 個字")
	private String weekday;

	@DateTimeFormat(pattern = "HH:mm")
	private LocalTime startTime;

	@DateTimeFormat(pattern = "HH:mm")
	private LocalTime endTime;

	private List<ScheduleEntryForm> scheduleEntries = new ArrayList<>();

	@Size(max = 1000, message = "說明不可超過 1000 個字")
	private String description;

	@AssertTrue(message = "請完整填寫上課時間")
	public boolean isScheduleComplete() {
		return scheduleEntries.stream().allMatch(ScheduleEntryForm::isCompleteOrEmpty);
	}

	@AssertTrue(message = "結束時間必須晚於開始時間")
	public boolean isEndTimeAfterStartTime() {
		return scheduleEntries.stream().allMatch(ScheduleEntryForm::isEndTimeAfterStartTime);
	}

	public static ClassRoomForm newForm() {
		ClassRoomForm form = new ClassRoomForm();
		form.getScheduleEntries().add(ScheduleEntryForm.defaultEntry());
		return form;
	}

	public static ClassRoomForm from(ClassRoom classRoom) {
		ClassRoomForm form = new ClassRoomForm();
		form.setGrade(classRoom.getGrade());
		if (classRoom.getSubject() != null) {
			form.setSubjectId(classRoom.getSubject().getId());
		}
		form.setClassType(classRoom.getClassType());
		if (classRoom.getTeacher() != null) {
			form.setTeacherId(classRoom.getTeacher().getId());
		}
		form.setScheduleEntries(classRoom.getEffectiveSchedules().stream()
				.map(ScheduleEntryForm::from)
				.toList());
		if (form.getScheduleEntries().isEmpty()) {
			form.getScheduleEntries().add(ScheduleEntryForm.defaultEntry());
		}
		form.setDescription(classRoom.getDescription());
		return form;
	}

	public void applyTo(ClassRoom classRoom) {
		classRoom.setGrade(grade);
		classRoom.setClassType(classType);
		classRoom.setDescription(description);
	}

	public List<ClassSchedule> toSchedules() {
		return scheduleEntries.stream()
				.filter(ScheduleEntryForm::isComplete)
				.sorted(Comparator.comparingInt((ScheduleEntryForm entry) -> weekdayOrder(entry.getWeekday()))
						.thenComparing(ScheduleEntryForm::getStartTime, Comparator.nullsLast(LocalTime::compareTo)))
				.map(entry -> new ClassSchedule(entry.getWeekday(), entry.getStartTime(), entry.getEndTime(),
						entry.isWeeklyExam()))
				.toList();
	}

	public String getGrade() {
		return grade;
	}

	public void setGrade(String grade) {
		this.grade = grade;
	}

	public Long getSubjectId() {
		return subjectId;
	}

	public void setSubjectId(Long subjectId) {
		this.subjectId = subjectId;
	}

	public String getClassType() {
		return classType;
	}

	public void setClassType(String classType) {
		this.classType = classType;
	}

	public Long getTeacherId() {
		return teacherId;
	}

	public void setTeacherId(Long teacherId) {
		this.teacherId = teacherId;
	}

	public String getWeekday() {
		return weekday;
	}

	public void setWeekday(String weekday) {
		this.weekday = weekday;
	}

	public LocalTime getStartTime() {
		return startTime;
	}

	public void setStartTime(LocalTime startTime) {
		this.startTime = startTime;
	}

	public LocalTime getEndTime() {
		return endTime;
	}

	public void setEndTime(LocalTime endTime) {
		this.endTime = endTime;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	private boolean hasText(String value) {
		return value != null && !value.trim().isEmpty();
	}

	public List<ScheduleEntryForm> getScheduleEntries() {
		return scheduleEntries;
	}

	public void setScheduleEntries(List<ScheduleEntryForm> scheduleEntries) {
		this.scheduleEntries = scheduleEntries == null ? new ArrayList<>() : new ArrayList<>(scheduleEntries);
	}

	private static int weekdayOrder(String weekday) {
		List<String> weekdays = List.of("星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日");
		int index = weekdays.indexOf(weekday);
		return index >= 0 ? index : weekdays.size();
	}

	public static class ScheduleEntryForm {

		@Size(max = 20, message = "星期不可超過 20 個字")
		private String weekday;

		@DateTimeFormat(pattern = "HH:mm")
		private LocalTime startTime;

		@DateTimeFormat(pattern = "HH:mm")
		private LocalTime endTime;

		private boolean weeklyExam;

		public static ScheduleEntryForm defaultEntry() {
			ScheduleEntryForm entry = new ScheduleEntryForm();
			entry.setStartTime(LocalTime.MIDNIGHT);
			entry.setEndTime(LocalTime.MIDNIGHT);
			return entry;
		}

		public static ScheduleEntryForm from(ClassSchedule schedule) {
			ScheduleEntryForm entry = new ScheduleEntryForm();
			entry.setWeekday(schedule.getWeekday());
			entry.setStartTime(schedule.getStartTime());
			entry.setEndTime(schedule.getEndTime());
			entry.setWeeklyExam(schedule.isWeeklyExam());
			return entry;
		}

		public boolean isCompleteOrEmpty() {
			return isEmpty() || isComplete();
		}

		public boolean isEndTimeAfterStartTime() {
			if (isEmpty() || startTime == null || endTime == null) {
				return true;
			}
			return endTime.isAfter(startTime);
		}

		public boolean isComplete() {
			return hasText(weekday) && startTime != null && endTime != null;
		}

		private boolean isEmpty() {
			return !hasText(weekday) && !hasMeaningfulTime(startTime) && !hasMeaningfulTime(endTime);
		}

		private boolean hasText(String value) {
			return value != null && !value.trim().isEmpty();
		}

		private boolean hasMeaningfulTime(LocalTime value) {
			return value != null && !LocalTime.MIDNIGHT.equals(value);
		}

		public String getWeekday() {
			return weekday;
		}

		public void setWeekday(String weekday) {
			this.weekday = weekday;
		}

		public LocalTime getStartTime() {
			return startTime;
		}

		public void setStartTime(LocalTime startTime) {
			this.startTime = startTime;
		}

		public LocalTime getEndTime() {
			return endTime;
		}

		public void setEndTime(LocalTime endTime) {
			this.endTime = endTime;
		}

		public boolean isWeeklyExam() {
			return weeklyExam;
		}

		public void setWeeklyExam(boolean weeklyExam) {
			this.weeklyExam = weeklyExam;
		}
	}
}
