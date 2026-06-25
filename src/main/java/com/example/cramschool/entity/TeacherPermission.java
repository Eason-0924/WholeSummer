package com.example.cramschool.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "teacher_permissions", uniqueConstraints = {
		@UniqueConstraint(name = "uk_teacher_permission", columnNames = { "teacher_id", "permission_type" })
})
public class TeacherPermission {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "teacher_id", nullable = false)
	private Teacher teacher;

	@Enumerated(EnumType.STRING)
	@jakarta.persistence.Column(name = "permission_type", nullable = false, length = 50)
	private TeacherPermissionType permissionType;

	@jakarta.persistence.Column(nullable = false)
	@ColumnDefault("true")
	private boolean enabled = true;

	public Long getId() {
		return id;
	}

	public Teacher getTeacher() {
		return teacher;
	}

	public void setTeacher(Teacher teacher) {
		this.teacher = teacher;
	}

	public TeacherPermissionType getPermissionType() {
		return permissionType;
	}

	public void setPermissionType(TeacherPermissionType permissionType) {
		this.permissionType = permissionType;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}
