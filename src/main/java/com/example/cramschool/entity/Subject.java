package com.example.cramschool.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "subjects")
public class Subject {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(name = "url_slug", length = 150, unique = true)
	private String urlSlug;

	@Column(length = 1000)
	private String description;

	@ManyToMany
	@JoinTable(name = "subject_teachers",
			joinColumns = @JoinColumn(name = "subject_id"),
			inverseJoinColumns = @JoinColumn(name = "teacher_id"))
	private Set<Teacher> teachers = new LinkedHashSet<>();

	@Column(name = "grade_levels", length = 200)
	private String gradeLevels;

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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUrlSlug() {
		return urlSlug;
	}

	public void setUrlSlug(String urlSlug) {
		this.urlSlug = urlSlug;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Set<Teacher> getTeachers() {
		return teachers;
	}

	public void setTeachers(Set<Teacher> teachers) {
		this.teachers = teachers;
	}

	public String getTeacherDisplay() {
		if (teachers == null || teachers.isEmpty()) {
			return "-";
		}
		return teachers.stream()
				.map(Teacher::getDisplayName)
				.toList()
				.stream()
				.collect(java.util.stream.Collectors.joining("、"));
	}

	public String getTeacherIdList() {
		if (teachers == null || teachers.isEmpty()) {
			return "";
		}
		return teachers.stream()
				.map(Teacher::getId)
				.map(String::valueOf)
				.collect(java.util.stream.Collectors.joining(","));
	}

	public List<String> getGradeLevelList() {
		if (gradeLevels == null || gradeLevels.isBlank()) {
			return new ArrayList<>();
		}
		return Arrays.stream(gradeLevels.split(","))
				.map(String::trim)
				.filter(value -> !value.isEmpty())
				.toList();
	}

	public String getGradeLevelDisplay() {
		List<String> values = getGradeLevelList();
		return values.isEmpty() ? "-" : String.join("、", values);
	}

	public boolean supportsGrade(String grade) {
		return getGradeLevelList().contains(grade);
	}

	public String getGradeLevels() {
		return gradeLevels;
	}

	public void setGradeLevels(String gradeLevels) {
		this.gradeLevels = gradeLevels;
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
