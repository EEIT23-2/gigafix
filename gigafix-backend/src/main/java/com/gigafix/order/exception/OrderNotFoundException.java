package com.gigafix.order.exception;

public class OrderNotFoundException extends RuntimeException {

	public OrderNotFoundException(Long orderId) {
		super("找不到訂單，orderId：" + orderId);
	}
}
