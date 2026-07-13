package com.example.cramschool.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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
}
