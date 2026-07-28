package com.gigafix.cart.exception;

public class CartMemberNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public CartMemberNotFoundException(Long memberId) {
		super("找不到會員，memberId：" + memberId);
	}
}
