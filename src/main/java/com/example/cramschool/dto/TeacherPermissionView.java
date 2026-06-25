package com.example.cramschool.dto;

import java.util.Set;

import com.example.cramschool.entity.TeacherPermissionType;

public record TeacherPermissionView(Set<TeacherPermissionType> granted) {

	public boolean isGeneralSettings() {
		return has(TeacherPermissionType.GENERAL_SETTINGS);
	}

	public boolean isRegistrationCode() {
		return has(TeacherPermissionType.REGISTRATION_CODE);
	}

	public boolean isCreateTeacher() {
		return has(TeacherPermissionType.CREATE_TEACHER);
	}

	public boolean isManageTeacherPosition() {
		return has(TeacherPermissionType.MANAGE_TEACHER_POSITION);
	}

	public boolean isManageAllAttendance() {
		return has(TeacherPermissionType.MANAGE_ALL_ATTENDANCE);
	}

	public boolean isManageAllSalary() {
		return has(TeacherPermissionType.MANAGE_ALL_SALARY);
	}

	public boolean isManageTuition() {
		return has(TeacherPermissionType.MANAGE_TUITION);
	}

	public boolean isSystemUpdate() {
		return has(TeacherPermissionType.SYSTEM_UPDATE);
	}

	public boolean isDatabaseBackup() {
		return has(TeacherPermissionType.DATABASE_BACKUP);
	}

	public boolean isGradePromotion() {
		return has(TeacherPermissionType.GRADE_PROMOTION);
	}

	public boolean isStudentCreate() {
		return has(TeacherPermissionType.STUDENT_CREATE);
	}

	public boolean isClassCreate() {
		return has(TeacherPermissionType.CLASS_CREATE);
	}

	public boolean isClassUpdate() {
		return has(TeacherPermissionType.CLASS_UPDATE);
	}

	public boolean isStudentSensitiveView() {
		return has(TeacherPermissionType.STUDENT_SENSITIVE_VIEW);
	}

	public boolean isStudentUpdate() {
		return has(TeacherPermissionType.STUDENT_UPDATE);
	}

	public boolean isTeacherSensitiveView() {
		return has(TeacherPermissionType.TEACHER_SENSITIVE_VIEW);
	}

	public boolean isTeacherUpdate() {
		return has(TeacherPermissionType.TEACHER_UPDATE);
	}

	private boolean has(TeacherPermissionType permissionType) {
		return granted != null && granted.contains(permissionType);
	}
}
