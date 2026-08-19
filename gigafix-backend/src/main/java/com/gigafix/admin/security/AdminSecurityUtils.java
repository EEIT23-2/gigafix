package com.gigafix.admin.security;

import org.springframework.security.core.Authentication;

public class AdminSecurityUtils {
	 private AdminSecurityUtils() {}//不給人new這個工具
	
	public static AdminUserDetails getCurrentAdmin(Authentication authentication) {
        return (AdminUserDetails) authentication.getPrincipal();
    }

}
