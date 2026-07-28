package com.gigafix.cart.exception;

/**
 * Cart Service 無法依會員識別碼找到會員時拋出，
 * 再由 CartExceptionHandler 轉為 404 回應。
 */
public class CartMemberNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public CartMemberNotFoundException(Long memberId) {
		super("找不到會員，memberId：" + memberId);
	}
}
