package com.gigafix.admin.security;

import org.springframework.security.core.Authentication;

public class AdminSecurityUtils {
	public static AdminUserDetails getCurrentAdmin(Authentication authentication) {
        return (AdminUserDetails) authentication.getPrincipal();
    }
}
