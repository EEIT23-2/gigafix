package com.gigafix.common.interceptor;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.gigafix.common.util.JwtUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {
	
	private final JwtUtils jwtUtils;
	
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)throws Exception {
		//預檢請求 (CORS Options) 直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        
        //前端的請求中找到Authorization Header 然後取出JWT
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
            return false;
        }

        // 裁切掉 "Bearer " 前綴取得純 Token
        String token = authHeader.substring(7);
		
		if (!jwtUtils.validateToken(token)) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);//401
			return false;
		}
		request.setAttribute("memberName", jwtUtils.extractMemberName(token));
		request.setAttribute("memberId", jwtUtils.extractMemberId(token)); //從網頁取得的jwt為字串
		return true;
	}
	
}
