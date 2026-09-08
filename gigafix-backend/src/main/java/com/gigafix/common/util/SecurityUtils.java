package com.gigafix.common.util;

import org.springframework.security.core.Authentication;

import com.gigafix.admin.security.AdminUserDetails;
import com.gigafix.member.security.MemberUserDetails;

// 前後台共用的工具：從Authentication拿目前登入者的UserDetails，兩邊Controller只要有Authentication參數就能用
public class SecurityUtils {

	private SecurityUtils() {
		// 純靜態方法的工具類別，不需要被實例化
	}

	public static AdminUserDetails getCurrentAdmin(Authentication authentication) {
		return (AdminUserDetails) authentication.getPrincipal();
	}

	public static MemberUserDetails getCurrentMember(Authentication authentication) {
		return (MemberUserDetails) authentication.getPrincipal();
	}

}
