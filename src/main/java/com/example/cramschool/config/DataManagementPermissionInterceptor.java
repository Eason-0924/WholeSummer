package com.example.cramschool.config;

import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.example.cramschool.controller.AuthController;
import com.example.cramschool.entity.TeacherPermissionType;
import com.example.cramschool.service.TeacherPermissionService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class DataManagementPermissionInterceptor implements HandlerInterceptor {
	private final TeacherPermissionService permissions;

	public DataManagementPermissionInterceptor(TeacherPermissionService permissions) {
		this.permissions = permissions;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		Object value = request.getSession(false) == null ? null
				: request.getSession(false).getAttribute(AuthController.TEACHER_ID_SESSION_KEY);
		if (value instanceof Long id && permissions.hasPermission(id, TeacherPermissionType.DATA_VIEW)) return true;
		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		if (request.getRequestURI().startsWith("/api/")) {
			response.setCharacterEncoding(StandardCharsets.UTF_8.name());
			response.setContentType("application/json");
			response.getWriter().write("{\"success\":false,\"code\":\"PERMISSION_DENIED\",\"message\":\"您沒有資料管理權限。\"}");
		} else {
			response.setCharacterEncoding(StandardCharsets.UTF_8.name());
			response.setContentType("text/html;charset=UTF-8");
			response.getWriter().write("<!doctype html><html lang=\"zh-Hant\"><meta charset=\"utf-8\"><title>403</title><body><h1>403</h1><p>您沒有資料管理權限。</p><a href=\"/\">返回首頁</a></body></html>");
		}
		return false;
	}
}
