package com.example.cramschool.controller.admin;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import com.example.cramschool.entity.TeacherPermissionType;
import com.example.cramschool.service.CurrentUserService;
import com.example.cramschool.service.TeacherPermissionService;
import com.example.cramschool.service.system.OnlineUserService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin/system/users")
public class SystemAdminUsersController {

	private final OnlineUserService onlineUserService;
	private final CurrentUserService currentUserService;
	private final TeacherPermissionService teacherPermissionService;

	public SystemAdminUsersController(OnlineUserService onlineUserService, CurrentUserService currentUserService,
			TeacherPermissionService teacherPermissionService) {
		this.onlineUserService = onlineUserService;
		this.currentUserService = currentUserService;
		this.teacherPermissionService = teacherPermissionService;
	}

	@GetMapping
	public String users(HttpSession session, Model model) {
		requireAccess(session);
		model.addAttribute("pageTitle", "上線使用者");
		model.addAttribute("onlineUsers", onlineUserService.findUsers());
		return "admin/system/users";
	}

	private void requireAccess(HttpSession session) {
		if (!teacherPermissionService.hasPermission(currentUserService.currentTeacherId(session),
				TeacherPermissionType.SYSTEM_STATUS_VIEW)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "目前帳號沒有查看上線使用者的權限");
		}
	}
}
