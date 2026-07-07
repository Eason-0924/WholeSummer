package com.example.cramschool.config;

import java.io.IOException;
import java.util.Locale;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LineHostRoutingFilter extends OncePerRequestFilter {

	static final String LINE_HOST = "line.whole-summer.com";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String host = forwardedHost(request);
		if (LINE_HOST.equals(host) && !isLineAllowedPath(pathWithinApplication(request))) {
			writeLineHostBlockedPage(response);
			return;
		}
		filterChain.doFilter(request, response);
	}

	private boolean isLineAllowedPath(String path) {
		return "/api/line/webhook".equals(path)
				|| "/liff/leave".equals(path)
				|| "/api/line/liff".equals(path)
				|| path.startsWith("/api/line/liff/")
				|| "/favicon.ico".equals(path)
				|| "/error".equals(path)
				|| path.startsWith("/css/")
				|| path.startsWith("/js/")
				|| path.startsWith("/images/")
				|| path.startsWith("/webjars/");
	}

	private String pathWithinApplication(HttpServletRequest request) {
		String requestUri = request.getRequestURI();
		String contextPath = request.getContextPath();
		if (contextPath != null && !contextPath.isBlank() && requestUri.startsWith(contextPath)) {
			return requestUri.substring(contextPath.length());
		}
		return requestUri;
	}

	private String forwardedHost(HttpServletRequest request) {
		String host = request.getHeader("X-Forwarded-Host");
		if (host == null || host.isBlank()) {
			host = request.getHeader("Host");
		}
		if (host == null || host.isBlank()) {
			host = request.getServerName();
		}
		return normalizeHost(host);
	}

	private String normalizeHost(String host) {
		String normalized = host.split(",", 2)[0].trim().toLowerCase(Locale.ROOT);
		if (normalized.startsWith("[")) {
			int closingBracket = normalized.indexOf(']');
			return closingBracket >= 0 ? normalized.substring(0, closingBracket + 1) : normalized;
		}
		int portSeparator = normalized.indexOf(':');
		return portSeparator >= 0 ? normalized.substring(0, portSeparator) : normalized;
	}

	private void writeLineHostBlockedPage(HttpServletResponse response) throws IOException {
		response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		response.setCharacterEncoding("UTF-8");
		response.setContentType("text/html;charset=UTF-8");
		response.getWriter().write("""
				<!DOCTYPE html>
				<html lang="zh-Hant">
				<head>
					<meta charset="UTF-8">
					<meta name="viewport" content="width=device-width, initial-scale=1">
					<title>LINE 專用入口</title>
				</head>
				<body>
					<h1>此網址為 LINE 專用入口</h1>
					<p>請從管理系統網址進入補習班後台。</p>
				</body>
				</html>
				""");
	}
}
