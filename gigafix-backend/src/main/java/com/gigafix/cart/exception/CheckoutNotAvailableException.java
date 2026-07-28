package com.gigafix.cart.exception;

public class CheckoutNotAvailableException extends RuntimeException {

	public CheckoutNotAvailableException() {
		super("商品模組尚未完成，暫時無法建立訂單");
	}
}
