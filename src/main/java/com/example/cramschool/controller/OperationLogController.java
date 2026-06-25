package com.example.cramschool.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.cramschool.service.OperationLogService;
import com.example.cramschool.service.TeacherAccountService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/operation-logs")
public class OperationLogController {

	private final OperationLogService operationLogService;
	private final TeacherAccountService teacherAccountService;

	public OperationLogController(OperationLogService operationLogService,
			TeacherAccountService teacherAccountService) {
		this.operationLogService = operationLogService;
		this.teacherAccountService = teacherAccountService;
	}

	@GetMapping
	public String list(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
		Object accountId = session.getAttribute(AuthController.ACCOUNT_ID_SESSION_KEY);
		if (!(accountId instanceof Long id) || !teacherAccountService.isDirector(id)) {
			redirectAttributes.addFlashAttribute("errorMessage", "只有主任可以查看操作紀錄");
			return "redirect:/settings";
		}
		model.addAttribute("pageTitle", "操作紀錄");
		model.addAttribute("operationLogs", operationLogService.findRecent());
		return "operation-logs/list";
	}
}
