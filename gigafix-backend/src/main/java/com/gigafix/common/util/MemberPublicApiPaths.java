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
			"/api/gigafix/members/registerOrLoginAFakeMember",
			// jack的商城路由加入機不可失路由監管
			"/api/gigafix/products",
			"/api/gigafix/products/**",

			// ECPay 超商選店回傳
			"/api/gigafix/ecpay/logistics/store-callback",
			// ECPay 金流付款結果 Server 回傳
			"/api/gigafix/ecpay/payment/return",
			// ECPay 金流 Browser Result
			"/api/gigafix/ecpay/payment/result",
			// ECPay 維修單付款結果 Server 回傳
			"/api/gigafix/ecpay/repair-payment/return",
			// ECPay 維修單付款 Browser Result
			"/api/gigafix/ecpay/repair-payment/result",
			// forum 的公開瀏覽端點（文章列表/詳情、樓層列表、留言列表、分類列表），皆為 GET，不需要登入
			"/api/articles",
			"/api/articles/**",
			"/api/categories",
			"/api/categories/**"
	};
}
