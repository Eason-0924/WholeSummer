package com.example.cramschool.config;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.example.cramschool.controller.AuthController;
import com.example.cramschool.service.SystemSettingService;
import com.example.cramschool.service.TeacherAccountService;

import jakarta.servlet.http.HttpSession;

@ControllerAdvice
public class GlobalModelAttributes {

	private final SystemSettingService systemSettingService;
	private final TeacherAccountService teacherAccountService;

	public GlobalModelAttributes(SystemSettingService systemSettingService,
			TeacherAccountService teacherAccountService) {
		this.systemSettingService = systemSettingService;
		this.teacherAccountService = teacherAccountService;
	}

	@ModelAttribute("systemName")
	public String systemName() {
		return systemSettingService.getValue(SystemSettingService.SYSTEM_NAME, "霍爾夏天補習班 Whole Summer");
	}

	@ModelAttribute("themeMode")
	public String themeMode() {
		return systemSettingService.getValue(SystemSettingService.THEME_MODE, "light");
	}

	@ModelAttribute("homeworkWarningDays")
	public int homeworkWarningDays() {
		return systemSettingService.getInt(SystemSettingService.HOMEWORK_WARNING_DAYS, 3);
	}

	@ModelAttribute("backupReminderDays")
	public int backupReminderDays() {
		return systemSettingService.getInt(SystemSettingService.BACKUP_REMINDER_DAYS, 7);
	}

	@ModelAttribute("currentTeacherName")
	public String currentTeacherName(HttpSession session) {
		Object name = session.getAttribute(AuthController.TEACHER_NAME_SESSION_KEY);
		return name == null ? "" : name.toString();
	}

	@ModelAttribute("currentTeacherIsDirector")
	public boolean currentTeacherIsDirector(HttpSession session) {
		Object accountId = session.getAttribute(AuthController.ACCOUNT_ID_SESSION_KEY);
		return accountId instanceof Long id && teacherAccountService.isDirector(id);
	}

	@ModelAttribute("currentTeacherId")
	public Long currentTeacherId(HttpSession session) {
		Object teacherId = session.getAttribute(AuthController.TEACHER_ID_SESSION_KEY);
		return teacherId instanceof Long id ? id : null;
	}
}
