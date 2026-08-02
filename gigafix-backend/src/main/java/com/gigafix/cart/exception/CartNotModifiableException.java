package com.gigafix.cart.exception;

import com.gigafix.cart.entity.Cart;

/**
 * 非 ACTIVE 購物車收到修改操作時拋出，由 CartExceptionHandler 轉為 409 回應。
 */
public class CartNotModifiableException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public CartNotModifiableException(Long cartId, Cart.CartStatus status) {
		super("購物車狀態不可修改，cartId：" + cartId + "，status：" + status);
	}
}
