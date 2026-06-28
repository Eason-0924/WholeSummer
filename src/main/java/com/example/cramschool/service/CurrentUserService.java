package com.example.cramschool.service;

import org.springframework.stereotype.Service;

import com.example.cramschool.controller.AuthController;
import com.example.cramschool.entity.TeacherPermissionType;

import jakarta.servlet.http.HttpSession;

@Service
public class CurrentUserService {

	private final TeacherPermissionService teacherPermissionService;

	public CurrentUserService(TeacherPermissionService teacherPermissionService) {
		this.teacherPermissionService = teacherPermissionService;
	}

	public Long currentTeacherId(HttpSession session) {
		Object teacherId = session.getAttribute(AuthController.TEACHER_ID_SESSION_KEY);
		if (teacherId instanceof Long id) {
			return id;
		}
		throw new IllegalArgumentException("找不到目前登入教師");
	}

	public String currentTeacherName(HttpSession session) {
		Object teacherName = session.getAttribute(AuthController.TEACHER_NAME_SESSION_KEY);
		return teacherName == null ? "" : teacherName.toString();
	}

	public boolean isDirector(HttpSession session) {
		return teacherPermissionService.hasPermission(
				currentTeacherId(session), TeacherPermissionType.MANAGE_ALL_ATTENDANCE);
	}
}
