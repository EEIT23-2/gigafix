package com.gigafix.common.util;

/**
 * 前台（/api/gigafix/**）底下不需要登入即可存取的公開路徑清單
 * 組員新增公開 API 時，只要把路徑加進這個陣列即可
 * 不用去理解或修改 SecurityConfig 裡 Spring Security 的設定邏輯
 */
public final class MemberPublicApiPaths {

	private MemberPublicApiPaths() {
		// 純常數類別，設定private故意不讓人去new這個物件
	}

	public static final String[] PATHS = {
			"/api/gigafix/login",
			"/api/gigafix/members/register",
			"/api/gigafix/members/register/otp",
			"/api/gigafix/members/forgot-password",
			"/api/gigafix/members/forgot-password/otp",
			"/api/gigafix/login/google",
			// jack的商城路由加入機不可失路由監管
			"/api/gigafix/products",
			"/api/gigafix/products/**"
	};
}
