package com.example.cramschool.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.cramschool.dto.HomeNotification;
import com.example.cramschool.service.HomeNotificationService;
import com.example.cramschool.service.SystemSettingService;
import com.example.cramschool.service.TeacherAccountService;
import com.example.cramschool.service.UpdateCoordinator;

import jakarta.servlet.http.HttpSession;

@Controller
public class HomeController {

	private final SystemSettingService systemSettingService;
	private final TeacherAccountService teacherAccountService;
	private final UpdateCoordinator updateCoordinator;
	private final HomeNotificationService homeNotificationService;

	public HomeController(SystemSettingService systemSettingService,
			TeacherAccountService teacherAccountService, UpdateCoordinator updateCoordinator,
			HomeNotificationService homeNotificationService) {
		this.systemSettingService = systemSettingService;
		this.teacherAccountService = teacherAccountService;
		this.updateCoordinator = updateCoordinator;
		this.homeNotificationService = homeNotificationService;
	}

	@GetMapping("/")
	public String index(HttpSession session, Model model) {
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
		model.addAttribute("notifications", notifications);
		return "index";
	}
}
