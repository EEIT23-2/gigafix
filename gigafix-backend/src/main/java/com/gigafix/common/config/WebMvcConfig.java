package com.gigafix.common.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.gigafix.common.interceptor.AuthInterceptor;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
	@Autowired
	private AuthInterceptor authInterceptor;

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(authInterceptor).
						addPathPatterns("/gigafix/member"). //之後要把/member拿掉
						excludePathPatterns(
								"/gigafix/member/login",
								"/gigafix/member/register",
								"/gigafix/member/forgot-password",
		                        "/error"
								);
	}
	
	
	
}
