package com.gigafix.cart.exception;

public class CartItemNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public CartItemNotFoundException(Long cartItemId) {
		super("找不到購物車項目，cartItemId：" + cartItemId);
	}
}
