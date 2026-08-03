package com.gigafix.cart.exception;

/**
 * 同一商品已存在於購物車時拋出，由 CartExceptionHandler 轉為 409 回應。
 */
public class DuplicateCartItemException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public DuplicateCartItemException(Long productId) {
		super("商品已在購物車中，productId：" + productId);
	}
}
