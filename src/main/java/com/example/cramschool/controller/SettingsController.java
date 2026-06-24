package com.example.cramschool.controller;

import com.example.cramschool.service.SystemSettingService;
import com.example.cramschool.service.TeacherAccountService;
import com.example.cramschool.entity.BackupStatus;
import com.example.cramschool.service.BackupService;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/settings")
public class SettingsController {

	private final SystemSettingService systemSettingService;
	private final TeacherAccountService teacherAccountService;
	private final BackupService backupService;

	public SettingsController(SystemSettingService systemSettingService, TeacherAccountService teacherAccountService,
			BackupService backupService) {
		this.systemSettingService = systemSettingService;
		this.teacherAccountService = teacherAccountService;
		this.backupService = backupService;
	}

	@GetMapping
	public String index(Model model) {
		systemSettingService.ensureDefaults();
		model.addAttribute("pageTitle", "系統設定");
		model.addAttribute("themeModeValue", systemSettingService.getValue(SystemSettingService.THEME_MODE, "light"));
		model.addAttribute("systemNameValue", systemSettingService.getValue(SystemSettingService.SYSTEM_NAME,
				"霍爾夏天補習班 Whole Summer"));
		model.addAttribute("homeworkWarningDaysValue",
				systemSettingService.getInt(SystemSettingService.HOMEWORK_WARNING_DAYS, 3));
		model.addAttribute("backupReminderDaysValue",
				systemSettingService.getInt(SystemSettingService.BACKUP_REMINDER_DAYS, 7));
		var backups = backupService.listBackups();
		model.addAttribute("backups", backups);
		model.addAttribute("latestBackup", backups.stream()
				.filter(backup -> backup.getStatus() == BackupStatus.SUCCESS)
				.findFirst()
				.orElse(null));
		return "settings/index";
	}

	@PostMapping("/general")
	public String updateGeneral(@RequestParam String themeMode,
			@RequestParam String systemName,
			@RequestParam int homeworkWarningDays,
			@RequestParam int backupReminderDays,
			RedirectAttributes redirectAttributes) {
		systemSettingService.setValue(SystemSettingService.THEME_MODE, "dark".equals(themeMode) ? "dark" : "light");
		systemSettingService.setValue(SystemSettingService.SYSTEM_NAME,
				systemName == null || systemName.isBlank() ? "霍爾夏天補習班 Whole Summer" : systemName.trim());
		systemSettingService.setValue(SystemSettingService.HOMEWORK_WARNING_DAYS,
				String.valueOf(Math.max(0, homeworkWarningDays)));
		systemSettingService.setValue(SystemSettingService.BACKUP_REMINDER_DAYS,
				String.valueOf(Math.max(0, backupReminderDays)));
		redirectAttributes.addFlashAttribute("message", "已更新系統設定");
		return "redirect:/settings";
	}

	@PostMapping("/password")
	public String updatePassword(@RequestParam String currentPassword,
			@RequestParam String newPassword,
			@RequestParam String confirmPassword,
			HttpSession session,
			RedirectAttributes redirectAttributes) {
		try {
			Long accountId = (Long) session.getAttribute(AuthController.ACCOUNT_ID_SESSION_KEY);
			teacherAccountService.changePassword(accountId, currentPassword, newPassword, confirmPassword);
			redirectAttributes.addFlashAttribute("message", "已更新個人登入密碼");
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return "redirect:/settings";
	}

	@PostMapping("/registration-code")
	public String updateRegistrationCode(@RequestParam String currentRegistrationCode,
			@RequestParam String newRegistrationCode,
			@RequestParam String confirmRegistrationCode,
			HttpSession session,
			RedirectAttributes redirectAttributes) {
		Long accountId = (Long) session.getAttribute(AuthController.ACCOUNT_ID_SESSION_KEY);
		if (!teacherAccountService.isDirector(accountId)) {
			redirectAttributes.addFlashAttribute("errorMessage", "只有主任可以變更教師註冊安全碼");
			return "redirect:/settings";
		}
		try {
			systemSettingService.changeRegistrationCode(
					currentRegistrationCode, newRegistrationCode, confirmRegistrationCode);
			redirectAttributes.addFlashAttribute("message", "已更新教師註冊安全碼");
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return "redirect:/settings";
	}
}
