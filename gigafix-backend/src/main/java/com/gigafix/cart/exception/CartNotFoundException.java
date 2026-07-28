package com.gigafix.cart.exception;

/**
 * 會員沒有啟用中購物車、無法繼續查詢或結帳時由 Service 拋出，
 * 再由模組的 ExceptionHandler 轉為 404 回應。
 */
public class CartNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public CartNotFoundException(Long memberId) {
		super("找不到會員的啟用中購物車，memberId：" + memberId);
	}
}
