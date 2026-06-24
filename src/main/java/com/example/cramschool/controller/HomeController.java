package com.example.cramschool.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.cramschool.service.HomeworkService;
import com.example.cramschool.service.SystemSettingService;
import com.example.cramschool.service.TeacherAccountService;
import com.example.cramschool.service.UpdateCoordinator;

import jakarta.servlet.http.HttpSession;

@Controller
public class HomeController {

	private final HomeworkService homeworkService;
	private final SystemSettingService systemSettingService;
	private final TeacherAccountService teacherAccountService;
	private final UpdateCoordinator updateCoordinator;

	public HomeController(HomeworkService homeworkService, SystemSettingService systemSettingService,
			TeacherAccountService teacherAccountService, UpdateCoordinator updateCoordinator) {
		this.homeworkService = homeworkService;
		this.systemSettingService = systemSettingService;
		this.teacherAccountService = teacherAccountService;
		this.updateCoordinator = updateCoordinator;
	}

	@GetMapping("/")
	public String index(HttpSession session, Model model) {
		model.addAttribute("pageTitle", "WholeSummer 補習班管理系統");
		model.addAttribute("upcomingHomeworkSummaries", homeworkService
				.findUpcomingNotSubmittedSummaries(systemSettingService.getInt(SystemSettingService.HOMEWORK_WARNING_DAYS, 3)));
		Object accountId = session.getAttribute(AuthController.ACCOUNT_ID_SESSION_KEY);
		if (accountId instanceof Long id && teacherAccountService.isDirector(id)) {
			model.addAttribute("availableUpdate", updateCoordinator.getAvailableUpdate().orElse(null));
			model.addAttribute("updateChecking", updateCoordinator.isChecking());
		}
		return "index";
	}
}
