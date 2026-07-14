package com.example.cramschool.config;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.example.cramschool.controller.AuthController;
import com.example.cramschool.service.TeacherAccountService;
import com.example.cramschool.service.ActiveUserRegistry;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

	private final TeacherAccountService teacherAccountService;
	private final ActiveUserRegistry activeUserRegistry;

	public AuthInterceptor(TeacherAccountService teacherAccountService, ActiveUserRegistry activeUserRegistry) {
		this.teacherAccountService = teacherAccountService;
		this.activeUserRegistry = activeUserRegistry;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		HttpSession session = request.getSession(false);
		if (session != null) {
			Object accountIdValue = session.getAttribute(AuthController.ACCOUNT_ID_SESSION_KEY);
				if (accountIdValue instanceof Long accountId
						&& teacherAccountService.findActiveAccount(accountId).isPresent()) {
					activeUserRegistry.touch(session.getId(), request.getRemoteAddr(), request.getHeader("User-Agent"));
					return true;
			}
			session.invalidate();
		}
		String target = request.getRequestURI();
		String query = request.getQueryString();
		if (query != null && !query.isBlank()) {
			target += "?" + query;
		}
		response.sendRedirect(request.getContextPath() + "/login?redirect="
				+ URLEncoder.encode(target, StandardCharsets.UTF_8));
		return false;
	}
}
