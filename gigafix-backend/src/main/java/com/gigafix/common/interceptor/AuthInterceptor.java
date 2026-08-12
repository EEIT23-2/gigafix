package com.gigafix.common.interceptor;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.gigafix.common.util.JwtUtils;

import jakarta.servlet.http.Cookie;
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
        
        Cookie[] cookies = request.getCookies();
        
        String token = null;
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("token".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;  // 加這個，找到就跳出，不用整個陣列跑完
                }
            }
        }
        
        if (token == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
            return false;
        } //怕空指標
        
        if (!jwtUtils.validateToken(token)) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
			return false;
		}
        
        
		request.setAttribute("memberId", jwtUtils.extractMemberId(token)); //從網頁取得的jwt為字串透過這個方法會轉回去Long
		return true;
	}
	
}
