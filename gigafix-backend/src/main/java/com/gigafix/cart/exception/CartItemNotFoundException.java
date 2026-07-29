package com.gigafix.cart.exception;

/**
 * Cart Service 找不到指定購物車項目，或項目不屬於目前會員時拋出，
 * 再由 CartExceptionHandler 轉為 404 回應。
 */
public class CartItemNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public CartItemNotFoundException(Long cartItemId) {
		super("找不到購物車項目，cartItemId：" + cartItemId);
	}
}
