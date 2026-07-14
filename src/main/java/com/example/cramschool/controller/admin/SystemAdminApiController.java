package com.example.cramschool.controller.admin;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.cramschool.dto.system.SystemDashboardDto;
import com.example.cramschool.entity.TeacherPermissionType;
import com.example.cramschool.service.CurrentUserService;
import com.example.cramschool.service.TeacherPermissionService;
import com.example.cramschool.service.system.SystemStatusService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/admin/system")
public class SystemAdminApiController {

	private final SystemStatusService systemStatusService;
	private final CurrentUserService currentUserService;
	private final TeacherPermissionService teacherPermissionService;

	public SystemAdminApiController(SystemStatusService systemStatusService,
			CurrentUserService currentUserService, TeacherPermissionService teacherPermissionService) {
		this.systemStatusService = systemStatusService;
		this.currentUserService = currentUserService;
		this.teacherPermissionService = teacherPermissionService;
	}

	@GetMapping("/dashboard")
	public SystemDashboardDto dashboard(HttpSession session) {
		Long teacherId = currentUserService.currentTeacherId(session);
		if (!teacherPermissionService.hasPermission(teacherId, TeacherPermissionType.SYSTEM_STATUS_VIEW)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "目前帳號沒有查看系統狀態的權限");
		}
		return systemStatusService.getDashboard();
	}
}
