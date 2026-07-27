package com.gigafix.cart.exception;

public class CartNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public CartNotFoundException(Long userId) {
		super("找不到使用者的啟用中購物車，userId：" + userId);
	}
}
