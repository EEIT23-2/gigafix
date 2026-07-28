package com.gigafix.cart.exception;

/**
 * 購物車商品數量不符合操作規則時拋出，
 * 再由 CartExceptionHandler 轉為 400 回應。
 */
public class InvalidCartQuantityException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public InvalidCartQuantityException(String message) {
		super(message);
	}
}
