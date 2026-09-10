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
						addPathPatterns(
								"/api/gigafix/members/**", //之後要把/member拿掉
								"/api/repairs/appointment", //客戶預約維修單，需要先登入
								"/api/repairs/me", //會員中心查詢自己的維修單，需要先登入
								"/api/repairs/*/approval" //客戶回應報價，需要先登入
								).
						excludePathPatterns(
								"/api/gigafix/members/login",
								"/api/gigafix/members/register",
								"/api/gigafix/members/register/otp",
								"/api/gigafix/members/forgot-password",
								"/api/gigafix/members/forgot-password/otp",
		                        "/error"
								);
	}
	
	
	
}
