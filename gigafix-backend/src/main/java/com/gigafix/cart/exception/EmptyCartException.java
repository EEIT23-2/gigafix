package com.gigafix.cart.exception;

/**
 * Order Service 發現購物車沒有商品、不能結帳時拋出，
 * 再由模組的 ExceptionHandler 轉為 409 回應。
 */
public class EmptyCartException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public EmptyCartException(Long cartId) {
		super("購物車沒有任何商品，cartId：" + cartId);
	}
}
