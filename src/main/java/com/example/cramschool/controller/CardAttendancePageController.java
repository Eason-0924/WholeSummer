package com.example.cramschool.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CardAttendancePageController {

	@GetMapping("/attendance/card-check-in")
	public String cardCheckInPage(Model model) {
		model.addAttribute("pageTitle", "刷卡點名");
		return "attendance/card-check-in";
	}
}
