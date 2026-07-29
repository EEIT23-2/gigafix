package com.gigafix.order.exception;

/**
 * Order Service 找不到指定訂單，或訂單不屬於目前會員時拋出，
 * 再由 OrderExceptionHandler 轉為 404 回應。
 */
public class OrderNotFoundException extends RuntimeException {

	public OrderNotFoundException(Long orderId) {
		super("找不到訂單，orderId：" + orderId);
	}
}
