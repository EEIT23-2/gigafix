package com.gigafix.order.exception;

/**
 * Order Service 發現訂單內容或狀態不符合操作規則時拋出，
 * 再由 OrderExceptionHandler 轉為 400 回應。
 */
public class InvalidOrderException extends RuntimeException {

	public InvalidOrderException(String message) {
		super(message);
	}
}
