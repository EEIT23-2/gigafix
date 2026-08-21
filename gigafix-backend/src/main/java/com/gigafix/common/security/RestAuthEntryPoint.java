package com.gigafix.common.security;

import java.io.IOException;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.gigafix.common.dto.ErrorResp;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class RestAuthEntryPoint implements AuthenticationEntryPoint {
	// 如果在filter時拋錯，若是沒登入/session失效的的畫會是authException，會被這個實做AuthenticationEntryPoint的類別攔截
	
	private final ObjectMapper objectMapper;
	
	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,AuthenticationException authException) throws IOException, ServletException {
        //可以用authException.getMessage()印log，但不建議直接回給前端（避免洩漏內部細節）
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); //設定response的status code為401
		response.setContentType("application/json;charset=UTF-8"); //設定回傳到前端的格式為Json編碼為UTF-8
		
		ErrorResp errorResp = ErrorResp.builder()
								.errorCode("UNAUTHORIZED")
								.message("請先登入")
								.build();
		
		response.getWriter().write(objectMapper.writeValueAsString(errorResp));
		
	}

}
