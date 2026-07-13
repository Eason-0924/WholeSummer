package com.example.cramschool.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	private final AuthInterceptor authInterceptor;
	private final OperationLogInterceptor operationLogInterceptor;
	private final DataManagementPermissionInterceptor dataManagementPermissionInterceptor;

	public WebConfig(AuthInterceptor authInterceptor, OperationLogInterceptor operationLogInterceptor,
			DataManagementPermissionInterceptor dataManagementPermissionInterceptor) {
		this.authInterceptor = authInterceptor;
		this.operationLogInterceptor = operationLogInterceptor;
		this.dataManagementPermissionInterceptor = dataManagementPermissionInterceptor;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new NoCacheInterceptor())
				.addPathPatterns("/make-up/**", "/data-management", "/data-management/**", "/api/admin/data/**",
						"/service-worker.js", "/manifest.json", "/js/data-management.js");
		registry.addInterceptor(authInterceptor)
				.addPathPatterns("/**")
				.excludePathPatterns(
						"/login",
						"/register",
						"/liff/**",
						"/api/line/**",
						"/internal/desktop/**",
						"/error",
						"/favicon.ico",
						"/manifest.json",
						"/service-worker.js",
						"/css/**",
						"/icons/**",
						"/js/**",
						"/images/**",
						"/webjars/**");
		registry.addInterceptor(operationLogInterceptor)
				.addPathPatterns("/**")
				.excludePathPatterns(
						"/logout",
						"/liff/**",
						"/api/line/**",
						"/internal/desktop/**",
						"/error",
						"/favicon.ico",
						"/manifest.json",
						"/service-worker.js",
						"/css/**",
						"/icons/**",
						"/js/**",
						"/images/**",
						"/webjars/**");
		registry.addInterceptor(dataManagementPermissionInterceptor)
				.addPathPatterns("/data-management", "/api/admin/data/**");
	}

	private static final class NoCacheInterceptor implements HandlerInterceptor {
		@Override
		public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
			response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
			response.setHeader("Pragma", "no-cache");
			response.setDateHeader("Expires", 0);
			return true;
		}
	}
}
