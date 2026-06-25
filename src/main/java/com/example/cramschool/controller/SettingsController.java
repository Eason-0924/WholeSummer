package com.example.cramschool.controller;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.example.cramschool.service.SystemSettingService;
import com.example.cramschool.service.TeacherAccountService;
import com.example.cramschool.service.TeacherService;
import com.example.cramschool.entity.BackupStatus;
import com.example.cramschool.entity.BugReportStatus;
import com.example.cramschool.entity.BugReportType;
import com.example.cramschool.entity.TeacherPermissionType;
import com.example.cramschool.form.BugReportForm;
import com.example.cramschool.service.BackupService;
import com.example.cramschool.service.AppVersionService;
import com.example.cramschool.service.BugReportService;
import com.example.cramschool.service.UpdateCoordinator;
import com.example.cramschool.service.TeacherPermissionService;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/settings")
public class SettingsController {

	private final SystemSettingService systemSettingService;
	private final TeacherAccountService teacherAccountService;
	private final BackupService backupService;
	private final AppVersionService appVersionService;
	private final UpdateCoordinator updateCoordinator;
	private final BugReportService bugReportService;
	private final TeacherService teacherService;
	private final TeacherPermissionService teacherPermissionService;

	public SettingsController(SystemSettingService systemSettingService, TeacherAccountService teacherAccountService,
			BackupService backupService, AppVersionService appVersionService,
			UpdateCoordinator updateCoordinator, BugReportService bugReportService,
			TeacherService teacherService, TeacherPermissionService teacherPermissionService) {
		this.systemSettingService = systemSettingService;
		this.teacherAccountService = teacherAccountService;
		this.backupService = backupService;
		this.appVersionService = appVersionService;
		this.updateCoordinator = updateCoordinator;
		this.bugReportService = bugReportService;
		this.teacherService = teacherService;
		this.teacherPermissionService = teacherPermissionService;
	}

	@GetMapping
	public String index(HttpSession session, Model model) {
		systemSettingService.ensureDefaults();
		model.addAttribute("pageTitle", "系統設定");
		model.addAttribute("appVersion", appVersionService.currentVersion());
		Object accountId = session.getAttribute(AuthController.ACCOUNT_ID_SESSION_KEY);
		Long teacherId = currentTeacherId(session);
		if (teacherPermissionService.hasPermission(teacherId, TeacherPermissionType.SYSTEM_UPDATE)) {
			model.addAttribute("updateEnabled", updateCoordinator.isEnabled());
			model.addAttribute("updateChecking", updateCoordinator.isChecking());
			model.addAttribute("availableUpdate", updateCoordinator.getAvailableUpdate().orElse(null));
			model.addAttribute("updateError", updateCoordinator.getLastError());
		}
		if (accountId instanceof Long id && teacherAccountService.isDirector(id)) {
			model.addAttribute("permissionTeachers", teacherService.findActiveTeacherList());
			model.addAttribute("permissionTypes", TeacherPermissionType.values());
			Map<Long, Set<TeacherPermissionType>> permissionGrants = new LinkedHashMap<>();
			teacherService.findActiveTeacherList().forEach(teacher -> permissionGrants.put(
					teacher.getId(), teacherPermissionService.findGrantedPermissions(teacher.getId())));
			model.addAttribute("permissionGrants", permissionGrants);
		}
		model.addAttribute("systemNameValue", systemSettingService.getValue(SystemSettingService.SYSTEM_NAME,
				"霍爾夏天補習班 Whole Summer"));
		model.addAttribute("homeworkWarningDaysValue",
				systemSettingService.getInt(SystemSettingService.HOMEWORK_WARNING_DAYS, 3));
		model.addAttribute("backupReminderDaysValue",
				systemSettingService.getInt(SystemSettingService.BACKUP_REMINDER_DAYS, 7));
		if (!model.containsAttribute("bugReportForm")) {
			model.addAttribute("bugReportForm", new BugReportForm());
		}
		model.addAttribute("bugReportTypes", BugReportType.values());
		model.addAttribute("bugReports", bugReportService.findRecentByTeacherId(teacherId));
		model.addAttribute("bugReportMailConfigured", bugReportService.isMailConfigured());
		var backups = teacherPermissionService.hasPermission(teacherId, TeacherPermissionType.DATABASE_BACKUP)
				? backupService.listBackups()
				: java.util.List.<com.example.cramschool.entity.BackupRecord>of();
		model.addAttribute("backups", backups);
		model.addAttribute("latestBackup", backups.stream()
				.filter(backup -> backup.getStatus() == BackupStatus.SUCCESS)
				.findFirst()
				.orElse(null));
		return "settings/index";
	}

	@PostMapping("/general")
	public String updateGeneral(@RequestParam String systemName,
			@RequestParam int homeworkWarningDays,
			@RequestParam int backupReminderDays,
			HttpSession session,
			RedirectAttributes redirectAttributes) {
		if (!teacherPermissionService.hasPermission(currentTeacherId(session),
				TeacherPermissionType.GENERAL_SETTINGS)) {
			redirectAttributes.addFlashAttribute("errorMessage", "目前帳號沒有變更一般設定的權限");
			return "redirect:/settings";
		}
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
		if (!teacherPermissionService.hasPermission(currentTeacherId(session),
				TeacherPermissionType.REGISTRATION_CODE)) {
			redirectAttributes.addFlashAttribute("errorMessage", "目前帳號沒有變更教師註冊安全碼的權限");
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

	@PostMapping("/permissions/{teacherId}")
	public String updatePermissions(@PathVariable Long teacherId,
			@RequestParam(name = "permissions", required = false) Set<TeacherPermissionType> permissions,
			HttpSession session, RedirectAttributes redirectAttributes) {
		Long accountId = (Long) session.getAttribute(AuthController.ACCOUNT_ID_SESSION_KEY);
		if (!teacherAccountService.isDirector(accountId)) {
			redirectAttributes.addFlashAttribute("errorMessage", "權限不足，無法進入權限設定");
			return "redirect:/settings";
		}
		try {
			teacherPermissionService.replacePermissions(teacherId,
					permissions == null ? EnumSet.noneOf(TeacherPermissionType.class) : permissions);
			redirectAttributes.addFlashAttribute("message", "權限設定已更新");
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return "redirect:/settings#teacher-permissions";
	}

	@PostMapping("/bug-reports")
	public String createBugReport(@Valid @org.springframework.web.bind.annotation.ModelAttribute("bugReportForm")
			BugReportForm form, BindingResult bindingResult, HttpSession session,
			RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			String error = bindingResult.getAllErrors().getFirst().getDefaultMessage();
			redirectAttributes.addFlashAttribute("bugReportForm", form);
			redirectAttributes.addFlashAttribute("errorMessage", error);
			return "redirect:/settings#bug-report";
		}
		try {
			var report = bugReportService.create(currentTeacherId(session), form);
			if (report.getStatus() == BugReportStatus.SENT) {
				redirectAttributes.addFlashAttribute("message", "問題回報已寄送給開發者");
			} else {
				redirectAttributes.addFlashAttribute("errorMessage",
						"問題回報已保存在本機，但尚未寄出：" + report.getErrorMessage());
			}
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("bugReportForm", form);
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return "redirect:/settings#bug-report";
	}

	@PostMapping("/bug-reports/{id}/retry")
	public String retryBugReport(@PathVariable Long id, HttpSession session,
			RedirectAttributes redirectAttributes) {
		try {
			var report = bugReportService.retry(id, currentTeacherId(session));
			if (report.getStatus() == BugReportStatus.SENT) {
				redirectAttributes.addFlashAttribute("message", "問題回報已重新寄送成功");
			} else {
				redirectAttributes.addFlashAttribute("errorMessage",
						"仍無法寄送問題回報：" + report.getErrorMessage());
			}
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return "redirect:/settings#bug-report";
	}

	private Long currentTeacherId(HttpSession session) {
		Object teacherId = session.getAttribute(AuthController.TEACHER_ID_SESSION_KEY);
		if (teacherId instanceof Long id) {
			return id;
		}
		throw new IllegalArgumentException("找不到目前登入教師");
	}
}
