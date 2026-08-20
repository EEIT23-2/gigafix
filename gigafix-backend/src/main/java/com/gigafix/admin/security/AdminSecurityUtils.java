package com.gigafix.admin.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
@Component
@RequiredArgsConstructor
public class AdminSecurityUtils {
	private final SecurityContextRepository securityContextRepository;
	private final HttpServletRequest request;   // Spring 自動注入 request-scoped proxy
    private final HttpServletResponse response;
	
	 public void refreshAuthentication(UserDetails userDetails) {
		//已認證後new UsernamePasswordAuthenticationToken(已驗證過的 UserDetails 物件,密碼而驗證成功後通常會設為 null,該使用者的權限清單)
		Authentication newAuth = new UsernamePasswordAuthenticationToken(
				userDetails,
				null,
				userDetails.getAuthorities());
		SecurityContextHolder.getContext().setAuthentication(newAuth);
		securityContextRepository.saveContext(SecurityContextHolder.getContext(), request, response);
	}
	
	
	public static AdminUserDetails getCurrentAdmin(Authentication authentication) {
        return (AdminUserDetails) authentication.getPrincipal();
    }

}
