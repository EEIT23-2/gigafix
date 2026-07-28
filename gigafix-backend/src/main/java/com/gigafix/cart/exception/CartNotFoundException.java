package com.gigafix.cart.exception;

public class CartNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public CartNotFoundException(Long memberId) {
		super("找不到會員的啟用中購物車，memberId：" + memberId);
	}
}
