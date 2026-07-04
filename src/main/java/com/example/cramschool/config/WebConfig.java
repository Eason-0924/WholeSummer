package com.example.cramschool.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	private final AuthInterceptor authInterceptor;
	private final OperationLogInterceptor operationLogInterceptor;

	public WebConfig(AuthInterceptor authInterceptor, OperationLogInterceptor operationLogInterceptor) {
		this.authInterceptor = authInterceptor;
		this.operationLogInterceptor = operationLogInterceptor;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(authInterceptor)
				.addPathPatterns("/**")
				.excludePathPatterns(
						"/login",
						"/register",
						"/api/line/**",
						"/internal/desktop/**",
						"/error",
						"/favicon.ico",
						"/css/**",
						"/js/**",
						"/images/**",
						"/webjars/**");
		registry.addInterceptor(operationLogInterceptor)
				.addPathPatterns("/**")
				.excludePathPatterns(
						"/logout",
						"/api/line/**",
						"/internal/desktop/**",
						"/error",
						"/favicon.ico",
						"/css/**",
						"/js/**",
						"/images/**",
						"/webjars/**");
	}
}
