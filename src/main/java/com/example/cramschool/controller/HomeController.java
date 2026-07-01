package com.example.cramschool.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.cramschool.dto.TeacherPermissionView;
import com.example.cramschool.dto.HomeNotification;
import com.example.cramschool.service.HomeShortcutService;
import com.example.cramschool.service.HomeNotificationService;
import com.example.cramschool.service.SystemSettingService;
import com.example.cramschool.service.TeacherAccountService;
import com.example.cramschool.service.TodayWorkbenchService;
import com.example.cramschool.service.UpdateCoordinator;

import jakarta.servlet.http.HttpSession;

@Controller
public class HomeController {

	private final SystemSettingService systemSettingService;
	private final TeacherAccountService teacherAccountService;
	private final UpdateCoordinator updateCoordinator;
	private final HomeNotificationService homeNotificationService;
	private final HomeShortcutService homeShortcutService;
	private final TodayWorkbenchService todayWorkbenchService;

	public HomeController(SystemSettingService systemSettingService,
			TeacherAccountService teacherAccountService, UpdateCoordinator updateCoordinator,
			HomeNotificationService homeNotificationService, HomeShortcutService homeShortcutService,
			TodayWorkbenchService todayWorkbenchService) {
		this.systemSettingService = systemSettingService;
		this.teacherAccountService = teacherAccountService;
		this.updateCoordinator = updateCoordinator;
		this.homeNotificationService = homeNotificationService;
		this.homeShortcutService = homeShortcutService;
		this.todayWorkbenchService = todayWorkbenchService;
	}

	@GetMapping("/")
	public String index(HttpSession session,
			@org.springframework.web.bind.annotation.ModelAttribute("teacherPermissions")
			TeacherPermissionView teacherPermissions,
			Model model) {
		model.addAttribute("pageTitle", "WholeSummer 補習班管理系統");
		int warningDays = systemSettingService.getInt(SystemSettingService.HOMEWORK_WARNING_DAYS, 3);
		Long teacherId = (Long) session.getAttribute(AuthController.TEACHER_ID_SESSION_KEY);
		java.util.List<HomeNotification> notifications = new java.util.ArrayList<>(
				homeNotificationService.buildNotifications(teacherId, warningDays));
		Object accountId = session.getAttribute(AuthController.ACCOUNT_ID_SESSION_KEY);
		boolean director = accountId instanceof Long id && teacherAccountService.isDirector(id);
		model.addAttribute("homeDirectorView", director);
		if (director) {
			updateCoordinator.getAvailableUpdate().ifPresent(update -> notifications.add(new HomeNotification(
					"update", "系統更新", "WholeSummer " + update.latestVersion() + " 可更新",
					"目前版本：" + update.currentVersion(),
					update.assetName(), "/settings#system-update", java.time.LocalDate.now())));
			model.addAttribute("availableUpdate", updateCoordinator.getAvailableUpdate().orElse(null));
			model.addAttribute("updateChecking", updateCoordinator.isChecking());
		}
		model.addAttribute("homeShortcutCards",
				homeShortcutService.selectedShortcuts(teacherId, teacherPermissions, director));
		model.addAttribute("homeShortcutAvailableCards",
				homeShortcutService.availableShortcuts(teacherId, teacherPermissions, director));
		model.addAttribute("homeShortcutShowDescription",
				homeShortcutService.showDescriptions(teacherId));
		model.addAttribute("todayWorkbench", todayWorkbenchService.build(teacherId, director));
		model.addAttribute("notifications", notifications);
		return "index";
	}

	@PostMapping("/home/shortcuts")
	public String updateShortcuts(HttpSession session,
			@org.springframework.web.bind.annotation.ModelAttribute("teacherPermissions")
			TeacherPermissionView teacherPermissions,
			@RequestParam(name = "shortcutIds", required = false) java.util.List<String> shortcutIds,
			@RequestParam(name = "showDescription", defaultValue = "false") boolean showDescription,
			RedirectAttributes redirectAttributes) {
		Long teacherId = (Long) session.getAttribute(AuthController.TEACHER_ID_SESSION_KEY);
		Object accountId = session.getAttribute(AuthController.ACCOUNT_ID_SESSION_KEY);
		boolean director = accountId instanceof Long id && teacherAccountService.isDirector(id);
		try {
			homeShortcutService.saveShortcuts(teacherId, shortcutIds, showDescription, teacherPermissions, director);
			redirectAttributes.addFlashAttribute("message", "已儲存首頁快捷欄設定");
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return "redirect:/";
	}
}
