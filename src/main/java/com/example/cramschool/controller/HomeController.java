package com.example.cramschool.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.cramschool.service.HomeworkService;
import com.example.cramschool.service.SystemSettingService;

@Controller
public class HomeController {

	private final HomeworkService homeworkService;
	private final SystemSettingService systemSettingService;

	public HomeController(HomeworkService homeworkService, SystemSettingService systemSettingService) {
		this.homeworkService = homeworkService;
		this.systemSettingService = systemSettingService;
	}

	@GetMapping("/")
	public String index(Model model) {
		model.addAttribute("pageTitle", "WholeSummer 補習班管理系統");
		model.addAttribute("upcomingHomeworkSummaries", homeworkService
				.findUpcomingNotSubmittedSummaries(systemSettingService.getInt(SystemSettingService.HOMEWORK_WARNING_DAYS, 3)));
		return "index";
	}
}
