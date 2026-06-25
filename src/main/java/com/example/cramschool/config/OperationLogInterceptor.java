package com.example.cramschool.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.example.cramschool.controller.AuthController;
import com.example.cramschool.service.OperationLogService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class OperationLogInterceptor implements HandlerInterceptor {

	private static final String ACCOUNT_ATTRIBUTE = OperationLogInterceptor.class.getName() + ".accountId";
	private static final String TEACHER_ATTRIBUTE = OperationLogInterceptor.class.getName() + ".teacherId";
	private static final String NAME_ATTRIBUTE = OperationLogInterceptor.class.getName() + ".actorName";

	private final OperationLogService operationLogService;

	public OperationLogInterceptor(OperationLogService operationLogService) {
		this.operationLogService = operationLogService;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		captureActor(request, request.getSession(false));
		return true;
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
			Object handler, Exception exception) {
		String action = OperationActionResolver.resolve(request.getRequestURI());
		if (action == null) {
			return;
		}
		if (request.getAttribute(ACCOUNT_ATTRIBUTE) == null) {
			captureActor(request, request.getSession(false));
		}
		if ("/login".equals(request.getRequestURI())
				&& request.getAttribute(ACCOUNT_ATTRIBUTE) == null) {
			return;
		}
		if (exception == null && response.getStatus() < 300) {
			return;
		}
		String result = exception == null && response.getStatus() < 400 ? "成功" : "失敗";
		try {
			operationLogService.record(
					(Long) request.getAttribute(ACCOUNT_ATTRIBUTE),
					(Long) request.getAttribute(TEACHER_ATTRIBUTE),
					(String) request.getAttribute(NAME_ATTRIBUTE),
					action,
					request.getMethod(),
					request.getRequestURI(),
					result);
		} catch (RuntimeException ignored) {
			// An audit failure must never replace the original user response.
		}
	}

	private void captureActor(HttpServletRequest request, HttpSession session) {
		if (session == null) {
			return;
		}
		Object accountId = session.getAttribute(AuthController.ACCOUNT_ID_SESSION_KEY);
		Object teacherId = session.getAttribute(AuthController.TEACHER_ID_SESSION_KEY);
		Object teacherName = session.getAttribute(AuthController.TEACHER_NAME_SESSION_KEY);
		if (accountId instanceof Long id) {
			request.setAttribute(ACCOUNT_ATTRIBUTE, id);
		}
		if (teacherId instanceof Long id) {
			request.setAttribute(TEACHER_ATTRIBUTE, id);
		}
		if (teacherName != null) {
			request.setAttribute(NAME_ATTRIBUTE, teacherName.toString());
		}
	}
}
