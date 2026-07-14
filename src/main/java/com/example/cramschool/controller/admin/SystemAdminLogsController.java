package com.example.cramschool.controller.admin;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import com.example.cramschool.entity.TeacherPermissionType;
import com.example.cramschool.service.CurrentUserService;
import com.example.cramschool.service.TeacherPermissionService;
import com.example.cramschool.service.system.SystemLogService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin/system/logs")
public class SystemAdminLogsController {

	private final SystemLogService systemLogService;
	private final CurrentUserService currentUserService;
	private final TeacherPermissionService teacherPermissionService;

	public SystemAdminLogsController(SystemLogService systemLogService, CurrentUserService currentUserService,
			TeacherPermissionService teacherPermissionService) {
		this.systemLogService = systemLogService;
		this.currentUserService = currentUserService;
		this.teacherPermissionService = teacherPermissionService;
	}

	@GetMapping
	public String logs(HttpSession session, Model model,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "30") int size) {
		requireAccess(session);
		model.addAttribute("pageTitle", "系統操作紀錄");
		model.addAttribute("logPage", systemLogService.findPage(page, size));
		return "admin/system/logs";
	}

	private void requireAccess(HttpSession session) {
		if (!teacherPermissionService.hasPermission(currentUserService.currentTeacherId(session),
				TeacherPermissionType.SYSTEM_STATUS_VIEW)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "目前帳號沒有查看操作紀錄的權限");
		}
	}
}
