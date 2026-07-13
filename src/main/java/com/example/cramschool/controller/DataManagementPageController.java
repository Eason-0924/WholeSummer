package com.example.cramschool.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DataManagementPageController {
	@GetMapping("/data-management")
	public String page() { return "data-management"; }
}
