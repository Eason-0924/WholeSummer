package com.example.cramschool.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.cramschool.config.LineProperties;

@Controller
@RequestMapping("/liff")
public class LiffPageController {

	private final LineProperties lineProperties;

	public LiffPageController(LineProperties lineProperties) {
		this.lineProperties = lineProperties;
	}

	@GetMapping("/leave")
	public String leave(Model model) {
		model.addAttribute("liffId", lineProperties.getLiffId());
		model.addAttribute("pageTitle", "學生請假");
		return "liff/leave";
	}
}
