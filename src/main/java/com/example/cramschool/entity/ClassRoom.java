package com.example.cramschool.entity;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "classes")
public class ClassRoom {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(length = 50)
	private String grade;

	@ManyToOne
	@JoinColumn(name = "subject_id")
	private Subject subject;

	@Column(name = "class_type", length = 100)
	private String classType;

	@Column(name = "url_slug", length = 150, unique = true)
	private String urlSlug;

	@ManyToOne
	@JoinColumn(name = "teacher_id")
	private Teacher teacher;

	@OneToMany(mappedBy = "classRoom", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ClassSchedule> schedules = new ArrayList<>();

	@Column(length = 1000)
	private String description;

	@Column(nullable = false)
	private boolean active = true;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	void prePersist() {
		LocalDateTime now = LocalDateTime.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	void preUpdate() {
		this.updatedAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getDisplayName() {
		String baseName = safeText(grade) + safeText(getSubjectName());
		if (hasText(classType)) {
			return baseName + "（" + classType.trim() + "）";
		}
		if (hasText(baseName)) {
			return baseName;
		}
		return "未命名班級";
	}

	private String safeText(String value) {
		return value == null ? "" : value.trim();
	}

	private boolean hasText(String value) {
		return value != null && !value.trim().isEmpty();
	}

	public String getGrade() {
		return grade;
	}

	public void setGrade(String grade) {
		this.grade = grade;
	}

	public Subject getSubject() {
		return subject;
	}

	public void setSubject(Subject subject) {
		this.subject = subject;
	}

	public String getSubjectName() {
		return subject == null ? "" : subject.getName();
	}

	public String getClassType() {
		return classType;
	}

	public void setClassType(String classType) {
		this.classType = classType;
	}

	public String getUrlSlug() {
		return urlSlug;
	}

	public void setUrlSlug(String urlSlug) {
		this.urlSlug = urlSlug;
	}

	public Teacher getTeacher() {
		return teacher;
	}

	public void setTeacher(Teacher teacher) {
		this.teacher = teacher;
	}

	public String getTeacherName() {
		return teacher == null ? "" : teacher.getDisplayName();
	}

	public String getScheduleText() {
		List<ClassSchedule> schedules = getEffectiveSchedules();
		if (schedules.isEmpty()) {
			return "-";
		}
		return schedules.stream()
				.map(ClassSchedule::getDisplayText)
				.toList()
				.stream()
				.collect(java.util.stream.Collectors.joining("、"));
	}

	public String getScheduleWeekdayData() {
		return getEffectiveSchedules().stream()
				.map(ClassSchedule::getWeekday)
				.distinct()
				.collect(java.util.stream.Collectors.joining(","));
	}

	public String getTimeRangeText() {
		ClassSchedule firstSchedule = getFirstEffectiveSchedule();
		if (firstSchedule == null) {
			return "-";
		}
		return firstSchedule.getTimeRangeText();
	}

	public List<ClassSchedule> getEffectiveSchedules() {
		if (schedules != null && !schedules.isEmpty()) {
			return schedules.stream()
					.filter(schedule -> schedule.getScheduleType() == ScheduleType.NORMAL
							&& schedule.getScheduledStartAt() == null)
					.sorted(Comparator.comparingInt((ClassSchedule schedule) -> weekdayOrder(schedule.getWeekday()))
							.thenComparing(ClassSchedule::getStartTime, Comparator.nullsLast(LocalTime::compareTo)))
					.toList();
		}
		return List.of();
	}

	private ClassSchedule getFirstEffectiveSchedule() {
		List<ClassSchedule> schedules = getEffectiveSchedules();
		return schedules.isEmpty() ? null : schedules.getFirst();
	}

	private int weekdayOrder(String weekday) {
		List<String> weekdays = List.of("星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日");
		int index = weekdays.indexOf(weekday);
		return index >= 0 ? index : weekdays.size();
	}

	public List<ClassSchedule> getSchedules() {
		return schedules;
	}

	public void setSchedules(List<ClassSchedule> schedules) {
		this.schedules.clear();
		if (schedules != null) {
			schedules.forEach(this::addSchedule);
		}
	}

	public void addSchedule(ClassSchedule schedule) {
		schedule.setClassRoom(this);
		this.schedules.add(schedule);
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
}
