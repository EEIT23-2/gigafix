package com.gigafix.cart.exception;

public class EmptyCartException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public EmptyCartException(Long cartId) {
		super("購物車沒有任何商品，cartId：" + cartId);
	}
}
